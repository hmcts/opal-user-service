package uk.gov.hmcts.reform.opal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.generated.model.BusinessUnitUserCommon;
import uk.gov.hmcts.opal.generated.model.DomainUserState;
import uk.gov.hmcts.opal.generated.model.DomainsUserState;
import uk.gov.hmcts.opal.generated.model.UserStateV2UserState;
import uk.gov.hmcts.reform.opal.dto.UserStateV1;
import uk.gov.hmcts.reform.opal.dto.UserStateV2;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", implementationName = "UserStateMapperImplementation")
public interface UserStateMapper {

    /**
     * Main mapping method to build the complete UserStateDto.
     * The service now provides a pre-built list of BusinessUnitUserDto objects.
     */
    @Mapping(source = "userEntity.userId", target = "userId")
    @Mapping(source = "userEntity.username", target = "username")
    @Mapping(source = "businessUnitUsers", target = "businessUnitUsers")
    @Mapping(source = "userEntity.tokenName", target = "name")
    @Mapping(source = "userEntity.status", target = "status")
    @Mapping(source = "userEntity.version", target = "version")
    UserStateDto toUserStateDto(UserEntity userEntity, List<BusinessUnitUserDto> businessUnitUsers);

    /**
     * Helper method called by the service to map a BusinessUnitUserEntity and its associated
     * list of entitlements into a BusinessUnitUserDto.
     */
    @Mapping(source = "businessUnitUser.businessUnitUserId", target = "businessUnitUserId")
    @Mapping(source = "businessUnitUser.businessUnitId", target = "businessUnitId")
    @Mapping(source = "entitlements", target = "permissions")
    BusinessUnitUserDto toBusinessUnitUserDto(BusinessUnitUserEntity businessUnitUser,
                                              List<UserEntitlementEntity> entitlements);

    /**
     * Helper method to map a UserEntitlementEntity to a PermissionDto.
     */
    @Mapping(source = "applicationFunction.applicationFunctionId", target = "permissionId")
    @Mapping(source = "applicationFunction.functionName", target = "permissionName")
    PermissionDto toPermissionDto(UserEntitlementEntity entitlementEntity);

    @Mapping(source = "businessUnitUsers", target = "businessUnitUsers")
    @Mapping(source = "version", target = "version", qualifiedByName = "mapToBigInteger")
    UserStateV1 toV1(UserStateDto userStateDto);

    @Mapping(source = "username", target = "cacheName")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "version", target = "version", qualifiedByName = "mapToBigInteger")
    @Mapping(source = "businessUnitUsers", target = "domains")
    UserStateV2 toV2(UserStateDto userStateDto);

    @Mapping(source = "businessUnitId", target = "businessUnitId")
    @Mapping(target = "businessUnitName", ignore = true)
    @Mapping(source = "permissions", target = "roles")
    BusinessUnitUserCommon toBusinessUnitUserCommon(BusinessUnitUserDto businessUnitUserDto);

    default Integer map(Short value) {
        return Optional.ofNullable(value).map(Integer::valueOf).orElse(null);
    }

    default Integer map(Long value) {
        return Optional.ofNullable(value).map(Math::toIntExact).orElse(null);
    }

    default UserStateV2UserState.StatusEnum map(String value) {
        return Optional.ofNullable(value)
            .map(UserStateV2UserState.StatusEnum::fromValue)
            .orElse(null);
    }

    default DomainsUserState map(List<BusinessUnitUserDto> businessUnitUsers) {
        return DomainsUserState.builder()
            .fines(DomainUserState.builder()
                       .businessUnitUsers(mapBusinessUnitUsers(businessUnitUsers))
                       .build())
            .build();
    }

    @Named("mapToBigInteger")
    default BigInteger mapToBigInteger(Long value) {
        return Optional.ofNullable(value).map(BigInteger::valueOf).orElse(null);
    }

    default List<String> mapPermissions(List<PermissionDto> permissions) {
        return Optional.ofNullable(permissions)
            .orElse(Collections.emptyList())
            .stream()
            .map(PermissionDto::getPermissionName)
            .toList();
    }

    default List<BusinessUnitUserCommon> mapBusinessUnitUsers(List<BusinessUnitUserDto> businessUnitUsers) {
        return Optional.ofNullable(businessUnitUsers)
            .map(list -> list.stream()
                .map(this::toBusinessUnitUserCommon)
                .toList())
            .orElse(null);
    }
}
