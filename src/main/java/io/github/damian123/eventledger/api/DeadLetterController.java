package io.github.damian123.eventledger.api;

import io.github.damian123.eventledger.api.dto.DeadLetterResponse;
import io.github.damian123.eventledger.retry.DeadLetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dead-letters")
@Tag(name = "Dead letters")
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    public DeadLetterController(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @GetMapping
    @Operation(summary = "List dead-lettered outbox work")
    public List<DeadLetterResponse> list() {
        return deadLetterService.list().stream().map(DeadLetterResponse::from).toList();
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Requeue a dead letter onto the outbox")
    public DeadLetterResponse retry(@PathVariable UUID id) {
        return DeadLetterResponse.from(deadLetterService.retry(id));
    }
}
