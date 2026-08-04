package at.fraihs.cookoff.shared.config;

import at.fraihs.cookoff.shared.config.jackson.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;

@Configuration
public class JacksonConfig {

    @Bean
    public JacksonModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
