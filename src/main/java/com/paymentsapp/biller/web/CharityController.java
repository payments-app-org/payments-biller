package com.paymentsapp.biller.web;

import com.paymentsapp.biller.charity.CharitySettlementService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Копилка фондов (внутреннее): что накоплено и не переведено, переводы фондам; ручной запуск «конца дня»
@RestController
@RequestMapping("/api/biller/charity")
public class CharityController {

    private final CharitySettlementService settlements;

    public CharityController(CharitySettlementService settlements) {
        this.settlements = settlements;
    }

    @GetMapping
    public Map<String, Object> state() {
        return Map.of("pending", settlements.pending(), "settlements", settlements.recent());
    }

    @PostMapping("/settle")
    public List<CharitySettlementService.Settlement> settle(@RequestParam(required = false) LocalDate upTo) {
        return settlements.settle(upTo == null ? LocalDate.now(ZoneId.of("Europe/Moscow")) : upTo);
    }
}
