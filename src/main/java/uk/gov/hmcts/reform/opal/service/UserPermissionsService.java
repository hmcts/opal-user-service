package uk.gov.hmcts.reform.opal.service;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.dto.BusinessUnitUserDto;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.entity.UserStatus;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsService")
public class UserPermissionsService {

    //The name claim of the authorised user.
    private static final String NAME_CLAIM = "name";

    //The claim used to map the authorised user to the user entity.
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String SUB_CLAIM = "sub";

    private final UserEntitlementRepository userEntitlementRepository;
    private final UserRepository userRepository;
    private final UserStateMapper userStateMapper;
    private final UserMapper userMapper;
    private final AccessTokenService tokenService;

    @Transactional(readOnly = true)
    public UserStateDto getUserState(Long userId, Authentication authentication) {
        log.debug(":getUserState: userId: {}", userId);

        if (userId == 0) {

            String username = extractClaimAsString(authentication, PREFERRED_USERNAME_CLAIM);
            String name = extractClaimAsString(authentication, NAME_CLAIM);

            log.debug(":getUserState: userId is 0, using username: {}", username);

            UserStateDto userStateDto = getUserState(username);
            userStateDto.setName(name);
            return userStateDto;

        } else {
            return getUserState(userId);
        }
    }

    public UserStateDto getUserState(Long userId) {

        // 1. Get all entitlements for the user.
        Set<UserEntitlementEntity> entitlements = userEntitlementRepository.findAllByUserIdWithFullJoins(userId);

        // 2. If the set is empty, check if the user actually exists.
        if (entitlements.isEmpty()) {
            UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
            // User exists but has no entitlements, so return the DTO with an empty list.
            return userStateMapper.toUserStateDto(userEntity, Collections.emptyList());
        }

        // 3. Get the UserEntity from an arbitrary element in the set.
        UserEntity userEntity = entitlements.iterator().next().getBusinessUnitUser().getUser();

        // 4. Group entitlements by the BusinessUnitUser's ID (a String)
        Map<String, List<UserEntitlementEntity>> entitlementsByBuuId = entitlements.stream()
            .collect(Collectors.groupingBy(UserEntitlementEntity::getBusinessUnitUserId));

        // 5. Create a map of the BusinessUnitUserEntity objects, keyed by their ID.
        Map<String, BusinessUnitUserEntity> buuMap = entitlements.stream()
            .map(UserEntitlementEntity::getBusinessUnitUser)
            .distinct()
            .collect(Collectors.toMap(BusinessUnitUserEntity::getBusinessUnitUserId, Function.identity()));

        // 6. Build the list of BusinessUnitUserDto objects.
        List<BusinessUnitUserDto> buuDtos = buuMap.values().stream()
            .map(buu -> {
                List<UserEntitlementEntity> buuEntitlements = entitlementsByBuuId.get(buu.getBusinessUnitUserId());
                return userStateMapper.toBusinessUnitUserDto(buu, buuEntitlements);
            })
            .toList();

        // 7. Pass the user entity and the BUU list to the mapper.
        return userStateMapper.toUserStateDto(userEntity, buuDtos);
    }

    public UserStateDto getUserState(String username) {

        UserEntity userEntity = userRepository.findOptionalByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        return this.getUserState(userEntity.getUserId());

    }

    @Transactional
    public UserDto createUser(String authHeaderValue) {
        log.debug(":createUser:");

        JWTClaimsSet claimSet = tokenService.extractClaims(authHeaderValue);

        UserEntity userEntity = userRepository
            .saveAndFlush(UserEntity.builder()
                              .username(claimSet.getClaim(PREFERRED_USERNAME_CLAIM).toString())
                              .status(UserStatus.CREATED)
                              .tokenSubject(claimSet.getSubject())
                              .tokenName(claimSet.getClaim(NAME_CLAIM).toString())
                              .version(0L)
                              .build());

        log.debug(":createUser: name: {}, new id: {}", userEntity.getTokenName(), userEntity.getUserId());
        return userMapper.toUserDto(userEntity);
    }

    public String extractClaimAsString(Authentication authentication, String claimName) {
        log.debug(":extractClaimAsString: claim name: {}", claimName);
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String claimValue = jwt.getClaimAsString(claimName);
            if (claimValue != null) {
                return claimValue;
            } else {
                log.debug(":extractClaimAsString: claim not found: {}", claimName);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claim not found: " + claimName);
            }
        } else {
            log.warn(":extractClaimAsString: Authentication not of type Jwt: " + authentication.getClass().getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication Token not of type Jwt.");
        }
    }
}
