package io.github.damian123.eventledger.outbox;

public interface DownstreamPublisher {

    void publish(PublishedEvent event);
}
