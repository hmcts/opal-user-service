package uk.gov.hmcts.reform.opal.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.dto.BusinessUnitUserDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
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
public class UserPermissionsService {

    private final UserEntitlementRepository userEntitlementRepository;
    private final UserRepository userRepository;
    private final UserStateMapper userStateMapper;

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
}
