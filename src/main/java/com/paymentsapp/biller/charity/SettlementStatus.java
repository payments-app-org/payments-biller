package com.paymentsapp.biller.charity;

// Перевод фонду: SENT — создан и отправляется, DONE — процессинг провёл, FAILED — повторим при следующем прогоне
public enum SettlementStatus {
    SENT,
    DONE,
    FAILED
}
