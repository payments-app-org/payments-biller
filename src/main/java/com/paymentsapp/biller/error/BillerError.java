package com.paymentsapp.biller.error;

import com.paymentsapp.platform.error.ErrorCode;
import org.springframework.http.HttpStatus;

// Отказы biller. code — контракт с payment: INVALID_REQUISITES, INVALID_AMOUNT, RECIPIENT_NOT_FOUND, BILL_NOT_PAYABLE
// payment показывает пользователю как есть.
public enum BillerError implements ErrorCode {

    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Поставщик не найден"),
    UNKNOWN_PROVIDER(HttpStatus.BAD_REQUEST, "Поставщик не найден", Codes.INVALID_REQUISITES),
    PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Поставщик не отвечает, попробуйте позже"),

    FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "Заполните поле «%s»", Codes.INVALID_REQUISITES),
    FIELD_INVALID(HttpStatus.BAD_REQUEST, "Проверьте поле «%s»", Codes.INVALID_REQUISITES),
    FIELD_INVALID_HINT(HttpStatus.BAD_REQUEST, "Проверьте поле «%s»: %s", Codes.INVALID_REQUISITES),
    EMAIL_INVALID(HttpStatus.BAD_REQUEST, "Проверьте адрес электронной почты", Codes.INVALID_REQUISITES),
    PHONE_FORMAT(HttpStatus.BAD_REQUEST, "Телефон в формате +7XXXXXXXXXX", Codes.INVALID_REQUISITES),
    CHOICE_REQUIRED(HttpStatus.BAD_REQUEST, "Выберите значение «%s»", Codes.INVALID_REQUISITES),
    NOMINAL_REQUIRED(HttpStatus.BAD_REQUEST, "Выберите номинал", Codes.INVALID_REQUISITES),
    MONTH_REQUIRED(HttpStatus.BAD_REQUEST, "Выберите месяц в поле «%s»", Codes.INVALID_REQUISITES),
    MONTH_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Период оплаты — не дальше %d месяцев от текущего", Codes.INVALID_REQUISITES),
    LOCAL_PHONE_LENGTH(HttpStatus.BAD_REQUEST, "Номер — %d цифр, без кода страны %s", Codes.INVALID_REQUISITES),
    LOCAL_PHONE_PREFIX(HttpStatus.BAD_REQUEST, "Номер должен начинаться с %s", Codes.INVALID_REQUISITES),

    OPERATOR_UNKNOWN(HttpStatus.UNPROCESSABLE_CONTENT, "Не удалось определить оператора по номеру", Codes.RECIPIENT_NOT_FOUND),
    OPERATOR_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Номер обслуживается оператором %s", Codes.RECIPIENT_NOT_FOUND),
    OPERATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "Не удалось определить оператора по номеру"),
    NOT_MOBILE_NUMBER(HttpStatus.BAD_REQUEST, "Это не мобильный номер"),
    PHONE_DIGITS(HttpStatus.BAD_REQUEST, "Номер телефона — 10 цифр после +7"),

    CHARGE_REQUIRED(HttpStatus.BAD_REQUEST, "Не выбрано начисление", Codes.INVALID_REQUISITES),
    CHARGE_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Начисление не найдено", Codes.BILL_NOT_PAYABLE),
    CHARGE_PAID(HttpStatus.UNPROCESSABLE_CONTENT, "Начисление уже оплачено", Codes.BILL_NOT_PAYABLE),
    CHARGE_AMOUNT(HttpStatus.UNPROCESSABLE_CONTENT, "Сумма не совпадает с начислением: %s", Codes.BILL_NOT_PAYABLE),
    CHARGE_UNKNOWN(HttpStatus.NOT_FOUND, "Начисление не найдено"),
    DIRECT_PAY_UNAVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "Оплата без начисления этому получателю недоступна", Codes.BILL_NOT_PAYABLE),
    MIN_AMOUNT(HttpStatus.BAD_REQUEST, "Минимальная сумма — %s", Codes.INVALID_AMOUNT),
    NOT_GOV(HttpStatus.BAD_REQUEST, "Это не госуслуга"),
    NO_CHARGE_LOOKUP(HttpStatus.BAD_REQUEST, "У этого получателя начисления не ищутся"),

    UIN_FORMAT(HttpStatus.BAD_REQUEST, "УИН — 20 или 25 цифр", Codes.INVALID_REQUISITES),
    UIN_PREFIX(HttpStatus.BAD_REQUEST, "УИН этого получателя начинается с %s", Codes.INVALID_REQUISITES),
    STS_OR_VU_REQUIRED(HttpStatus.BAD_REQUEST, "Укажите свидетельство о регистрации ТС или водительское удостоверение",
            Codes.INVALID_REQUISITES),
    STS_FORMAT(HttpStatus.BAD_REQUEST, "Номер СТС — 10 символов: XX XX XXXXXX", Codes.INVALID_REQUISITES),
    VU_FORMAT(HttpStatus.BAD_REQUEST, "Номер водительского удостоверения — 10 символов: XX XX XXXXXX", Codes.INVALID_REQUISITES),
    PERSON_INN(HttpStatus.BAD_REQUEST, "Некорректный ИНН физического лица", Codes.INVALID_REQUISITES),
    PASSPORT_OR_INN_REQUIRED(HttpStatus.BAD_REQUEST, "Укажите паспорт или ИНН", Codes.INVALID_REQUISITES),
    PASSPORT_FORMAT(HttpStatus.BAD_REQUEST, "Серия и номер паспорта — 10 цифр", Codes.INVALID_REQUISITES),
    PAYER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "Укажите ФИО плательщика", Codes.INVALID_REQUISITES),
    PAYER_ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "Укажите адрес плательщика", Codes.INVALID_REQUISITES),
    PAYER_PHONE_FORMAT(HttpStatus.BAD_REQUEST, "Телефон плательщика в формате +7XXXXXXXXXX", Codes.INVALID_REQUISITES),

    INVOICE_REQUIRED(HttpStatus.BAD_REQUEST, "Не выбран счёт", Codes.INVALID_REQUISITES),
    INVOICE_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Счёт не найден", Codes.BILL_NOT_PAYABLE),
    INVOICE_PAID(HttpStatus.UNPROCESSABLE_CONTENT, "Счёт уже оплачен", Codes.BILL_NOT_PAYABLE),
    INVOICE_AMOUNT(HttpStatus.UNPROCESSABLE_CONTENT, "Сумма не совпадает со счётом: %s", Codes.BILL_NOT_PAYABLE),
    NOMINAL_PRICE(HttpStatus.BAD_REQUEST, "Цена номинала — %s", Codes.INVALID_AMOUNT),
    AMOUNT_RANGE(HttpStatus.BAD_REQUEST, "Сумма у этого поставщика — от %s до %s", Codes.INVALID_AMOUNT),
    NO_INVOICE_SEARCH(HttpStatus.BAD_REQUEST, "У этого поставщика счета не ищутся", Codes.INVALID_REQUISITES),

    NO_BILLS(HttpStatus.BAD_REQUEST, "У этого поставщика нет счетов на оплату", Codes.INVALID_REQUISITES),
    TEMPLATE_NAME(HttpStatus.BAD_REQUEST, "Название шаблона — от 1 до %d символов", Codes.INVALID_REQUISITES),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Шаблон не найден"),
    BILL_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Оплата у поставщика не найдена"),
    COUNTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "Страна не найдена"),
    METHOD_NOT_FOUND(HttpStatus.NOT_FOUND, "Способ перевода не найден");

    private final HttpStatus status;
    private final String template;
    private final String code;

    BillerError(HttpStatus status, String template) {
        this(status, template, null);
    }

    BillerError(HttpStatus status, String template, String code) {
        this.status = status;
        this.template = template;
        this.code = code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String template() {
        return template;
    }

    @Override
    public String code() {
        return code != null ? code : name();
    }

    private static final class Codes {
        static final String INVALID_REQUISITES = "INVALID_REQUISITES";
        static final String INVALID_AMOUNT = "INVALID_AMOUNT";
        static final String RECIPIENT_NOT_FOUND = "RECIPIENT_NOT_FOUND";
        static final String BILL_NOT_PAYABLE = "BILL_NOT_PAYABLE";
    }
}
