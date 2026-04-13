package uk.gov.hmcts.reform.opal.service;

import static uk.gov.hmcts.opal.common.logging.LogUtil.getRequestTimestamp;
import static uk.gov.hmcts.reform.opal.util.VersionUtils.verifyIfMatch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.dto.Versioned;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsService")
public class UserPermissionsService implements UserPermissionsProxy {

    //The name claim of the authorised user.
    private static final String NAME_CLAIM = "name";

    //The claim used to map the authorised user to the user entity.
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";

    private final BusinessUnitUserRepository businessUnitUserRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final UserRepository userRepository;
    private final UserStateMapper userStateMapper;
    private final UserMapper userMapper;
    private final AccessTokenService tokenService;
    private final SecurityEventLoggingService securityEventLoggingService;
    private final Clock clock;


    @Transactional(readOnly = true)
    @Deprecated //Use getUserStateV2 equivalent method
    public UserStateDto getUserState(Authentication authentication, UserPermissionsProxy proxy, Boolean newLogin) {
        Jwt jwt = getJwtToken(authentication);
        String subject = extractSubject(jwt);
        UserEntity user = proxy.getUser(subject);

        String username = extractClaim(jwt, PREFERRED_USERNAME_CLAIM);
        compare(username, user.getUsername(), user.getUserId(), "Preferred Username mismatch:", user);
        String name = extractClaim(jwt, NAME_CLAIM);
        compare(name, user.getTokenName(), user.getUserId(), "Name mismatch:", user);

        log.debug(":getUserState: found User: {}", username);
        UserStateDto dto = proxy.buildUserState(user);
        if (Optional.ofNullable(newLogin).orElse(false)) {
            logUserAuthenticationEvent(dto.getUserId());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    @Deprecated //Use getUserStateV2 equivalent method
    public UserStateDto getUserState(Long userId, Authentication authentication, UserPermissionsProxy proxy,
                                     Boolean newLogin) {
        log.debug(":getUserState: userId: {}", userId);
        if (userId == 0) {
            return proxy.getUserState(authentication, proxy, newLogin);
        } else {
            UserStateDto dto = proxy.buildUserState(proxy.getUser(userId));
            if (Optional.ofNullable(newLogin).orElse(false)) {
                Long clientUserId = proxy.getUserId(authentication, proxy);
                logUserAuthenticationEvent(clientUserId);
            }
            return dto;
        }
    }

    @Transactional(readOnly = true)
    public UserStateV2Dto getUserStateV2(UserPermissionsProxy proxy, Boolean newLogin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = getJwtToken(authentication);
        String subject = extractSubject(jwt);
        UserEntity user = proxy.getUserV2(subject);

        String username = extractClaim(jwt, PREFERRED_USERNAME_CLAIM);
        compare(username, user.getUsername(), user.getUserId(), "Preferred Username mismatch:", user);
        String name = extractClaim(jwt, NAME_CLAIM);
        compare(name, user.getTokenName(), user.getUserId(), "Name mismatch:", user);

        log.debug(":getUserState: found User: {}", username);
        UserStateV2Dto dto = userStateMapper.toUserStateV2Dto(user);
        if (Optional.ofNullable(newLogin).orElse(false)) {
            logUserAuthenticationEvent(user.getUserId());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public UserStateV2Dto getUserStateV2(Long userId, UserPermissionsProxy proxy, Boolean newLogin) {
        log.debug(":getUserState: userId: {}", userId);
        if (userId == 0) {
            return proxy.getUserStateV2(proxy, newLogin);
        } else {
            UserEntity user = proxy.getUserV2(userId);
            UserStateV2Dto dto = userStateMapper.toUserStateV2Dto(user);
            if (Optional.ofNullable(newLogin).orElse(false)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                Long clientUserId = proxy.getUserId(authentication, proxy);
                logUserAuthenticationEvent(clientUserId);
            }
            return dto;
        }
    }

    @Transactional(readOnly = true)
    public Long getUserId(Authentication authentication, UserPermissionsProxy proxy) {
        Jwt jwt = getJwtToken(authentication);
        String subject = extractSubject(jwt);
        UserEntity user = proxy.getUser(subject);
        return user.getUserId();
    }

    private void logUserAuthenticationEvent(Long userId) {
        Map<String, Object> data = Map.of("UserIdentifier", userId);
        securityEventLoggingService.logEvent("User Authentication", "Success",
                                             null, "Authentication", getRequestTimestamp(), data);
    }

    @Transactional(readOnly = true)
    public UserStateDto buildUserState(UserEntity user) {

        // 1. Get all entitlements for the user.
        Set<UserEntitlementEntity> entitlements = userEntitlementRepository
            .findAllByUserIdWithFullJoins(user.getUserId());

        // 2. If the set is empty, check if the user actually exists.
        if (entitlements.isEmpty()) {
            List<BusinessUnitUserDto> businessUnitUsers = businessUnitUserRepository
                .findAllByUser_UserId(user.getUserId())
                .stream()
                .map(businessUnitUser -> userStateMapper.toBusinessUnitUserDto(
                    businessUnitUser,
                    Collections.emptyList()
                ))
                .toList();

            return userStateMapper.toUserStateDto(user, businessUnitUsers);
        }

        // 3. Group entitlements by the BusinessUnitUser's ID (a String)
        Map<String, List<UserEntitlementEntity>> entitlementsByBuuId = entitlements.stream()
            .collect(Collectors.groupingBy(UserEntitlementEntity::getBusinessUnitUserId));

        // 4. Create a map of the BusinessUnitUserEntity objects, keyed by their ID.
        Map<String, BusinessUnitUserEntity> buuMap = entitlements.stream()
            .map(UserEntitlementEntity::getBusinessUnitUser)
            .distinct()
            .collect(Collectors.toMap(BusinessUnitUserEntity::getBusinessUnitUserId, Function.identity()));

        // 5. Build the list of BusinessUnitUserDto objects.
        List<BusinessUnitUserDto> buuDtos = buuMap.values().stream()
            .map(buu -> {
                List<UserEntitlementEntity> buuEntitlements = entitlementsByBuuId.get(buu.getBusinessUnitUserId());
                return userStateMapper.toBusinessUnitUserDto(buu, buuEntitlements);
            })
            .toList();

        // 6. Pass the user entity and the BUU list to the mapper.
        return userStateMapper.toUserStateDto(user, buuDtos);
    }

    private void compare(String fromToken, String fromDb, Long userId, String reason, Versioned versioned) {
        if (!fromToken.equals(fromDb)) {
            throw new ResourceConflictException("User", userId,
                                                reason + " token: " + fromToken + ", db: " + fromDb, versioned);
        }
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(String subject) {
        return userRepository.findByTokenSubject(subject)
            .orElseThrow(() -> new EntityNotFoundException("User not found with subject: " + subject));
    }

    @Transactional(readOnly = true)
    public UserEntity getUserV2(Long userId) {
        return userRepository.findIdWithPermissions(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public UserEntity getUserV2(String subject) {
        return userRepository.findByTokenSubjectWithPermissions(subject)
            .orElseThrow(() -> new EntityNotFoundException("User not found with subject: " + subject));
    }

    @Transactional
    public UserDto addUser(String authHeaderValue) {
        log.debug(":createUser:");

        JWTClaimsSet claimSet = tokenService.extractClaims(authHeaderValue);

        UserEntity userEntity = userRepository
            .saveAndFlush(UserEntity.builder()
                              .username(claimSet.getClaim(PREFERRED_USERNAME_CLAIM).toString())
                              .createdDate(LocalDateTime.now(clock))
                              .tokenSubject(claimSet.getSubject())
                              .tokenName(claimSet.getClaim(NAME_CLAIM).toString())
                              .versionNumber(0L)
                              .build());

        log.debug(":createUser: name: {}, new id: {}", userEntity.getTokenName(), userEntity.getUserId());
        return userMapper.toUserDto(userEntity);
    }

    @Transactional
    public UserDto updateUser(Long userId, String authHeaderValue, UserPermissionsProxy proxy, String ifMatch) {
        log.debug(":updatedUser: userId: {}", userId);

        if (userId == 0) {
            return proxy.updateUser(authHeaderValue, proxy, ifMatch);
        } else {

            JWTClaimsSet claimSet = tokenService.extractClaims(authHeaderValue);

            UserEntity existingUser = proxy.getUser(userId);
            verifyIfMatch(existingUser, ifMatch, userId, "updateUser");

            existingUser.setUsername(claimSet.getClaim(PREFERRED_USERNAME_CLAIM).toString());
            existingUser.setTokenSubject(claimSet.getSubject()); // TODO Tech Debt - subject should be unique in DB.
            existingUser.setTokenName(claimSet.getClaim(NAME_CLAIM).toString());

            UserEntity updatedUser = userRepository.saveAndFlush(existingUser);

            log.debug(":updatedUser: name: {}, user id: {}", updatedUser.getTokenName(), updatedUser.getUserId());
            return userMapper.toUserDto(updatedUser);
        }
    }

    @Transactional
    public UserDto updateUser(String authHeaderValue, UserPermissionsProxy proxy, String ifMatch) {
        log.debug(":updatedUser:");

        JWTClaimsSet claimSet = tokenService.extractClaims(authHeaderValue);
        String subject = claimSet.getSubject();

        UserEntity existingUser = proxy.getUser(subject);
        verifyIfMatch(existingUser, ifMatch, existingUser.getUserId(), "updateUser");

        existingUser.setUsername(claimSet.getClaim(PREFERRED_USERNAME_CLAIM).toString());
        existingUser.setTokenName(claimSet.getClaim(NAME_CLAIM).toString());

        UserEntity updatedUser = userRepository.saveAndFlush(existingUser);

        log.debug(":updatedUser: name: {}, user id: {}", updatedUser.getTokenName(), updatedUser.getUserId());
        return userMapper.toUserDto(updatedUser);
    }

    public Jwt getJwtToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        } else {
            log.warn(":getJwtToken: Authentication not of type Jwt: " + authentication.getClass().getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication Token not of type Jwt.");
        }
    }

    public String extractClaim(final Jwt jwt, String claimName) {
        String claimValue = jwt.getClaimAsString(claimName);
        if (claimValue != null) {
            return claimValue;
        } else {
            log.debug(":ClaimAction.extract: claim not found: {}", claimName);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claim not found: " + claimName);
        }
    }

    public String extractSubject(final Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject != null) {
            return subject;
        } else {
            log.debug(":SubjectAction.extract: subject not found.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Subject not found.");
        }
    }
}
