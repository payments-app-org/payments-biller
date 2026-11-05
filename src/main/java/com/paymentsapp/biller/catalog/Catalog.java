package com.paymentsapp.biller.catalog;

import java.math.BigDecimal;
import java.util.List;

// Модель справочника переводов за рубеж
public final class Catalog {

    private Catalog() {
    }

    public record Country(String code, String nameRu, String nameNative, String dialCode, boolean popular,
                          List<String> currencies) {
    }

    // kindLabel — подпись для интерфейса: «Пополнение карты», «По номеру телефона»…
    // minAmount / maxAmount — сколько ₽ можно отправить одним переводом этим способом
    public record Method(int id, String countryCode, String name, MethodKind kind, String kindLabel, String color,
                         String templateCode, BigDecimal minAmount, BigDecimal maxAmount) {
    }

    // Страна со способами и полями каждого способа: форма строится одним запросом, без второго за полями
    public record CountryDetails(Country country, List<CountryMethod> methods) {
    }

    public record CountryMethod(int id, String name, MethodKind kind, String kindLabel, String color, BigDecimal minAmount,
                                BigDecimal maxAmount, List<Field> fields) {

        public static CountryMethod of(Method m, List<Field> fields) {
            return new CountryMethod(m.id(), m.name(), m.kind(), m.kindLabel(), m.color(), m.minAmount(), m.maxAmount(), fields);
        }
    }

    // Поле данных получателя: что спросить и как проверить
    public record Field(String key, String label, String type, String pattern, String placeholder) {
    }

    public record MethodDetails(Method method, List<String> currencies, List<Field> fields) {
    }

    // Вид способа перевода (transfer_methods.kind) и подпись для интерфейса
    public enum MethodKind {
        CARD("Пополнение карты"),
        PHONE("По номеру телефона"),
        ACCOUNT("На банковский счёт"),
        WALLET("Электронный кошелёк");

        private final String label;

        MethodKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
