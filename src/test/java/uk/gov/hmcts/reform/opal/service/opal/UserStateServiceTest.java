package uk.gov.hmcts.reform.opal.service.opal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStateServiceTest {

    static final String BEARER_TOKEN = "Bearer a_token_here";


    @Mock
    private UserService userService;
    @Mock
    private UserStateMapper userStateMapper;
    @Mock
    private Clock clock;

    @InjectMocks
    private UserStateService userStateService;

    @Test
    void testGetUserState_fromUserService() {

        // Arrange
        Optional<UserStateV2> userState = Optional.of(UserStateV2.builder()
            .userId(123L).username("John Smith").build());
        UserEntity userEntity = UserEntity.builder().userId(123L).username("John Smith").build();

        when(userService.getAuthenticatedUser()).thenReturn(userEntity);
        when(userStateMapper.toUserStateV2(userEntity, clock)).thenReturn(userState.get());

        // Act
        UserStateV2 result = userStateService.getUserStateUsingAuthToken();

        // Assert
        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        assertEquals("John Smith", result.getUsername());
    }
}
