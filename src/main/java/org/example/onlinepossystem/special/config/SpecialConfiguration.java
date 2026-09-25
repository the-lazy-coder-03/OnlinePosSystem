package org.example.onlinepossystem.special.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class SpecialConfiguration {
    @Bean
    ZoneId businessZone(@Value("${app.business-zone:Africa/Johannesburg}") String zone) {
        return ZoneId.of(zone);
    }

    @Bean
    Clock businessClock(ZoneId businessZone) {
        return Clock.system(businessZone);
    }
}
