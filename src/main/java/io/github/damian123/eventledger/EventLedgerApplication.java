package io.github.damian123.eventledger;

import io.github.damian123.eventledger.config.LedgerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LedgerProperties.class)
public class EventLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventLedgerApplication.class, args);
    }
}
