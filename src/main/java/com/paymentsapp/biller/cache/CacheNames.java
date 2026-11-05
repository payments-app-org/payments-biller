package com.paymentsapp.biller.cache;

// Кэши справочников в Redis (libs/platform). Справочники меняются только миграциями: TTL — platform.cache.ttl.*,
// после изменения формата данных — поднять platform.cache.version.
public final class CacheNames {

    public static final String PROVIDER_DETAILS = "provider-details";
    public static final String PROVIDER = "provider";
    public static final String PROVIDER_SEARCH = "provider-search";
    public static final String REGIONS = "regions";
    public static final String COUNTRIES = "countries";
    public static final String COUNTRY = "country";
    public static final String METHODS = "methods";
    public static final String METHOD = "method";
    public static final String METHOD_FIELDS = "method-fields";

    private CacheNames() {
    }
}
