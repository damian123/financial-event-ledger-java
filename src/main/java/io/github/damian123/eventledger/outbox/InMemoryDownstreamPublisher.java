package io.github.damian123.eventledger.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryDownstreamPublisher implements DownstreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDownstreamPublisher.class);

    private final List<PublishedEvent> sink = new CopyOnWriteArrayList<>();
    private final AtomicInteger remainingFailures = new AtomicInteger(0);

    public void failNext(int count) {
        remainingFailures.set(count);
    }

    public List<PublishedEvent> published() {
        return List.copyOf(sink);
    }

    public List<PublishedEvent> publishedFor(String eventId) {
        List<PublishedEvent> matches = new ArrayList<>();
        for (PublishedEvent event : sink) {
            if (event.eventId().equals(eventId)) {
                matches.add(event);
            }
        }
        return List.copyOf(matches);
    }

    public void clear() {
        sink.clear();
        remainingFailures.set(0);
    }

    @Override
    public void publish(PublishedEvent event) {
        if (remainingFailures.get() > 0 && remainingFailures.getAndDecrement() > 0) {
            throw new IllegalStateException("injected downstream publish failure");
        }
        sink.add(event);
        log.info("Published outbox event {} (outbox {})", event.eventId(), event.outboxId());
    }
}
