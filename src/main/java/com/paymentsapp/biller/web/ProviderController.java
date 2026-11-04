package com.paymentsapp.biller.web;

import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Витрина поставщиков: поиск по всем, карточка поставщика, оператор связи по номеру телефона
@RestController
@RequestMapping("/api/biller")
public class ProviderController {

    private final ProviderRepository providers;

    public ProviderController(ProviderRepository providers) {
        this.providers = providers;
    }

    @GetMapping("/providers")
    public List<Provider> search(@RequestParam(required = false) String q, @RequestParam(required = false) String group) {
        return providers.search(q, group);
    }

    @GetMapping("/providers/{code}")
    public ProviderDetails provider(@PathVariable String code) {
        return providers.details(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Поставщик не найден"));
    }

    // +7 996 448-80-61 → Yota. Номер — 10 цифр после +7, мобильные коды начинаются на 9.
    @GetMapping("/mobile/operator")
    public Provider mobileOperator(@RequestParam String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
            digits = digits.substring(1);
        }
        if (digits.length() != 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Номер телефона — 10 цифр после +7");
        }
        if (digits.charAt(0) != '9') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Это не мобильный номер");
        }
        return providers.mobileOperator(digits.substring(0, 3), Integer.parseInt(digits.substring(3)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Не удалось определить оператора по номеру"));
    }
}
