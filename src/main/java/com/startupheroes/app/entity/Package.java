package com.startupheroes.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "package")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Package {
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "arrival_for_delivery_at")
    private LocalDateTime arrivalForDeliveryAt;

    @Column(name = "arrival_for_pickup_at")
    private LocalDateTime arrivalForPickupAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancelled",nullable = false)
    private Boolean cancelled;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "in_delivery_at")
    private LocalDateTime inDeliveryAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "eta")
    private Integer eta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PackageStatus status;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "origin_address_id")
    private Long originAddressId;

    @Column(name = "type")
    private String type;

    @Column(name = "waiting_for_assignment_at")
    private LocalDateTime waitingForAssignmentAt;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "collected")
    private Integer collected;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "reassigned")
    private Integer reassigned;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
}
