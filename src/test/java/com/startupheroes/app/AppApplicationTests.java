package com.startupheroes.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupheroes.app.entity.Package;
import com.startupheroes.app.entity.PackageStatus;
import com.startupheroes.app.repository.PackageRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"package-events-test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CaseApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        packageRepository.deleteAll();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("package-events-test"));
    }

    @Test
    void shouldSendSinglePackageToKafka() throws Exception {
        Package pkg = createCompletedPackage(1L);
        packageRepository.save(pkg);

        mockMvc.perform(post("/kafka/send/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1));

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isEqualTo(1);

        String message = records.iterator().next().value();
        JsonNode json = objectMapper.readTree(message);

        assertThat(json.get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("eta").asInt()).isEqualTo(277);
        assertThat(json.get("leadTime").asInt()).isEqualTo(52);
        assertThat(json.get("orderInTime").asBoolean()).isTrue();
    }

    @Test
    void shouldNotSendCancelledPackageToKafka() throws Exception {
        Package pkg = createCancelledPackage(2L);
        packageRepository.save(pkg);

        mockMvc.perform(post("/kafka/send/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnNotFoundForNonExistentPackage() throws Exception {
        mockMvc.perform(post("/kafka/send/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Package not found with id: 999"));
    }

    @Test
    void shouldSendAllNonCancelledPackagesToKafka() throws Exception {
        packageRepository.save(createCompletedPackage(1L));
        packageRepository.save(createCancelledPackage(2L));
        packageRepository.save(createInProgressPackage(3L));

        mockMvc.perform(post("/kafka/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(2));

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isEqualTo(2);
    }

    @Test
    void shouldSetNullFieldsForNonCompletedPackage() throws Exception {
        Package pkg = createInProgressPackage(4L);
        packageRepository.save(pkg);

        mockMvc.perform(post("/kafka/send/4"))
                .andExpect(status().isOk());

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isEqualTo(1);

        String message = records.iterator().next().value();
        JsonNode json = objectMapper.readTree(message);

        assertThat(json.get("id").asLong()).isEqualTo(4L);
        assertThat(json.get("leadTime").isNull()).isTrue();
        assertThat(json.get("orderInTime").isNull()).isTrue();
        assertThat(json.get("deliveryDuration").isNull()).isTrue();
    }

    @Test
    void shouldRejectInvalidPackageId() throws Exception {
        mockMvc.perform(post("/kafka/send/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Package createCompletedPackage(Long id) {
        return Package.builder()
                .id(id)
                .createdAt(LocalDateTime.of(2021, 11, 13, 10, 47, 52, 675248000))
                .lastUpdatedAt(LocalDateTime.of(2021, 11, 13, 11, 40, 15, 314340000))
                .completedAt(LocalDateTime.of(2021, 11, 13, 11, 40, 15, 314340000))
                .pickedUpAt(LocalDateTime.of(2021, 11, 13, 10, 49, 50, 278087000))
                .inDeliveryAt(LocalDateTime.of(2021, 11, 13, 11, 5, 56, 861614000))
                .arrivalForDeliveryAt(LocalDateTime.of(2021, 11, 13, 11, 5, 58, 45640000))
                .arrivalForPickupAt(LocalDateTime.of(2021, 11, 13, 10, 48, 37, 32078000))
                .waitingForAssignmentAt(LocalDateTime.of(2021, 11, 13, 10, 47, 52, 675248000))
                .collectedAt(LocalDateTime.of(2021, 11, 13, 10, 47, 52, 828692000))
                .eta(277)
                .status(PackageStatus.COMPLETED)
                .cancelled(false)
                .collected(1)
                .customerId(20002011575015L)
                .storeId(20000000004103L)
                .originAddressId(999000020443388L)
                .userId(50002010395213L)
                .orderId(123972783L)
                .type("REGULAR")
                .deliveryDate(LocalDate.of(2021, 11, 13))
                .build();
    }

    private Package createCancelledPackage(Long id) {
        return Package.builder()
                .id(id)
                .createdAt(LocalDateTime.of(2021, 11, 13, 10, 55, 0))
                .lastUpdatedAt(LocalDateTime.of(2021, 11, 13, 11, 10, 0))
                .cancelledAt(LocalDateTime.of(2021, 11, 13, 11, 10, 0))
                .cancelReason("Customer requested cancellation")
                .eta(300)
                .status(PackageStatus.CANCELLED)
                .cancelled(true)
                .collected(0)
                .customerId(20002011575016L)
                .storeId(20000000004103L)
                .originAddressId(999000020443389L)
                .userId(50002010395214L)
                .orderId(123972784L)
                .type("REGULAR")
                .deliveryDate(LocalDate.of(2021, 11, 13))
                .build();
    }

    private Package createInProgressPackage(Long id) {
        return Package.builder()
                .id(id)
                .createdAt(LocalDateTime.of(2021, 11, 13, 11, 50, 0))
                .lastUpdatedAt(LocalDateTime.of(2021, 11, 13, 12, 15, 0))
                .pickedUpAt(LocalDateTime.of(2021, 11, 13, 12, 5, 0))
                .inDeliveryAt(LocalDateTime.of(2021, 11, 13, 12, 15, 0))
                .arrivalForPickupAt(LocalDateTime.of(2021, 11, 13, 12, 0, 0))
                .collectedAt(LocalDateTime.of(2021, 11, 13, 11, 55, 0))
                .eta(250)
                .status(PackageStatus.IN_DELIVERY)
                .cancelled(false)
                .collected(1)
                .customerId(20002011575017L)
                .storeId(20000000004103L)
                .originAddressId(999000020443390L)
                .userId(50002010395215L)
                .orderId(123972785L)
                .type("EXPRESS")
                .deliveryDate(LocalDate.of(2021, 11, 13))
                .build();
    }
}