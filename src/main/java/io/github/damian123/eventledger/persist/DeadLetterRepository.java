package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.DeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, UUID> {

    List<DeadLetter> findAllByOrderByCreatedAtDesc();

    Optional<DeadLetter> findFirstByEventPkAndRequeuedAtIsNullOrderByCreatedAtDesc(UUID eventPk);
}
