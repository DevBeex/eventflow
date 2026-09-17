package com.eventflow.customer.service;

import com.eventflow.common.BusinessException;
import com.eventflow.common.ClockService;
import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.common.TextUtils;
import com.eventflow.common.ValidationException;
import com.eventflow.customer.dto.CreateCustomerRequest;
import com.eventflow.customer.dto.CustomerResponse;
import com.eventflow.customer.entity.Customer;
import com.eventflow.customer.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CustomerService {

    @Inject
    CustomerRepository customerRepository;

    @Inject
    ClockService clockService;

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        List<ErrorResponse.FieldError> errors = validateCreate(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid request", errors);
        }

        String name = TextUtils.normalizeName(request.name());
        String email = TextUtils.normalizeEmail(request.email());

        var now = clockService.now();
        Customer customer = new Customer();
        customer.name = name;
        customer.email = email;
        customer.createdAt = now;
        customer.updatedAt = now;
        customerRepository.persist(customer);
        return CustomerMapper.toResponse(customer);
    }

    public PageResponse<CustomerResponse> list(PaginationParams pagination) {
        long total = customerRepository.countAll();
        List<CustomerResponse> items = customerRepository.findPage(pagination.page(), pagination.size())
                .stream()
                .map(CustomerMapper::toResponse)
                .toList();
        return PageResponse.of(items, pagination.page(), pagination.size(), total);
    }

    private List<ErrorResponse.FieldError> validateCreate(CreateCustomerRequest request) {
        List<ErrorResponse.FieldError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ErrorResponse.FieldError("body", "request body is required"));
            return errors;
        }

        String name = request.name() == null ? null : TextUtils.normalizeName(request.name());
        if (name == null || name.isEmpty() || name.length() > 120) {
            errors.add(new ErrorResponse.FieldError("name", "must be between 1 and 120 characters"));
        }

        String email = request.email() == null ? null : TextUtils.normalizeEmail(request.email());
        if (email == null || email.isBlank()) {
            errors.add(new ErrorResponse.FieldError("email", "must not be blank"));
        } else if (email.length() > 254 || !isValidEmail(email)) {
            errors.add(new ErrorResponse.FieldError("email", "must be a valid email address"));
        }

        return errors;
    }

    private boolean isValidEmail(String email) {
        return jakarta.validation.Validation.buildDefaultValidatorFactory()
                .getValidator()
                .validateValue(EmailHolder.class, "email", email)
                .isEmpty();
    }

    private static class EmailHolder {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        String email;
    }

    public BusinessException duplicateEmailException() {
        return new BusinessException(409, ErrorCode.DUPLICATE_CUSTOMER_EMAIL, "Customer email already registered");
    }
}
