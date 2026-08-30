package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.dto.IngestEventRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PayloadHasher {

    private PayloadHasher() {
    }

    public static String sha256(IngestEventRequest request) {
        String canonical = String.join(
                "\u001f",
                request.eventId(),
                request.accountId().toString(),
                request.counterAccountId().toString(),
                request.amount(),
                request.currency(),
                request.type().name(),
                request.occurredAt().toString(),
                request.description()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
