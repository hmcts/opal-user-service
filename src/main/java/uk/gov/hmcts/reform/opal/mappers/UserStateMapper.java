package uk.gov.hmcts.reform.opal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.reform.opal.dto.BusinessUnitUserDto;
import uk.gov.hmcts.reform.opal.dto.PermissionDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserStateMapper {

    /**
     * Main mapping method to build the complete UserStateDto.
     * The service now provides a pre-built list of BusinessUnitUserDto objects.
     */
    @Mapping(source = "userEntity.userId", target = "userId")
    @Mapping(source = "userEntity.username", target = "username")
    @Mapping(source = "businessUnitUsers", target = "businessUnitUsers")
    @Mapping(target = "name", ignore = true) // Name is not in db yet, get from auth till we have it in db
    @Mapping(target = "status", ignore = true)//status is not in db yet, placeholder for future use
    @Mapping(target = "version", ignore = true)//version is not in db yet, placeholder for future use
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
}
