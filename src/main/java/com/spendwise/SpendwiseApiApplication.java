package com.spendwise;

import com.spendwise.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SpendwiseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpendwiseApiApplication.class, args);
    }
}

