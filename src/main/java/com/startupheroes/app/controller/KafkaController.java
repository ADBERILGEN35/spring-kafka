package com.startupheroes.app.controller;

import com.startupheroes.app.dto.response.ApiResponse;
import com.startupheroes.app.service.KafkaOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
@Tag(name = "Kafka Operations")
public class KafkaController {

    private final KafkaOperationService kafkaOperationService;

    @PostMapping("/send/{packageId}")
    @Operation(summary = "Send a package to Kafka")
    public ApiResponse<Long> sendPackage(@PathVariable @Positive Long packageId) {
        log.info("Sending package to Kafka: packageId={}", packageId);
        Long sentPackageId = kafkaOperationService.sendPackageToKafka(packageId);

        return ApiResponse.success("Package sent to Kafka successfully", sentPackageId);
    }

    @PostMapping("/bootstrap")
    @Operation(summary = "Bootstrap all packages to Kafka")
    public ApiResponse<Integer> bootstrapPackages() {

        log.debug("Starting Kafka bootstrap");
        int sentCount = kafkaOperationService.sendAllPackagesToKafka();

        return ApiResponse.success("All packages sent successfully", sentCount);
    }
}