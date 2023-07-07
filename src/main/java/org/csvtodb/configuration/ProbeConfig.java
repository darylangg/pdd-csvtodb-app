package org.csvtodb.configuration;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProbeConfig {

    @Autowired
    private CamelContext camelContext;

    @Bean
    public HealthIndicator camelHealthIndicator() {
        return () -> {
            if (camelContext.isStarted()) {
                return Health.up().withDetail("test","test").build();
            } else {
                return Health.down().build();
            }
        };
    }
}