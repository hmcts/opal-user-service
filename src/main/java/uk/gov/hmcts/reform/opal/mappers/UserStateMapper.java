package uk.gov.hmcts.reform.opal.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.DomainDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    @Mapping(target = "status", constant = "active")
    @Mapping(source = "userEntity.version", target = "version")
    UserStateDto toUserStateDto(UserEntity userEntity, List<BusinessUnitUserDto> businessUnitUsers);

    @Mapping(source = "userEntity.userId", target = "userId")
    @Mapping(source = "userEntity.username", target = "username")
    @Mapping(source = "userEntity.tokenName", target = "name")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(source = "userEntity.version", target = "version")
    @Mapping(target = "cacheName", ignore = true)
    @Mapping(target = "domains", expression = "java(mapBusinessUnitUsersToDomains(userEntity.getBusinessUnitUsers()))")
    UserStateV2Dto toUserStateV2Dto(UserEntity userEntity);

    /**
     * Helper method called by the service to map a BusinessUnitUserEntity and its associated
     * list of entitlements into a BusinessUnitUserDto.
     */
    @Mapping(source = "businessUnitUser.businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitUser.businessUnitId", target = "businessUnitId")
    @Mapping(source = "entitlements", target = "permissions")
    BusinessUnitUserDto toBusinessUnitUserDto(BusinessUnitUserEntity businessUnitUser,
                                              List<UserEntitlementEntity> entitlements);

    @Mapping(source = "businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitId", target = "businessUnitId")
    @Mapping(source = "businessUnitUserRoleList", target = "permissions")
    BusinessUnitUserDto toBusinessUnitUserDto(BusinessUnitUserEntity businessUnitUser);

    /**
     * Helper method to map a UserEntitlementEntity to a PermissionDto.
     */
    @Mapping(source = "applicationFunction.applicationFunctionId", target = "permissionId")
    @Mapping(source = "applicationFunction.functionName", target = "permissionName")
    PermissionDto toPermissionDto(UserEntitlementEntity entitlementEntity);

    default Map<Domain, DomainDto> mapBusinessUnitUsersToDomains(Set<BusinessUnitUserEntity> businessUnitUsers) {

        if (businessUnitUsers == null || businessUnitUsers.isEmpty()) {
            return new EnumMap<>(Domain.class);
        }

        // Group BU users by domain.
        Map<Domain, List<BusinessUnitUserEntity>> groupedByDomain = new EnumMap<>(Domain.class);
        businessUnitUsers.stream()
            .filter(Objects::nonNull)
            .filter(buu -> buu.getBusinessUnit() != null)
            .filter(buu -> buu.getBusinessUnit().getDomain() != null)
            .forEach(businessUnitUser -> {
                Domain mappedDomain = toDomainOrNull(businessUnitUser.getBusinessUnit().getDomain().getName());
                if (mappedDomain != null) {
                    groupedByDomain.computeIfAbsent(mappedDomain, ignored -> new java.util.ArrayList<>())
                        .add(businessUnitUser);
                }
            });

        // Convert the map *values* from Lists to DomainDtos
        EnumMap<Domain, DomainDto> domains = new EnumMap<>(Domain.class);
        groupedByDomain.forEach((domain, users) -> domains.put(domain, DomainDto.builder()
            .businessUnitUsers(users.stream()
                .sorted(Comparator.comparing(BusinessUnitUserEntity::getBusinessUnitUserId))
                .map(this::toBusinessUnitUserDto)
                .toList())
            .build()));

        return domains;
    }

    default List<PermissionDto> map(Set<BusinessUnitUserRoleEntity> businessUnitUserRoleList) {
        if (businessUnitUserRoleList == null || businessUnitUserRoleList.isEmpty()) {
            return Collections.emptyList();
        }
        return businessUnitUserRoleList.stream()
            .map(BusinessUnitUserRoleEntity::getRole)
            .filter(Objects::nonNull)
            .filter(RoleEntity::isActive)
            .flatMap(role -> role.getApplicationFunctionList().stream())
            .map(Permissions::toPermissionOrNull)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(Comparator.comparingLong((Permissions permission) -> permission.id))
            .map(permission -> new PermissionDto(permission.id, permission.description))
            .toList();
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
}
