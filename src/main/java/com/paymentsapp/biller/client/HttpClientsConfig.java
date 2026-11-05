package com.paymentsapp.biller.client;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

// HTTP-клиенты интеграций — интерфейсы @HttpExchange; адрес и тайм-ауты группы — spring.http.serviceclient.<группа>.*
@Configuration
@ImportHttpServices(group = Backends.GATEWAY, types = GatewayApi.class)
@ImportHttpServices(group = Backends.GIS_GMP, types = GisGmpApi.class)
@ImportHttpServices(group = Backends.PROCESSING, types = ProcessingApi.class)
class HttpClientsConfig {
}
