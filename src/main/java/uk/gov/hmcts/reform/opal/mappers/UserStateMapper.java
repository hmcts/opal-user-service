package uk.gov.hmcts.reform.opal.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.mapper.CommonUserStateMapper;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserStateMapper {

    Logger log = LoggerFactory.getLogger(UserStateMapper.class);

    CommonUserStateMapper commonUserStateMapper = Mappers.getMapper(CommonUserStateMapper.class);

    default UserStateV2Dto toUserStateV2Dto(UserEntity userEntity, @Context Clock clock) {
        return commonUserStateMapper.toUserStateV2Dto(toUserStateV2(userEntity, clock));
    }


    @Mapping(target = "permissions", source = "businessUnitUserRoleList", qualifiedByName = "mapRolesToPermissions")
    BusinessUnitUserV2 toBusinessUnitUserV2(BusinessUnitUserEntity businessUnitUser);

    @Mapping(source = "userEntity.userId", target = "userId")
    @Mapping(source = "userEntity.username", target = "username")
    @Mapping(source = "userEntity.tokenName", target = "name")
    @Mapping(source = "userEntity", target = "status", qualifiedByName = "mapUpperCaseStatus")
    @Mapping(source = "userEntity.version", target = "version")
    @Mapping(target = "cacheName", ignore = true)
    @Mapping(target = "domains", expression = "java(mapBusinessUnitUsersToDomainBusinessUnitUsers(userEntity"
        + ".getUserId(), userEntity.getBusinessUnitUsers()))")
    UserStateV2 toUserStateV2(UserEntity userEntity, @Context  Clock clock);

    default Map<Domain, DomainBusinessUnitUsers> mapBusinessUnitUsersToDomainBusinessUnitUsers(
        Long userId,
        Set<BusinessUnitUserEntity> businessUnitUsers) {

        if (businessUnitUsers == null || businessUnitUsers.isEmpty()) {
            return new EnumMap<>(Domain.class);
        }

        List<BusinessUnitUserEntity> validBusinessUnitUsers =
            filterValidBusinessUnitUsers(userId, businessUnitUsers);

        return groupBusinessUnitUsersByDomain(validBusinessUnitUsers);
    }

    default List<BusinessUnitUserEntity> filterValidBusinessUnitUsers(
        Long userId,
        Set<BusinessUnitUserEntity> businessUnitUsers) {

        Map<Short, List<BusinessUnitUserEntity>> groupedByBusinessUnitId = businessUnitUsers.stream()
            .filter(Objects::nonNull)
            .filter(buu -> buu.getBusinessUnit() != null)
            .filter(buu -> buu.getBusinessUnit().getDomain() != null)
            .filter(buu -> commonUserStateMapper.toDomainOrNull(buu.getBusinessUnit().getDomain().getName()) != null)
            .collect(Collectors.groupingBy(BusinessUnitUserEntity::getBusinessUnitId));

        Set<Short> duplicatedBusinessUnitIds = groupedByBusinessUnitId.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        duplicatedBusinessUnitIds.stream()
            .sorted()
            .forEach(businessUnitId -> log.warn(
                "Duplicate business unit user mappings found for user {} and business unit {}: {}; "
                    + "ignoring all entries for that business unit",
                userId,
                businessUnitId,
                groupedByBusinessUnitId.get(businessUnitId).stream()
                    .map(BusinessUnitUserEntity::getBusinessUnitUserId)
                    .sorted()
                    .toList()));

        return groupedByBusinessUnitId.values().stream()
            .flatMap(List::stream)
            .filter(buu -> !duplicatedBusinessUnitIds.contains(buu.getBusinessUnitId()))
            .toList();
    }

    default Map<Domain, DomainBusinessUnitUsers> groupBusinessUnitUsersByDomain(
        List<BusinessUnitUserEntity> businessUnitUsers) {

        // Group BU users by domain.
        Map<Domain, List<BusinessUnitUserEntity>> groupedByDomain = new EnumMap<>(Domain.class);
        businessUnitUsers.forEach(businessUnitUser -> {
            Domain mappedDomain = commonUserStateMapper
                .toDomainOrNull(businessUnitUser.getBusinessUnit().getDomain().getName());
            if (mappedDomain != null) {
                groupedByDomain.computeIfAbsent(mappedDomain, ignored -> new java.util.ArrayList<>())
                    .add(businessUnitUser);
            }
        });

        // Convert the map *values* from Lists to DomainDto
        EnumMap<Domain, DomainBusinessUnitUsers> domains = new EnumMap<>(Domain.class);
        groupedByDomain.forEach((domain, users) -> domains.put(domain, DomainBusinessUnitUsers.builder()
            .businessUnitUsers(users.stream()
                .sorted(Comparator.comparing(BusinessUnitUserEntity::getBusinessUnitUserId))
                .map(this::toBusinessUnitUserV2)
                .toList())
            .build()));

        return domains;
    }

    @Named("mapRolesToPermissions")
    default Set<PermissionV2> mapBusinessUnitUsersRoleEntityToPermissionV2(
        Set<BusinessUnitUserRoleEntity> businessUnitUserRoleList) {
        if (businessUnitUserRoleList == null || businessUnitUserRoleList.isEmpty()) {
            return Collections.emptySet();
        }

        return businessUnitUserRoleList.stream()
            .map(BusinessUnitUserRoleEntity::getRole)
            .filter(Objects::nonNull)
            .flatMap(role -> role.getApplicationFunctionList().stream())
            .map(PermissionV2::fromPermissionName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    @Named("mapUpperCaseStatus")
    default UserStatus mapUpperCaseStatus(UserEntity userEntity, @Context Clock clock) {
        return userEntity.getStatusFromTime(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }
}
