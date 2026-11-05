package com.paymentsapp.biller.client;

// Имена соседей: группы HTTP-клиентов (spring.http.serviceclient.*) и экземпляров Resilience4j (resilience4j.*)
public final class Backends {

    public static final String GATEWAY = "gateway";
    public static final String GIS_GMP = "gis-gmp";
    public static final String PROCESSING = "processing";
    // Оплату у поставщика повторяем, только если не удалось соединиться (шлюз идемпотентен, но исход неизвестен)
    public static final String GATEWAY_CONNECT_ONLY = "gateway-connect-only";

    private Backends() {
    }
}
