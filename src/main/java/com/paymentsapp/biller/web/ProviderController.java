package com.paymentsapp.biller.web;

import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderRepository;
import com.paymentsapp.platform.error.DomainException;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Витрина поставщиков: поиск по всем, карточка поставщика, оператор связи по номеру телефона
@RestController
@RequestMapping("/api/biller")
public class ProviderController {

    private final ProviderRepository providers;

    public ProviderController(ProviderRepository providers) {
        this.providers = providers;
    }

    @GetMapping("/providers")
    public List<Provider> search(@RequestParam(required = false) @Size(max = 100) String q,
                                 @RequestParam(required = false) @Size(max = 40) String group,
                                 @RequestParam(required = false) @Size(max = 40) String region) {
        return providers.search(q, group, region);
    }

    // Регионы для фильтра «Регион: Москва и Московская область»
    @GetMapping("/regions")
    public List<ProviderRepository.Region> regions() {
        return providers.regions();
    }

    @GetMapping("/providers/{code}")
    public ProviderDetails provider(@PathVariable @Size(max = 40) String code) {
        return providers.details(code)
                .orElseThrow(() -> new DomainException(BillerError.PROVIDER_NOT_FOUND));
    }

    // +7 996 448-80-61 → Yota. Номер — 10 цифр после +7, мобильные коды начинаются на 9.
    @GetMapping("/mobile/operator")
    public Provider mobileOperator(@RequestParam @Size(max = 20) String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
            digits = digits.substring(1);
        }
        if (digits.length() != 10) {
            throw new DomainException(BillerError.PHONE_DIGITS);
        }
        if (digits.charAt(0) != '9') {
            throw new DomainException(BillerError.NOT_MOBILE_NUMBER);
        }
        return providers.mobileOperator(digits.substring(0, 3), Integer.parseInt(digits.substring(3)))
                .orElseThrow(() -> new DomainException(BillerError.OPERATOR_NOT_FOUND));
    }
}
