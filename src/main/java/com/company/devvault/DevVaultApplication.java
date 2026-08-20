package com.company.devvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DevVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevVaultApplication.class, args);
    }
}