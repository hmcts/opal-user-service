package uk.gov.hmcts.reform.opal.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceTest {

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStateMapper userStateMapper;

    @Spy
    @InjectMocks
    private UserPermissionsService service;

    private static final long USER_ID = 42L;
    private static final String USERNAME = "opal-user";
    private UserEntity userEntity;
    private UserStateDto userDto;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.setUserId(USER_ID);
        userEntity.setUsername(USERNAME);

        userDto = new UserStateDto();
        userDto.setUserId(USER_ID);
        userDto.setUsername(USERNAME);
    }

    @Test
    @DisplayName("getUserState(Long) throws when no entitlements and user missing")
    void getUserState_longNoEntitlements_throws() {
        when(userEntitlementRepository.findAllByUserIdWithFullJoins(USER_ID))
            .thenReturn(Collections.emptySet());
        when(userRepository.findById(USER_ID))
            .thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(USER_ID)
        );
        assertEquals("User not found with ID: " + USER_ID, ex.getMessage());

        verify(userEntitlementRepository).findAllByUserIdWithFullJoins(USER_ID);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("getUserState(String) delegates to getUserState(Long) after lookup")
    void getUserState_stringDelegatesToLong() {
        when(userRepository.findOptionalByUsername(USERNAME))
            .thenReturn(java.util.Optional.of(userEntity));
        doReturn(userDto).when(service).getUserState(USER_ID);

        UserStateDto result = service.getUserState(USERNAME);

        assertEquals(userDto, result);
        verify(userRepository).findOptionalByUsername(USERNAME);
        verify(service).getUserState(USER_ID);
    }

    @Test
    @DisplayName("getUserState(String) throws when username not found")
    void getUserState_stringNotFound_throws() {
        when(userRepository.findOptionalByUsername(USERNAME))
            .thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(USERNAME)
        );
        assertEquals("User not found with username: " + USERNAME, ex.getMessage());

        verify(userRepository).findOptionalByUsername(USERNAME);
    }
}
