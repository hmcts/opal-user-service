package uk.gov.hmcts.reform.opal.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsersV2;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionV2;
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

    @Mapping(target = "cacheName", ignore = true)
    @Mapping(target = "domains", expression = "java(mapUserStateV2DomainsToDto(userStateV2.getDomains()))")
    UserStateV2Dto toUserStateV2Dto(UserStateV2 userStateV2);

    BusinessUnitUserDto toBusinessUnitUserDto(BusinessUnitUser businessUnitUser);

    BusinessUnitUserV2Dto toBusinessUnitUserV2Dto(BusinessUnitUserV2 businessUnitUser);

    @Mapping(source = "businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitId", target = "businessUnitId")
    @Mapping(source = "businessUnitUserRoleList", target = "permissions")
    BusinessUnitUser toBusinessUnitUser(BusinessUnitUserEntity businessUnitUser);

    @Mapping(source = "businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitId", target = "businessUnitId")
    @Mapping(target = "permissions", expression = "java(mapBusinessUserEntityToPermissions(businessUnitUser))")
    BusinessUnitUserV2 toBusinessUnitUserV2(BusinessUnitUserEntity businessUnitUser);

    default Set<PermissionV2> mapBusinessUserEntityToPermissions(BusinessUnitUserEntity businessUnitUser) {
        if (Objects.isNull(businessUnitUser)) {
            return Collections.emptySet();
        }

        return Collections.emptySet();                  //  Place-holder
    }

    @Mapping(source = "userEntity.userId", target = "userId")
    @Mapping(source = "userEntity.username", target = "username")
    @Mapping(source = "userEntity.tokenName", target = "name")
    @Mapping(source = "userEntity", target = "status", qualifiedByName = "mapUpperCaseStatus")
    @Mapping(source = "userEntity.version", target = "version")
    @Mapping(target = "cacheName", ignore = true)
    @Mapping(target = "domains", expression = "java(mapBusinessUnitUsersToDomainBusinessUnitUsers(userEntity"
        + ".getUserId(), userEntity.getBusinessUnitUsers()))")
    UserStateV2 toUserStateV2(UserEntity userEntity, @Context  Clock clock);

    default Map<Domain, DomainBusinessUnitUsersV2> mapBusinessUnitUsersToDomainBusinessUnitUsers(
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

        return groupedByBusinessUnitId.values().stream()
            .flatMap(List::stream)
            .filter(buu -> !duplicatedBusinessUnitIds.contains(buu.getBusinessUnitId()))
            .toList();
    }

    default Map<Domain, DomainBusinessUnitUsersV2> groupBusinessUnitUsersByDomain(
        List<BusinessUnitUserEntity> businessUnitUsers) {

        // Group BU users by domain.
        Map<Domain, List<BusinessUnitUserEntity>> groupedByDomain = new EnumMap<>(Domain.class);
        businessUnitUsers.stream()
            .forEach(businessUnitUser -> {
                Domain mappedDomain = toDomainOrNull(businessUnitUser.getBusinessUnit().getDomain().getName());
                if (mappedDomain != null) {
                    groupedByDomain.computeIfAbsent(mappedDomain, ignored -> new java.util.ArrayList<>())
                        .add(businessUnitUser);
                }
            });

        // Convert the map *values* from Lists to DomainDtos
        EnumMap<Domain, DomainBusinessUnitUsersV2> domains = new EnumMap<>(Domain.class);
        groupedByDomain.forEach((domain, users) -> domains.put(domain, DomainBusinessUnitUsersV2.builder()
            .businessUnitUsers(users.stream()
                .sorted(Comparator.comparing(BusinessUnitUserEntity::getBusinessUnitUserId))
                .map(this::toBusinessUnitUserV2)
                .toList())
            .build()));

        return domains;
    }

    default Map<Domain, DomainV2Dto> mapUserStateV2DomainsToDto(Map<Domain, DomainBusinessUnitUsersV2> domains) {
        if (domains == null || domains.isEmpty()) {
            return new EnumMap<>(Domain.class);
        }

        EnumMap<Domain, DomainV2Dto> mappedDomains = new EnumMap<>(Domain.class);
        domains.forEach((domain, domainBusinessUnitUsers) -> {
            if (domainBusinessUnitUsers == null || domainBusinessUnitUsers.getBusinessUnitUsers() == null) {
                mappedDomains.put(domain, DomainV2Dto.builder().businessUnitUsers(List.of()).build());
                return;
            }

            mappedDomains.put(domain, DomainV2Dto.builder()
                .businessUnitUsers(domainBusinessUnitUsers.getBusinessUnitUsers().stream()
                    .map(this::toBusinessUnitUserV2Dto)
                    .toList())
                .build());
        });

        return mappedDomains;
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
