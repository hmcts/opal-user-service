package uk.gov.hmcts.reform.opal.service.opal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserStateService")
public class UserStateService {
    private final UserService userService;
    private final Clock clock;
    private final UserStateMapper userStateMapper;

    public UserStateV2 getUserStateUsingAuthToken() {
        UserEntity userEntity = userService.getAuthenticatedUser();
        return userStateMapper.toUserStateV2(userEntity, clock);
    }
}
