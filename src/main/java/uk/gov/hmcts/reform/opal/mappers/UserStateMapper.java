package uk.gov.hmcts.reform.opal.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
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

@Mapper(componentModel = "spring", implementationName = "UserStateMapperImplementation")
public interface UserStateMapper {

    Logger log = LoggerFactory.getLogger(UserStateMapper.class);

    /**
     * Main mapping method to build the complete UserStateDto.
     * The service now provides a pre-built list of BusinessUnitUserDto objects.
     */
    @Mapping(source = "userEntity.userId", target = "userId")
    @Mapping(source = "userEntity.username", target = "username")
    @Mapping(source = "businessUnitUsers", target = "businessUnitUsers")
    @Mapping(source = "userEntity.tokenName", target = "name")
    @Mapping(source = "userEntity", target = "status", qualifiedByName = "mapLowerCaseStatus")
    @Mapping(source = "userEntity.version", target = "version")
    UserStateDto toUserStateDto(UserEntity userEntity,
                                List<BusinessUnitUserDto> businessUnitUsers,
                                @Context Clock clock);

    default UserStateV2Dto toUserStateV2Dto(UserEntity userEntity, @Context Clock clock) {
        return toUserStateV2Dto(toUserStateV2(userEntity, clock));
    }

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "version", target = "version")
    @Mapping(target = "cacheName", ignore = true)
    @Mapping(target = "domains", expression = "java(mapUserStateV2DomainsToDto(userStateV2.getDomains()))")
    UserStateV2Dto toUserStateV2Dto(UserStateV2 userStateV2);

    @Mapping(source = "businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitId", target = "businessUnitId")
    @Mapping(source = "permissions", target = "permissions")
    BusinessUnitUserDto toBusinessUnitUserDto(BusinessUnitUser businessUnitUser);

    @Mapping(source = "businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitId", target = "businessUnitId")
    @Mapping(source = "businessUnitUserRoleList", target = "permissions")
    BusinessUnitUser toBusinessUnitUser(BusinessUnitUserEntity businessUnitUser);

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

        Map<Short, List<BusinessUnitUserEntity>> groupedByBusinessUnitId = businessUnitUsers.stream()
            .filter(Objects::nonNull)
            .filter(buu -> buu.getBusinessUnit() != null)
            .filter(buu -> buu.getBusinessUnit().getDomain() != null)
            .filter(buu -> toDomainOrNull(buu.getBusinessUnit().getDomain().getName()) != null)
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

        // Group BU users by domain.
        Map<Domain, List<BusinessUnitUserEntity>> groupedByDomain = new EnumMap<>(Domain.class);
        groupedByBusinessUnitId.values().stream()
            .flatMap(List::stream)
            .filter(buu -> !duplicatedBusinessUnitIds.contains(buu.getBusinessUnitId()))
            .forEach(businessUnitUser -> {
                Domain mappedDomain = toDomainOrNull(businessUnitUser.getBusinessUnit().getDomain().getName());
                if (mappedDomain != null) {
                    groupedByDomain.computeIfAbsent(mappedDomain, ignored -> new java.util.ArrayList<>())
                        .add(businessUnitUser);
                }
            });

        // Convert the map *values* from Lists to DomainDtos
        EnumMap<Domain, DomainBusinessUnitUsers> domains = new EnumMap<>(Domain.class);
        groupedByDomain.forEach((domain, users) -> domains.put(domain, DomainBusinessUnitUsers.builder()
            .businessUnitUsers(users.stream()
                .sorted(Comparator.comparing(BusinessUnitUserEntity::getBusinessUnitUserId))
                .map(this::toBusinessUnitUser)
                .toList())
            .build()));

        return domains;
    }

    default Map<Domain, DomainDto> mapUserStateV2DomainsToDto(Map<Domain, DomainBusinessUnitUsers> domains) {
        if (domains == null || domains.isEmpty()) {
            return new EnumMap<>(Domain.class);
        }

        EnumMap<Domain, DomainDto> mappedDomains = new EnumMap<>(Domain.class);
        domains.forEach((domain, domainBusinessUnitUsers) -> {
            if (domainBusinessUnitUsers == null || domainBusinessUnitUsers.getBusinessUnitUsers() == null) {
                mappedDomains.put(domain, DomainDto.builder().businessUnitUsers(List.of()).build());
                return;
            }

            mappedDomains.put(domain, DomainDto.builder()
                .businessUnitUsers(domainBusinessUnitUsers.getBusinessUnitUsers().stream()
                    .map(this::toBusinessUnitUserDto)
                    .toList())
                .build());
        });

        return mappedDomains;
    }

    default List<PermissionDto> mapBusinessUnitUserRoleEntitysToPermissionDto(
        Set<BusinessUnitUserRoleEntity> businessUnitUserRoleList) {
        if (businessUnitUserRoleList == null || businessUnitUserRoleList.isEmpty()) {
            return Collections.emptyList();
        }
        return businessUnitUserRoleList.stream()
            .map(BusinessUnitUserRoleEntity::getRole)
            .filter(Objects::nonNull)
            .flatMap(role -> role.getApplicationFunctionList().stream())
            .map(Permissions::toPermissionOrNull)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(Comparator.comparingLong((Permissions permission) -> permission.id))
            .map(permission -> new PermissionDto(permission.id, permission.description))
            .toList();
    }

    default List<PermissionDto> mapPermissionsToPermissionDto(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(Permission::getId))
            .map(permission -> new PermissionDto(permission.getId(), permission.getDescription()))
            .toList();
    }

    default Set<Permission> mapBusinessUnitUsersRoleEntityToPermission(
        Set<BusinessUnitUserRoleEntity> businessUnitUserRoleList) {
        if (businessUnitUserRoleList == null || businessUnitUserRoleList.isEmpty()) {
            return Collections.emptySet();
        }
        return businessUnitUserRoleList.stream()
            .map(BusinessUnitUserRoleEntity::getRole)
            .filter(Objects::nonNull)
            .flatMap(role -> role.getApplicationFunctionList().stream())
            .map(Permissions::toPermissionOrNull)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(Comparator.comparingLong((Permissions permission) -> permission.id))
            .map(permission -> new Permission(permission.id, permission.description))
            .collect(Collectors.toSet());
    }

    private Domain toDomainOrNull(String domainName) {
        if (domainName == null || domainName.isBlank()) {
            return null;
        }
        try {
            return Domain.findByDisplayName(domainName);
        } catch (IllegalArgumentException e) {
            log.error("Domain could not be mapped to enum: {}", domainName);
            return null;
        }
    }

    @Named("mapLowerCaseStatus")
    default String mapLowerCaseStatus(UserEntity userEntity, @Context Clock clock) {
        return mapUpperCaseStatusStr(userEntity, clock).toLowerCase();
    }

    @Named("mapUpperCaseStatusStr")
    default String mapUpperCaseStatusStr(UserEntity userEntity, @Context Clock clock) {
        return userEntity.getStatusFromTime(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)).name();
    }

    @Named("mapUpperCaseStatus")
    default UserStatus mapUpperCaseStatus(UserEntity userEntity, @Context Clock clock) {
        return userEntity.getStatusFromTime(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }
}
