$ErrorActionPreference = "Stop"
$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }
$Suffix = [guid]::NewGuid().ToString("N").Substring(0, 8)

Write-Host "Using base URL: $BaseUrl"

$customer = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/customers" -ContentType "application/json" -Body (@{
    name = "John Doe $Suffix"
    email = "john+$Suffix@example.com"
} | ConvertTo-Json)
Write-Host "Customer id=$($customer.id) email=$($customer.email)"

$productA = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/products" -ContentType "application/json" -Body (@{
    name = "Mechanical Keyboard $Suffix"
    description = "RGB mechanical keyboard"
    price = 75.00
    active = $true
} | ConvertTo-Json)
Write-Host "Product A id=$($productA.id) price=$($productA.price)"

$productB = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/products" -ContentType "application/json" -Body (@{
    name = "Mouse $Suffix"
    description = "Wireless mouse"
    price = 50.00
    active = $true
} | ConvertTo-Json)
Write-Host "Product B id=$($productB.id) price=$($productB.price)"

$order = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/orders" -ContentType "application/json" -Body (@{
    customerId = $customer.id
    items = @(
        @{ productId = $productA.id; quantity = 2 },
        @{ productId = $productB.id; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5)
Write-Host "Order id=$($order.id) status=$($order.status) total=$($order.total)"

$detail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/orders/$($order.id)"
Write-Host "Order detail items=$($detail.items.Count)"

Write-Host "Polling notifications for order $($order.id)..."
$notification = $null
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    $page = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/notifications?orderId=$($order.id)"
    if ($page.totalItems -gt 0) {
        $notification = $page.items[0]
        break
    }
}
if ($null -eq $notification) {
    throw "Notification was not created within timeout"
}
Write-Host "Notification id=$($notification.id) eventId=$($notification.eventId)"

$completed = Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/orders/$($order.id)/status" -ContentType "application/json" -Body (@{ status = "COMPLETED" } | ConvertTo-Json)
Write-Host "Completed order status=$($completed.status)"

try {
    Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/orders/$($order.id)/status" -ContentType "application/json" -Body (@{ status = "CANCELLED" } | ConvertTo-Json) | Out-Null
    throw "Expected 409 when cancelling completed order"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw }
    Write-Host "Cancel after complete correctly returned 409"
}

$noop = Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/orders/$($order.id)/status" -ContentType "application/json" -Body (@{ status = "COMPLETED" } | ConvertTo-Json)
Write-Host "No-op complete updatedAt=$($noop.updatedAt)"

Write-Host "Demo completed successfully."
