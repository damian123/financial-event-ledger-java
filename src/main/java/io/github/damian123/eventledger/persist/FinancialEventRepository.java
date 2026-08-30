package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.EventStatus;
import io.github.damian123.eventledger.domain.FinancialEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialEventRepository extends JpaRepository<FinancialEvent, UUID> {

    Optional<FinancialEvent> findByEventId(String eventId);

    Optional<FinancialEvent> findByIdempotencyKey(String idempotencyKey);

    List<FinancialEvent> findByStatus(EventStatus status);

    @Query("""
            select e.eventId
            from FinancialEvent e
            group by e.eventId
            having count(e) > 1
            """)
    List<String> findDuplicateEventIds();
}
