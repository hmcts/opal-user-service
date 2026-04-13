package uk.gov.hmcts.reform.opal.mappers;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "tokenSubject", target = "subject")
    @Mapping(source = "tokenName", target = "name")
    @Mapping(target = "status", constant = "active")
    UserDto toUserDto(UserEntity userEntity);

}
