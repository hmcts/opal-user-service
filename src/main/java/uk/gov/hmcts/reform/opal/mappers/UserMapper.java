package uk.gov.hmcts.reform.opal.mappers;


import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "userEntity.tokenSubject", target = "subject")
    @Mapping(source = "userEntity.tokenName", target = "name")
    @Mapping(source = "userEntity", target = "status", qualifiedByName = "mapStatus")
    UserDto toUserDto(UserEntity userEntity, @Context Clock clock);

    @Named("mapStatus")
    default String mapStatus(UserEntity userEntity, @Context Clock clock) {
        return userEntity.getStatusFromTime(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))
            .name().toLowerCase();
    }
}
