package io.roach.pipeline.item.changefeed;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SubscriberNotFound extends RuntimeException {
    public SubscriberNotFound(Subscriber subscriber) {
        this("No subscriber found with ID: " + subscriber.getId());
    }

    public SubscriberNotFound(String message) {
        super(message);
    }
}
