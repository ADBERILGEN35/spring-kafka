package com.startupheroes.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupheroes.app.dto.MappedPackage;
import com.startupheroes.app.exception.KafkaSerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaProperties springKafkaProperties;

    public void send(MappedPackage mappedPackage) {
        String message = serialize(mappedPackage);
        String topic = springKafkaProperties.getTemplate().getDefaultTopic();

        kafkaTemplate.send(topic, String.valueOf(mappedPackage.id()), message)
                .whenComplete((result, ex) -> {
                    if (nonNull(ex)) {
                        handleSendFailure(mappedPackage.id(), ex);
                    } else {
                        handleSendSuccess(mappedPackage.id(), result);
                    }
                });
    }

    public int sendAll(List<MappedPackage> packages) {
        int successCount = 0;

        for (MappedPackage pkg : packages) {
            send(pkg);
            successCount++;
        }

        log.info("Queued {}/{} packages for Kafka", successCount, packages.size());
        return successCount;
    }


    private String serialize(MappedPackage mappedPackage) {
        try {
            return objectMapper.writeValueAsString(mappedPackage);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize package {}", mappedPackage.id(), ex);
            throw new KafkaSerializationException(mappedPackage.id(), ex);
        }
    }

    /**
     * Production notu:
     * -----------------
     * Gerçek bir production ortamında burada genellikle:
     * - Spring Retry veya Kafka'nın retry politikaları kullanılara belirli sayıda yeniden deneme yapılabilir.
     * Retry sayısı bir üst limite bağlanır (ör. 3–5 deneme) ve bu limit aşıldığındamesaj DLT/DLQ 'ya yönlendirilir.
     * <p>
     * Dead Letter Topic (DLT / DLQ) kullanımı:
     * Gönderilemeyen veya sürekli hata veren mesajlar ayrı bir topic'e yazılır.
     * -----------------
     */
    private void handleSendFailure(Long packageId, Throwable ex) {
        log.error("Failed to send package {} to Kafka", packageId, ex);
        throw new RuntimeException("Kafka send failed for package " + packageId, ex);
    }

    private void handleSendSuccess(Long packageId, SendResult<String, String> result) {
        var metadata = result.getRecordMetadata();
        log.debug("Sent package {}: topic={} partition={} offset={}",
                packageId, metadata.topic(), metadata.partition(), metadata.offset());
    }
}
