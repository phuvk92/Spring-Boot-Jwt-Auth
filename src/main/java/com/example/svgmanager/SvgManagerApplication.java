package com.example.svgmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SvgManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SvgManagerApplication.class, args);
    }
}
