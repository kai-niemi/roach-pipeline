package io.roach.pipeline.web.cdc;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.roach.pipeline.web.NotFoundException;
import io.roach.pipeline.item.changefeed.ChangeFeedDispatcher;
import io.roach.pipeline.item.changefeed.ChangeFeedEvent;
import io.roach.pipeline.item.changefeed.Subscriber;
import io.roach.pipeline.item.changefeed.SubscriberNotFound;
import io.roach.pipeline.util.LogicalTimestamp;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/cdc2sql")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ChangeFeedWebHookController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ObjectMapper prettyObjectMapper;

    private ChangeFeedDispatcher changeFeedDispatcher;

    private final Counter eventsReceived;

    private final Counter eventsResolved;

    private final Counter eventsRejected;

    private final Counter eventsFailed;

    private final Counter eventsPublished;

    private final ObjectReader objectReader;

    public ChangeFeedWebHookController(
            MeterRegistry meterRegistry,
            @Qualifier("objectMapper") ObjectMapper objectMapper,
            @Qualifier("prettyObjectMapper") ObjectMapper prettyObjectMapper,
            ChangeFeedDispatcher changeFeedDispatcher) {
        this.eventsReceived = meterRegistry.counter("cdc.events.received");
        this.eventsResolved = meterRegistry.counter("cdc.events.resolved");
        this.eventsRejected = meterRegistry.counter("cdc.events.rejected");
        this.eventsFailed = meterRegistry.counter("cdc.events.failed");
        this.eventsPublished = meterRegistry.counter("cdc.events.published");

        this.objectReader = objectMapper.readerFor(ChangeFeedEvent.class);
        this.prettyObjectMapper = prettyObjectMapper;
        this.changeFeedDispatcher = changeFeedDispatcher;
    }

    @PostMapping(value = "/{id}", consumes = {MediaType.ALL_VALUE})
    public ResponseEntity<String> onChangeEvent(@PathVariable("id") String subscriberId, @RequestBody String body) {
        try {
            if (logger.isTraceEnabled()) {
                String prettyJson = prettyObjectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(prettyObjectMapper.readTree(body));
                logger.trace("onChangeEvent for subscriberId [{}]:\n{}", subscriberId, prettyJson);
            }

            // Short-circuit if there's no subscriber to link feeds to
            Optional<Subscriber> subscriber = changeFeedDispatcher.getSubscriber(subscriberId);
            if (!subscriber.isPresent()) {
                throw new SubscriberNotFound(subscriberId);
            }

            eventsReceived.increment();

            ChangeFeedEvent event = objectReader.readValue(body);

            Optional<LogicalTimestamp> timestamp = event.getResolvedTimestamp();
            if (timestamp.isPresent()) {
                eventsResolved.increment();
                changeFeedDispatcher.resolved(subscriber.get(), timestamp.get());
            } else {
                eventsPublished.increment();
                // Block at queue capacity which applies backpressure to CDC publisher
                changeFeedDispatcher.publish(subscriber.get(), event);
            }

            return ResponseEntity.ok().build();
        } catch (SubscriberNotFound e) {
            eventsRejected.increment();
            throw e;
        } catch (JacksonException e) {
            eventsFailed.increment();
            throw new JsonParseException(e);
        }
    }

    @GetMapping(value = "/{id}", consumes = {MediaType.ALL_VALUE})
    public ResponseEntity<SubscriberModel> getSubscriberInfo(@PathVariable("id") String id) {
        Subscriber subscriber = changeFeedDispatcher.getSubscriber(id)
                .orElseThrow(() -> new NotFoundException("No subscriber with ID: " + id));
        return ResponseEntity.ok(SubscriberModel.from(subscriber)
                .add(linkTo(methodOn(getClass())
                        .getSubscriberInfo(id))
                        .withSelfRel()));
    }
}
