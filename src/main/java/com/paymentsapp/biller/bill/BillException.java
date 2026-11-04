package com.paymentsapp.biller.bill;

import org.springframework.http.HttpStatus;

// Отказ по платежу поставщику. code совпадает с кодами ошибок payment — он передаёт их пользователю как есть.
public class BillException extends RuntimeException {

    public enum Code {
        INVALID_REQUISITES(HttpStatus.BAD_REQUEST),
        INVALID_AMOUNT(HttpStatus.BAD_REQUEST),
        RECIPIENT_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT),
        BILL_NOT_PAYABLE(HttpStatus.UNPROCESSABLE_CONTENT);

        final HttpStatus status;

        Code(HttpStatus status) {
            this.status = status;
        }
    }

    private final Code code;

    public BillException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return code.status;
    }
}
