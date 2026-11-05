package com.paymentsapp.biller.client;

import com.paymentsapp.biller.gov.GisGmpClient.ExternalCharge;
import java.util.List;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

// ГИС ГМП (заглушка в providers): начисления по УИН или по идентификаторам плательщика
@HttpExchange("/gis-gmp")
public interface GisGmpApi {

    @GetExchange("/charges")
    List<ExternalCharge> charges(@RequestParam MultiValueMap<String, String> query);
}
