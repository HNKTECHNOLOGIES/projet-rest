package fr.gouv.finances.rloc.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RlocAsyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(RlocAsyncApplication.class, args);
    }
}
