package com.paymentsapp.biller.bill;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

// govMinAmount — минимум оплаты госуслуги без начисления; monthRange — на сколько месяцев от текущего можно
// выбрать период оплаты; templateNameMax — длина названия шаблона «Счета на оплату»
@ConfigurationProperties("biller")
public record BillerProperties(BigDecimal govMinAmount, int monthRange, int templateNameMax) {
}
