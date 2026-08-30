package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    List<JournalLine> findByEventIdOrderByPostedAtAsc(String eventId);

    List<JournalLine> findByAccountIdOrderByPostedAtAsc(UUID accountId);

    @Query("""
            select sum(j.amount)
            from JournalLine j
            where j.accountId = :accountId and j.direction = io.github.damian123.eventledger.domain.JournalDirection.DEBIT
            """)
    BigDecimal debitTotal(@Param("accountId") UUID accountId);

    @Query("""
            select sum(j.amount)
            from JournalLine j
            where j.accountId = :accountId and j.direction = io.github.damian123.eventledger.domain.JournalDirection.CREDIT
            """)
    BigDecimal creditTotal(@Param("accountId") UUID accountId);
}
