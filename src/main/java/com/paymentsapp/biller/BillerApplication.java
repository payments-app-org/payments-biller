package com.paymentsapp.biller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class BillerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillerApplication.class, args);
    }
}
