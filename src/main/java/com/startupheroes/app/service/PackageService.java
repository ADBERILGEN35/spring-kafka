package com.startupheroes.app.service;

import com.startupheroes.app.dto.MappedPackage;
import com.startupheroes.app.entity.Package;
import com.startupheroes.app.entity.PackageStatus;
import com.startupheroes.app.exception.PackageCancelledException;
import com.startupheroes.app.exception.PackageNotFoundException;
import com.startupheroes.app.mapper.PackageMapper;
import com.startupheroes.app.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.startupheroes.app.entity.PackageStatus.COMPLETED;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageService {

    private final PackageRepository packageRepository;
    private final PackageMapper packageMapper;

    public MappedPackage getMappedPackageById(Long id) {
        log.debug("Fetching package: id={}", id);

        Package pkg = findByIdOrThrow(id);
        validateNotCancelled(pkg);

        return toMappedPackage(pkg);
    }

    public List<MappedPackage> getAllMappedPackages() {
        log.debug("Fetching all non-cancelled packages");

        List<MappedPackage> packages = packageRepository.findAllByCancelledFalse()
                .stream()
                .map(this::toMappedPackage)
                .toList();

        log.debug("Found {} packages", packages.size());
        return packages;
    }

    private Package findByIdOrThrow(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new PackageNotFoundException(id));
    }

    private void validateNotCancelled(Package pkg) {
        if (TRUE.equals(pkg.getCancelled())) {
            throw new PackageCancelledException(pkg.getId());
        }
    }

    private MappedPackage toMappedPackage(Package pkg) {
        MappedPackage base = packageMapper.toMappedPackage(pkg);

        if (!COMPLETED.equals(pkg.getStatus())) {
            return base;
        }

        Integer leadTime = minutesBetween(pkg.getCreatedAt(), pkg.getCompletedAt());
        Integer collectionDuration = minutesBetween(pkg.getCreatedAt(), pkg.getPickedUpAt());
        Integer deliveryDuration = minutesBetween(pkg.getInDeliveryAt(), pkg.getCompletedAt());

        return MappedPackage.builder()
                .id(base.id())
                .createdAt(base.createdAt())
                .lastUpdatedAt(base.lastUpdatedAt())
                .eta(base.eta())
                .collectionDuration(collectionDuration)
                .deliveryDuration(deliveryDuration)
                .leadTime(leadTime)
                .orderInTime(isOrderInTime(leadTime, base.eta()))
                .build();
    }


    private Integer minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (isNull(start) || isNull(end)) {
            return null;
        }
        return (int) ChronoUnit.MINUTES.between(start, end);
    }


    private Boolean isOrderInTime(Integer leadTime, Integer eta) {
        if (isNull(leadTime) || isNull(eta)) {
            return null;
        }
        return leadTime <= eta;
    }
}