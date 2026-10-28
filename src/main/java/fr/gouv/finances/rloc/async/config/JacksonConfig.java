package fr.gouv.finances.rloc.async.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@Configuration
public class JacksonConfig {

    @Bean
    Module jodaModule() {
        return new JodaModule();
    }
}
