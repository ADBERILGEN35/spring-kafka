package com.startupheroes.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Mapped package data transfer object")
public record MappedPackage(

        @Schema(description = "Package ID", example = "1")
        Long id,

        @Schema(description = "Creation timestamp", example = "2021-11-13 10:47:52.675248")
        String createdAt,

        @Schema(description = "Last update timestamp", example = "2021-11-13 11:40:15.314340")
        String lastUpdatedAt,

        @Schema(description = "Collection duration in minutes", example = "30")
        Integer collectionDuration,

        @Schema(description = "Delivery duration in minutes", example = "45")
        Integer deliveryDuration,

        @Schema(description = "Estimated time of arrival in minutes", example = "75")
        Integer eta,

        @Schema(description = "Lead time in minutes", example = "60")
        Integer leadTime,

        @Schema(description = "Whether order was delivered on time")
        Boolean orderInTime
) {
}