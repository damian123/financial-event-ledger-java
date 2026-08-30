package io.github.damian123.eventledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI eventLedgerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Financial Event Ledger")
                .version("0.1.0")
                .description("Ingest financial posting events, journal them with double-entry bookkeeping, "
                        + "publish through a transactional outbox, and reconcile control totals.")
                .license(new License().name("MIT")));
    }
}
