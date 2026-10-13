package com.medimpact.mt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MemberGroupApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberGroupApplication.class, args);
    }
}
