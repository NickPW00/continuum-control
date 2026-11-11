package com.infnet.continuum_control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ContinuumControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContinuumControlApplication.class, args);
    }

}
