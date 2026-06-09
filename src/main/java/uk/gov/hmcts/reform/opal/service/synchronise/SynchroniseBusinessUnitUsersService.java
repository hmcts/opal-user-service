package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Implements Step 3 of https://tools.hmcts.net/jira/browse/PO-2831

@Service
@AllArgsConstructor
@Slf4j(topic = "opal.SynchroniseBusinessUnitUsersService")
public class SynchroniseBusinessUnitUsersService {

    private static final int BUSINESS_UNIT_USER_ID_MAX_LENGTH = 6;
    private static final String SYNC_STAGE = "synchronise business unit users";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    private final UserService userService;
    private final BusinessUnitUserRepository businessUnitUserRepository;
    private final BusinessUnitRepository businessUnitRepository;

    @Transactional
    public void synchroniseBusinessUnitsUsers(UserEntity user, List<LegacyBusinessUnitUserId> legacyBusinessUnitUsers) {
        try {
            if (legacyBusinessUnitUsers == null) {
                log.error("Legacy business unit user payload is null for user {}", user.getUserId());
                throw new SynchronisePermissionsException(user, SYNC_STAGE,
                    "legacy business unit user payload is missing");
            }

            Set<String> legacyBusinessUnitUserIds = new LinkedHashSet<>();

            for (LegacyBusinessUnitUserId legacyBusinessUnitUser : legacyBusinessUnitUsers) {
                if (legacyBusinessUnitUser == null) {
                    log.error("Legacy business unit user payload contains null entry for user {}",
                        user.getUserId());
                    throw new SynchronisePermissionsException(user, SYNC_STAGE,
                        "legacy business unit user entry is missing");
                }

                String businessUnitUserId = parseBusinessUnitUserId(user,
                    legacyBusinessUnitUser.getBusinessUnitUserId());
                Short businessUnitId = parseBusinessUnitId(user, legacyBusinessUnitUser.getBusinessUnitId());
                processBusinessUnitUser(user, businessUnitUserId, businessUnitId);
                legacyBusinessUnitUserIds.add(businessUnitUserId);
            }

            removeStaleBusinessUnitUsers(user, legacyBusinessUnitUserIds);
        } catch (RuntimeException exception) {
            if (exception instanceof SynchronisePermissionsException synchronisePermissionsException) {
                throw synchronisePermissionsException;
            }
            throw new SynchronisePermissionsException(user, SYNC_STAGE, UNEXPECTED_RUNTIME_EXCEPTION_REASON, exception);
        }
    }

    @Transactional
    public void removeBusinessUnitUsersWithoutValidatedRoleMappings(UserEntity user,
                                                                    Set<Short> validatedBusinessUnitIds) {
        List<BusinessUnitUserEntity> staleBusinessUnitUsers = businessUnitUserRepository
            .findAllByUser_UserId(user.getUserId()).stream()
            .filter(buUser -> !validatedBusinessUnitIds.contains(buUser.getBusinessUnitId()))
            .toList();

        if (!staleBusinessUnitUsers.isEmpty()) {

            List<String> staleBusinessUnitUserIds = staleBusinessUnitUsers.stream()
                .map(BusinessUnitUserEntity::getBusinessUnitUserId)
                .toList();
            log.debug("Deleting business units not in cache for user:{} BUUs:{}", user.getUserId(),
                 staleBusinessUnitUserIds);

            userService.deleteBusinessUnitUsers(user, staleBusinessUnitUsers);
        }
    }

    private void processBusinessUnitUser(UserEntity user, String businessUnitUserId, Short businessUnitId) {
        Optional<BusinessUnitEntity> maybeBusinessUnitEntity = businessUnitRepository.findById(businessUnitId);

        BusinessUnitEntity businessUnit;
        if (maybeBusinessUnitEntity.isPresent()) {
            businessUnit = maybeBusinessUnitEntity.get();
        } else {
            log.error("legacyBusinessUnitUser not found for businessUnit {}", businessUnitId);
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                "legacy business unit not found: " + businessUnitId);
        }

        Optional<BusinessUnitUserEntity> maybeBuu = businessUnitUserRepository.findById(businessUnitUserId);
        if (maybeBuu.isPresent()) {
            //update existing buu
            BusinessUnitUserEntity buu = maybeBuu.get();
            if (!buu.getBusinessUnitId().equals(businessUnitId)) {
                buu.setBusinessUnit(businessUnit);
            }
            if (!buu.getUser().getUserId().equals(user.getUserId())) {
                buu.setUser(user);
            }
        } else {
            //insert new buu
            BusinessUnitUserEntity buu = BusinessUnitUserEntity.builder()
                .businessUnitUserId(businessUnitUserId)
                .businessUnit(businessUnit)
                .user(user)
                .build();
            businessUnitUserRepository.save(buu);
        }
    }

    private String parseBusinessUnitUserId(UserEntity user, String legacyBusinessUnitUserId) {
        if (legacyBusinessUnitUserId == null
            || legacyBusinessUnitUserId.isBlank()
            || legacyBusinessUnitUserId.length() > BUSINESS_UNIT_USER_ID_MAX_LENGTH) {
            log.error("Invalid businessUnitUserId {}", legacyBusinessUnitUserId);
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                "invalid business unit user id: " + legacyBusinessUnitUserId);
        }
        return legacyBusinessUnitUserId;
    }

    private Short parseBusinessUnitId(UserEntity user, String legacyBusinessUnitId) {
        try {
            return Short.valueOf(legacyBusinessUnitId);
        } catch (NumberFormatException e) {
            log.error("Invalid businessUnitId {}", legacyBusinessUnitId, e);
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                "invalid business unit id: " + legacyBusinessUnitId, e);
        }
    }

    private void removeStaleBusinessUnitUsers(UserEntity user, Set<String> legacyBusinessUnitUserIds) {
        List<BusinessUnitUserEntity> staleBusinessUnitUsers = legacyBusinessUnitUserIds.isEmpty()
            ? businessUnitUserRepository.findAllByUser_UserId(user.getUserId())
            : businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnitUserIdNotIn(
                user.getUserId(),
                legacyBusinessUnitUserIds
            );

        if (!staleBusinessUnitUsers.isEmpty()) {

            List<String> staleBusinessUnitUserIds = staleBusinessUnitUsers.stream()
                .map(BusinessUnitUserEntity::getBusinessUnitUserId)
                .toList();
            log.debug("Deleting business units not in legacy for user:{} BUUs:{}", user.getUserId(),
                     staleBusinessUnitUserIds);

            userService.deleteBusinessUnitUsers(user, staleBusinessUnitUsers);
        }
    }
}
