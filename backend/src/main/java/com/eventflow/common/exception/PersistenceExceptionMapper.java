package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;
import org.jboss.logging.Logger;

import java.sql.SQLException;
import java.util.List;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    private static final Logger LOG = Logger.getLogger(PersistenceExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException exception) {
        if (isDuplicateCustomerEmail(exception)) {
            ErrorResponse body = new ErrorResponse(
                    409,
                    ErrorCode.DUPLICATE_CUSTOMER_EMAIL.name(),
                    "Customer email already registered",
                    List.of()
            );
            return Response.status(409).entity(body).build();
        }
        if (isTransient(exception)) {
            ErrorResponse body = new ErrorResponse(
                    503,
                    ErrorCode.SERVICE_UNAVAILABLE.name(),
                    "Database temporarily unavailable",
                    List.of()
            );
            return Response.status(503).entity(body).build();
        }

        LOG.error("Unexpected persistence error", exception);
        ErrorResponse body = new ErrorResponse(
                500,
                ErrorCode.INTERNAL_ERROR.name(),
                "Internal server error",
                List.of()
        );
        return Response.status(500).entity(body).build();
    }

    private boolean isDuplicateCustomerEmail(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException cve) {
                String constraint = cve.getConstraintName();
                if (constraint != null && constraint.toLowerCase().contains("email")) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("uk_customers_email") || lower.contains("customers_email")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isTransient(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof JDBCConnectionException) {
                return true;
            }
            if (current instanceof SQLException sqlEx) {
                String state = sqlEx.getSQLState();
                if ("08000".equals(state) || "08003".equals(state) || "08006".equals(state) || "57P01".equals(state)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
