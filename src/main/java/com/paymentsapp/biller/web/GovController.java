package com.paymentsapp.biller.web;

import com.paymentsapp.biller.gov.GovCharge;
import com.paymentsapp.biller.gov.GovChargeService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

// Госуслуги: поиск начислений по УИН и карточка начисления (её проверяет payment перед оплатой)
@RestController
@RequestMapping("/api/biller/gov")
public class GovController {

    private final GovChargeService charges;

    public GovController(GovChargeService charges) {
        this.charges = charges;
    }

    @GetMapping("/charges")
    public List<GovCharge> find(@RequestParam String uin) {
        String clean = uin.replaceAll("\\s", "");
        if (!clean.matches("\\d{20}|\\d{25}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "УИН — 20 или 25 цифр");
        }
        return charges.find(clean);
    }

    @GetMapping("/charges/{id}")
    public GovCharge get(@PathVariable UUID id) {
        return charges.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Начисление не найдено"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail status(ResponseStatusException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(RestClientException.class)
    ProblemDetail gisGmpDown(RestClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "ГИС ГМП не отвечает, попробуйте позже");
    }
}
