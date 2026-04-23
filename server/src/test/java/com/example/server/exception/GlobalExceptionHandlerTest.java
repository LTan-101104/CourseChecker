package com.example.server.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.example.server.api.ErrorResponseFactory;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorResponseFactory());

    @Test
    void handlesValidationAndConflictResponses() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/me");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.handleInvalidArguments(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().details()).containsEntry("email", "must be a well-formed email address");

        var conflictResponse = handler.handleConflict(new ConflictException("duplicate"), request);
        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handlesOtherMappedStatuses() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");

        assertThat(handler.handleUnauthorized(new UnauthorizedException("bad credentials"), request)
            .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleForbidden(new ForbiddenException("forbidden"), request)
            .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleNotFound(new ResourceNotFoundException("missing"), request)
            .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleBadRequest(
            new HttpMessageNotReadableException("invalid json", (org.springframework.http.HttpInputMessage) null),
            request
        )
            .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleHandlerValidation((HandlerMethodValidationException) null, request)
            .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
