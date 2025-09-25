package com.learning.messaging.producer;

import com.learning.messaging.dto.PublishResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventProducer<T> implements EventProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, T> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    @Override
    public CompletableFuture<PublishResult> send(String topic, T event) {
        return send(topic, null, event);
    }

    @Override
    public CompletableFuture<PublishResult> send(String topic, String key, T event) {

       // this.kafkaTemplate.send(topic, key, event).






        return null;
    }
}
