package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CigaretteDistributionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CigaretteDistributionApplication.class, args);
    }
}
