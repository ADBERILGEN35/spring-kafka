package com.startupheroes.app.mapper;

import com.startupheroes.app.dto.MappedPackage;
import com.startupheroes.app.entity.Package;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.isNull;

@Mapper(componentModel = "spring")
public interface PackageMapper {

    String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);


    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toFormattedString")
    @Mapping(target = "lastUpdatedAt", source = "lastUpdatedAt", qualifiedByName = "toFormattedString")
    @Mapping(target = "collectionDuration", ignore = true)
    @Mapping(target = "deliveryDuration", ignore = true)
    @Mapping(target = "leadTime", ignore = true)
    @Mapping(target = "orderInTime", ignore = true)
    MappedPackage toMappedPackage(Package pkg);

    @Named("toFormattedString")
    default String toFormattedString(LocalDateTime dateTime) {
        if (isNull(dateTime)) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }
}