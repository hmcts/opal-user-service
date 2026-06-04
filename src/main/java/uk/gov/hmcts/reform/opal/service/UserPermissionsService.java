package uk.gov.hmcts.reform.opal.service;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.common.dto.Versioned;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.reform.opal.config.properties.AppModeConfiguration;
import uk.gov.hmcts.reform.opal.config.properties.CacheConfiguration;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.opal.UserService;
import uk.gov.hmcts.reform.opal.service.synchronise.SynchronisePermissionsService;
import uk.gov.hmcts.reform.opal.util.JwtUtil;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static uk.gov.hmcts.opal.common.dto.ToJsonString.objectToPrettyJson;
import static uk.gov.hmcts.opal.common.logging.LogUtil.getRequestTimestamp;
import static uk.gov.hmcts.reform.opal.util.VersionUtils.verifyIfMatch;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsService")
public class UserPermissionsService {

    //The name claim of the authorised user.
    private static final String NAME_CLAIM = "name";

    //The claim used to map the authorised user to the user entity.
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private final UserRepository userRepository;
    private final UserStateMapper userStateMapper;
    private final UserMapper userMapper;
    private final AccessTokenService tokenService;
    private final SecurityEventLoggingService securityEventLoggingService;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final CacheConfiguration cacheConfiguration;
    private final SynchronisePermissionsService synchronisePermissionsService;
    private final AppModeConfiguration appModeConfiguration;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Long getAuthenticatedUserId() {
        return SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().getUserId();
    }

    @Transactional
    public UserStateV2Dto getUserStateV2(boolean newLogin) {
        log.debug(":getUserState");

        UserEntity user = getUserFromAuthentication();
        Long userId = user.getUserId();

        if (appModeConfiguration.getAppMode().equalsIgnoreCase("legacy")) {
            synchronisePermissionsService.synchronise(user);
            // synchronise() was processed in a different transaction, so we need to refresh user entity
            userService.refreshUser(user);
        }

        // NB. When legacy refresh gets deleted we will need to update the first fetch to include all roles
        // and remove this second fetch
        user = userRepository.findIdWithPermissions(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (newLogin) {
            logUserAuthenticationEvent(userId);
            updateLastLogin(user);
        }
        UserStateV2Dto dto = userStateMapper.toUserStateV2Dto(user, clock);
        cacheUserState(dto, user);
        return dto;
    }

    private UserEntity getUserFromAuthentication() {
        Jwt jwt = SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().getToken();
        UserEntity user = userService.getAuthenticatedUser();
        validateAuthenticatedUser(user, jwt);
        return user;
    }

    private void validateAuthenticatedUser(UserEntity user, Jwt jwt) {
        String username = JwtUtil.extractClaim(jwt, PREFERRED_USERNAME_CLAIM);
        compare(username, user.getUsername(), user.getUserId(), "Preferred Username mismatch:", user);
        String name = JwtUtil.extractClaim(jwt, NAME_CLAIM);
        compare(name, user.getTokenName(), user.getUserId(), "Name mismatch:", user);
    }


    private void compare(String fromToken, String fromDb, Long userId, String reason, Versioned versioned) {
        if (!fromToken.equals(fromDb)) {
            throw new ResourceConflictException("User", userId,
                reason + " token: " + fromToken + ", db: " + fromDb, versioned);
        }
    }

    private void cacheUserState(UserStateV2Dto dto, UserEntity user) {
        String cacheKey = "USER_STATE_" + user.getTokenSubject();
        dto.setCacheName(cacheKey);
        String dtoJson = objectToPrettyJson(dto);
        Long cacheTimeout = cacheConfiguration.getUserStateTimeoutMinutes();
        try {
            redisTemplate.opsForValue().set(cacheKey, dtoJson, cacheTimeout, TimeUnit.MINUTES);
        } catch (DataAccessException e) {
            log.warn("Unable to cache user state for user {}, reason: {}", user.getUsername(), e.getMessage());
        }
    }

    private void logUserAuthenticationEvent(Long userId) {
        Map<String, Object> data = Map.of("UserIdentifier", userId);
        securityEventLoggingService.logEvent("User Authentication", "Success",
            null, "Authentication", getRequestTimestamp(), data);
    }

    private void updateLastLogin(UserEntity user) {
        user.setLastLoginDate(LocalDateTime.now(clock));
        userRepository.saveAndFlush(user);
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
        return userMapper.toUserDto(userEntity, clock);
    }

    @Transactional
    public UserDto updateUser(Long userId, String authHeaderValue, String ifMatch) {
        log.debug(":updatedUser: userId: {}", userId);

        if (userId == 0) {
            return updateUser(authHeaderValue, ifMatch);
        } else {

            JWTClaimsSet claimSet = tokenService.extractClaims(authHeaderValue);

            UserEntity existingUser = getUser(userId);
            verifyIfMatch(existingUser, ifMatch, userId, "updateUser");

            existingUser.setUsername(claimSet.getClaim(PREFERRED_USERNAME_CLAIM).toString());
            existingUser.setTokenSubject(claimSet.getSubject()); // TODO Tech Debt - subject should be unique in DB.
            existingUser.setTokenName(claimSet.getClaim(NAME_CLAIM).toString());

            UserEntity updatedUser = userRepository.saveAndFlush(existingUser);

            log.debug(":updatedUser: name: {}, user id: {}", updatedUser.getTokenName(), updatedUser.getUserId());
            return userMapper.toUserDto(updatedUser, clock);
        }
    }

    @Transactional
    public UserDto updateUser(String authHeaderValue, String ifMatch) {
        log.debug(":updatedUser:");

        JWTClaimsSet claimSet = tokenService.extractClaims(authHeaderValue);
        String subject = claimSet.getSubject();

        UserEntity existingUser = getUser(subject);
        verifyIfMatch(existingUser, ifMatch, existingUser.getUserId(), "updateUser");

        existingUser.setUsername(claimSet.getClaim(PREFERRED_USERNAME_CLAIM).toString());
        existingUser.setTokenName(claimSet.getClaim(NAME_CLAIM).toString());

        UserEntity updatedUser = userRepository.saveAndFlush(existingUser);

        log.debug(":updatedUser: name: {}, user id: {}", updatedUser.getTokenName(), updatedUser.getUserId());
        return userMapper.toUserDto(updatedUser, clock);
    }


}
