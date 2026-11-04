package com.paymentsapp.biller.web;

import com.paymentsapp.biller.gov.GovCharge;
import com.paymentsapp.biller.gov.GovChargeService;
import com.paymentsapp.biller.gov.PayerIds;
import com.paymentsapp.biller.provider.GovDetails;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Госуслуги: поиск начислений у поставщика и карточка начисления (её проверяет payment перед оплатой).
// provider=gibdd&sts=…&vu=… · provider=fns-uin&uin=… · provider=fns-inn&inn=… · provider=fssp&passport=…&inn=…
// Без provider — общий поиск по УИН (provider=uin).
@RestController
@RequestMapping("/api/biller/gov")
public class GovController {

    private final GovChargeService charges;
    private final ProviderRepository providers;

    public GovController(GovChargeService charges, ProviderRepository providers) {
        this.charges = charges;
        this.providers = providers;
    }

    @GetMapping("/charges")
    public List<GovCharge> find(@RequestParam(defaultValue = "uin") String provider, @RequestParam Map<String, String> params) {
        Provider p = providers.byCode(provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Поставщик не найден"));
        GovDetails gov = providers.gov(p.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Это не госуслуга"));
        return charges.find(p.id(), gov, PayerIds.check(gov.lookup(), gov.uinPrefix(), params));
    }

    @GetMapping("/charges/{id}")
    public GovCharge get(@PathVariable UUID id) {
        return charges.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Начисление не найдено"));
    }
}
