package com.paymentsapp.biller.web;

import com.paymentsapp.biller.catalog.Catalog.Country;
import com.paymentsapp.biller.catalog.Catalog.CountryDetails;
import com.paymentsapp.biller.catalog.Catalog.Method;
import com.paymentsapp.biller.catalog.Catalog.MethodDetails;
import com.paymentsapp.biller.catalog.CatalogRepository;
import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.platform.error.DomainException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Справочник переводов за рубеж: страны → валюты и способы → поля получателя
@RestController
@RequestMapping("/api/biller")
public class CatalogController {

    private final CatalogRepository catalog;

    public CatalogController(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/countries")
    public List<Country> countries(@RequestParam(required = false) String q) {
        return catalog.countries(q);
    }

    @GetMapping("/countries/{code}")
    public CountryDetails country(@PathVariable String code) {
        Country country = catalog.country(code).orElseThrow(() -> notFound(BillerError.COUNTRY_NOT_FOUND));
        return new CountryDetails(country, catalog.methods(country.code()));
    }

    // Способ перевода с валютами страны и полями, которые нужно заполнить
    @GetMapping("/methods/{id}")
    public MethodDetails method(@PathVariable int id) {
        Method method = catalog.method(id).orElseThrow(() -> notFound(BillerError.METHOD_NOT_FOUND));
        Country country = catalog.country(method.countryCode()).orElseThrow(() -> notFound(BillerError.COUNTRY_NOT_FOUND));
        return new MethodDetails(method, country.currencies(), catalog.fields(method.templateCode()));
    }

    private static DomainException notFound(BillerError error) {
        return new DomainException(error);
    }
}
