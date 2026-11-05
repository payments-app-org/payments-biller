package com.paymentsapp.biller.web;

import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.subscription.Subscription;
import com.paymentsapp.biller.subscription.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// «Счета на оплату»: шаблоны пользователя (X-User-Id — пока без авторизации, как в payment) и их счета у поставщиков
@RestController
@RequestMapping("/api/biller")
public class SubscriptionController {

    private final SubscriptionService subscriptions;

    public SubscriptionController(SubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    // «Выберите категорию»: поставщики, которые выставляют счета
    @GetMapping("/subscription-providers")
    public List<Provider> providers(@RequestParam(required = false) @Size(max = 40) String region) {
        return subscriptions.eligible(region);
    }

    @GetMapping("/subscriptions")
    public List<Subscription> list(@RequestHeader("X-User-Id") UUID userId) {
        return subscriptions.list(userId);
    }

    @GetMapping("/subscriptions/{id}")
    public Subscription.WithBills get(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        return subscriptions.withBills(userId, id);
    }

    @PostMapping("/subscriptions")
    public Subscription.WithBills create(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody Request req) {
        return subscriptions.create(userId, req.providerCode(), req.name(), req.fields());
    }

    @PutMapping("/subscriptions/{id}")
    public Subscription.WithBills update(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id, @Valid @RequestBody Request req) {
        return subscriptions.update(userId, id, req.name(), req.fields());
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        subscriptions.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    public record Request(@NotBlank @Size(max = 40) String providerCode, @Size(max = 100) String name,
                          @Size(max = 20) Map<String, @Size(max = 200) String> fields) {
    }
}
