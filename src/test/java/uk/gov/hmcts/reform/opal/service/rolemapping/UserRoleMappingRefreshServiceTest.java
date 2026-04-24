package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserRoleMappingRefreshServiceTest {

    private static final String LAST_UPDATE_AT = "2025-01-02T03:04:05.678";
    private static final String PREVIOUS_LAST_UPDATE_AT = "2025-01-01T00:00:00.000";

    @Mock
    private MappingFileClient mappingFileClient;

    @Mock
    private UserRoleMappingParser parser;

    @Mock
    private UserRoleMappingCacheService cacheService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRoleMappingRefreshService refreshService;

    @Test
    void skipsRefreshWhenFileHasNotChangedAndRefreshesTtls() throws Exception {

        // ARRANGE
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot(
                LAST_UPDATE_AT,
                new java.io.ByteArrayInputStream(new byte[0])
            ));

        when(cacheService.hasLastUpdateAt()).thenReturn(true);
        when(cacheService.getLastUpdateAt()).thenReturn(LAST_UPDATE_AT);

        // ACT
        refreshService.refreshMappings();

        // ASSERT
        verify(cacheService).refreshAllTtls();
        verify(parser, never()).parse(any(Reader.class));
        verify(userRepository, never()).findByUsernameIgnoreCase(any());
        verify(cacheService, never()).putUserMapping(any(), any());
        verify(cacheService, never()).deleteUserMapping(any());
        verify(cacheService, never()).setLastUpdateAt(any());
        verify(cacheService, never()).deleteStaleUserMappings(any());
    }

    @Test
    void refreshesCacheAndUpdatesLastModifiedWhenFileHasChanged() throws Exception {

        // ARRANGE
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot(
                LAST_UPDATE_AT,
                new java.io.ByteArrayInputStream(new byte[0])
            ));

        when(cacheService.hasLastUpdateAt()).thenReturn(true);
        when(cacheService.getLastUpdateAt()).thenReturn(PREVIOUS_LAST_UPDATE_AT);

        MappingFileProcessingResult mappingResult = new MappingFileProcessingResult(
            List.of(
                new ParsedUserMapping(
                    "user1@test.com",
                    Map.of(
                        "BU1", Set.of("R1", "R2"),
                        "BU2", Set.of("R3")
                    )
                ),
                new ParsedUserMapping(
                    "user2@test.com",
                    Map.of("BU4", Set.of("R4"))
                )
            ),
            Set.of("baduser@test.com")
        );

        when(parser.parse(any(Reader.class))).thenReturn(mappingResult);

        when(userRepository.findByUsernameIgnoreCase("user1@test.com"))
            .thenReturn(Optional.of(user("user1@test.com", "AS1")));

        when(userRepository.findByUsernameIgnoreCase("user2@test.com"))
            .thenReturn(Optional.of(user("user2@test.com", "AS2")));

        when(userRepository.findByUsernameIgnoreCase("baduser@test.com"))
            .thenReturn(Optional.of(user("baduser@test.com", "AS9")));

        // ACT
        refreshService.refreshMappings();

        // ASSERT
        verify(cacheService).putUserMapping(
            "AS1",
            Map.of(
                "BU1", Set.of("R1", "R2"),
                "BU2", Set.of("R3")
            )
        );

        verify(cacheService).putUserMapping(
            "AS2",
            Map.of("BU4", Set.of("R4"))
        );

        verify(cacheService).deleteUserMapping("AS9");

        verify(cacheService).deleteStaleUserMappings(Set.of("AS1", "AS2"));

        verify(cacheService).setLastUpdateAt(LAST_UPDATE_AT);
    }

    @Test
    void refreshesCacheAndSkipsUsersMissingInDb() throws Exception {

        // ARRANGE
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot(
                LAST_UPDATE_AT,
                new java.io.ByteArrayInputStream(new byte[0])
            ));

        when(cacheService.hasLastUpdateAt()).thenReturn(false);

        MappingFileProcessingResult mappingResult = new MappingFileProcessingResult(
            List.of(
                new ParsedUserMapping(
                    "user1@test.com",
                    Map.of("BU1", Set.of("R1"))
                ),
                new ParsedUserMapping(
                    "missing@test.com",
                    Map.of("BU2", Set.of("R2"))
                )
            ),
            Set.of()
        );

        when(parser.parse(any(Reader.class))).thenReturn(mappingResult);

        when(userRepository.findByUsernameIgnoreCase("user1@test.com"))
            .thenReturn(Optional.of(user("user1@test.com", "AS1")));

        when(userRepository.findByUsernameIgnoreCase("missing@test.com"))
            .thenReturn(Optional.empty());

        // ACT
        refreshService.refreshMappings();

        // ASSERT
        verify(cacheService).putUserMapping(
            "AS1",
            Map.of("BU1", Set.of("R1"))
        );

        verify(cacheService, never()).putUserMapping(
            eq("missing@test.com"),
            any()
        );

        verify(cacheService).deleteStaleUserMappings(Set.of("AS1"));

        verify(cacheService).setLastUpdateAt(LAST_UPDATE_AT);
    }

    @Test
    void skipsUserWhenTokenSubjectIsNullOrBlank() throws Exception {

        // ARRANGE
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot(
                LAST_UPDATE_AT,
                new java.io.ByteArrayInputStream(new byte[0])
            ));

        when(cacheService.hasLastUpdateAt()).thenReturn(false);

        MappingFileProcessingResult mappingResult = new MappingFileProcessingResult(
            List.of(
                new ParsedUserMapping(
                    "user1@test.com",
                    Map.of("BU1", Set.of("R1"))
                )
            ),
            Set.of()
        );

        when(parser.parse(any(Reader.class))).thenReturn(mappingResult);

        // User exists but has null tokenSubject
        when(userRepository.findByUsernameIgnoreCase("user1@test.com"))
            .thenReturn(Optional.of(user("user1@test.com", null)));

        // ACT
        refreshService.refreshMappings();

        // ASSERT
        verify(cacheService, never()).putUserMapping(any(), any());
        verify(cacheService).deleteStaleUserMappings(Set.of());
        verify(cacheService).setLastUpdateAt(LAST_UPDATE_AT);
    }

    private UserEntity user(String username, String subject) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setTokenSubject(subject);
        return user;
    }
}
