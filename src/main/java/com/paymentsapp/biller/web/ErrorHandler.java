package com.paymentsapp.biller.web;

import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.platform.error.Problems;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

// Отказы домена (DomainException) — обработчик платформы. Здесь — недоступный поставщик или интеграция:
// тайм-аут, 5xx, разомкнутый circuit breaker
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(RestClientException.class)
    ProblemDetail providerDown(RestClientException e) {
        return Problems.of(BillerError.PROVIDER_UNAVAILABLE, BillerError.PROVIDER_UNAVAILABLE.template());
    }
}
