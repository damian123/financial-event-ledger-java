package io.github.damian123.eventledger.api;

import io.github.damian123.eventledger.api.dto.ReconciliationResponse;
import io.github.damian123.eventledger.reconcile.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliations")
@Tag(name = "Reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Compare ingested events against journal lines")
    public ReconciliationResponse run() {
        return reconciliationService.run();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a persisted reconciliation report")
    public ReconciliationResponse get(@PathVariable UUID id) {
        return reconciliationService.get(id);
    }
}
