package com.startupheroes.app.service;

import com.startupheroes.app.dto.MappedPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaOperationService {

    private final PackageService packageService;
    private final KafkaProducerService kafkaProducerService;

    public Long sendPackageToKafka(Long packageId) {
        log.debug("Sending package to Kafka: packageId={}", packageId);

        MappedPackage mappedPackage = packageService.getMappedPackageById(packageId);
        kafkaProducerService.send(mappedPackage);

        return packageId;
    }

    public int sendAllPackagesToKafka() {
        log.info("Starting Kafka bootstrap for all packages");

        List<MappedPackage> packages = packageService.getAllMappedPackages();
        int sentCount = kafkaProducerService.sendAll(packages);

        log.info("Kafka bootstrap completed: {}/{} packages queued", sentCount, packages.size());
        return sentCount;
    }
}