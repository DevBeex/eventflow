#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SUFFIX="$(uuidgen 2>/dev/null | tr '[:upper:]' '[:lower:]' | cut -c1-8 || date +%s)"

echo "Using base URL: ${BASE_URL}"

CUSTOMER_JSON=$(curl -sS -X POST "${BASE_URL}/api/customers" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"John Doe ${SUFFIX}\",\"email\":\"john+${SUFFIX}@example.com\"}")
CUSTOMER_ID=$(echo "${CUSTOMER_JSON}" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "Customer id=${CUSTOMER_ID}"

PRODUCT_A_JSON=$(curl -sS -X POST "${BASE_URL}/api/products" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Mechanical Keyboard ${SUFFIX}\",\"description\":\"RGB mechanical keyboard\",\"price\":75.00,\"active\":true}")
PRODUCT_A_ID=$(echo "${PRODUCT_A_JSON}" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

PRODUCT_B_JSON=$(curl -sS -X POST "${BASE_URL}/api/products" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Mouse ${SUFFIX}\",\"description\":\"Wireless mouse\",\"price\":50.00,\"active\":true}")
PRODUCT_B_ID=$(echo "${PRODUCT_B_JSON}" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

ORDER_JSON=$(curl -sS -X POST "${BASE_URL}/api/orders" \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":${CUSTOMER_ID},\"items\":[{\"productId\":${PRODUCT_A_ID},\"quantity\":2},{\"productId\":${PRODUCT_B_ID},\"quantity\":1}]}")
ORDER_ID=$(echo "${ORDER_JSON}" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "Order id=${ORDER_ID}"

for i in $(seq 1 30); do
  COUNT=$(curl -sS "${BASE_URL}/api/notifications?orderId=${ORDER_ID}" | sed -n 's/.*"totalItems":\([0-9]*\).*/\1/p')
  if [ "${COUNT}" -gt 0 ]; then
    curl -sS "${BASE_URL}/api/notifications?orderId=${ORDER_ID}"
    echo
    break
  fi
  sleep 1
done

curl -sS -X PATCH "${BASE_URL}/api/orders/${ORDER_ID}/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"COMPLETED"}'
echo

curl -sS -o /dev/null -w "cancel status=%{http_code}\n" -X PATCH "${BASE_URL}/api/orders/${ORDER_ID}/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"CANCELLED"}'

echo "Demo completed."
