package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.OutboxMessage;
import io.github.damian123.eventledger.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    Optional<OutboxMessage> findByEventPk(UUID eventPk);

    List<OutboxMessage> findByEventId(String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OutboxMessage o where o.status = :status order by o.createdAt asc")
    List<OutboxMessage> lockByStatus(@Param("status") OutboxStatus status, Pageable pageable);
}
