package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;

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

    private final BusinessUnitUserRepository businessUnitUserRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final BusinessUnitUserRoleRepository businessUnitUserRoleRepository;
    private final UserEntitlementRepository userEntitlementRepository;

    @Transactional
    public void refreshBusinessUnitUsers(UserEntity user, List<LegacyBusinessUnitUser> legacyBusinessUnitUsers)
        throws SynchronisePermissionsException {
        Set<String> legacyBusinessUnitUserIds = new LinkedHashSet<>();

        for (LegacyBusinessUnitUser legacyBusinessUnitUser : legacyBusinessUnitUsers) {

            String businessUnitUserId = parseBusinessUnitUserId(legacyBusinessUnitUser.getBusinessUnitUserId());
            legacyBusinessUnitUserIds.add(businessUnitUserId);
            Short businessUnitId = parseBusinessUnitId(legacyBusinessUnitUser.getBusinessUnitId());
            Optional<BusinessUnitEntity> maybeBusinessUnitEntity = businessUnitRepository.findById(businessUnitId);

            BusinessUnitEntity businessUnit;
            if (maybeBusinessUnitEntity.isPresent()) {
                businessUnit = maybeBusinessUnitEntity.get();
            } else {
                log.error("legacyBusinessUnitUser not found for businessUnit {}",
                          legacyBusinessUnitUser.getBusinessUnitId());
                throw new SynchronisePermissionsException("legacyBusinessUnitUser not found for businessUnit");
            }

            Optional<BusinessUnitUserEntity> maybeBuu = businessUnitUserRepository.findById(businessUnitUserId);

            if (maybeBuu.isPresent()) {
                BusinessUnitUserEntity buu = maybeBuu.get();
                if (!buu.getBusinessUnitId().equals(businessUnitId)) {
                    buu.setBusinessUnit(businessUnit);
                }
                if (!buu.getUser().getUserId().equals(user.getUserId())) {
                    buu.setUser(user);
                }
            } else {
                BusinessUnitUserEntity buu = BusinessUnitUserEntity.builder()
                    .businessUnitUserId(businessUnitUserId)
                    .businessUnit(businessUnit)
                    .user(user)
                    .build();
                businessUnitUserRepository.save(buu);
            }
        }

        removeStaleBusinessUnitUsers(user.getUserId(), legacyBusinessUnitUserIds);
    }

    private String parseBusinessUnitUserId(String legacyBusinessUnitUserId) throws SynchronisePermissionsException {
        if (legacyBusinessUnitUserId == null
            || legacyBusinessUnitUserId.isBlank()
            || legacyBusinessUnitUserId.length() > BUSINESS_UNIT_USER_ID_MAX_LENGTH) {
            log.error("Invalid businessUnitUserId {}", legacyBusinessUnitUserId);
            throw new SynchronisePermissionsException("Invalid business unit user id: " + legacyBusinessUnitUserId);
        }
        return legacyBusinessUnitUserId;
    }

    private Short parseBusinessUnitId(String legacyBusinessUnitId) throws SynchronisePermissionsException {
        try {
            return Short.valueOf(legacyBusinessUnitId);
        } catch (NumberFormatException e) {
            log.error("Invalid businessUnitId {}", legacyBusinessUnitId, e);
            throw new SynchronisePermissionsException("Invalid business unit id: " + legacyBusinessUnitId);
        }
    }

    private void removeStaleBusinessUnitUsers(Long userId, Set<String> legacyBusinessUnitUserIds) {
        List<String> staleBusinessUnitUserIds = businessUnitUserRepository.findAllByUser_UserId(userId).stream()
            .map(BusinessUnitUserEntity::getBusinessUnitUserId)
            .filter(existingBusinessUnitUserId -> !legacyBusinessUnitUserIds.contains(existingBusinessUnitUserId))
            .toList();

        if (staleBusinessUnitUserIds.isEmpty()) {
            return;
        }

        userEntitlementRepository.deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(staleBusinessUnitUserIds);
        businessUnitUserRoleRepository.deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(staleBusinessUnitUserIds);
        businessUnitUserRepository.deleteAllById(staleBusinessUnitUserIds);
    }

}
