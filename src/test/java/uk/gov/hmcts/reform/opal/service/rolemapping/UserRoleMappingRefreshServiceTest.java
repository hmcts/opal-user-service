package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.time.Duration;
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

    @Mock
    private MappingFileClient mappingFileClient;

    @Mock
    private UserRoleMappingParser parser;

    @Mock
    private UserRoleMappingCacheService cacheService;

    @Mock
    private RoleMappingCacheProperties properties;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRoleMappingRefreshService refreshService;

    @Test
    void skipsRefreshWhenFileHasNotChangedAndRefreshesTtls() throws Exception {
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot("2025-01-02T03:04:05.678",
                                                new java.io.ByteArrayInputStream(new byte[0])));
        when(cacheService.hasLastUpdateAt()).thenReturn(true);
        when(cacheService.getLastUpdateAt()).thenReturn("2025-01-02T03:04:05.678");
        when(properties.getUserTtl()).thenReturn(Duration.ofHours(24));
        when(properties.getLastUpdateTtl()).thenReturn(Duration.ofHours(1));

        refreshService.refreshMappings();

        verify(cacheService).refreshAllTtls(Duration.ofHours(24), Duration.ofHours(1));
        verify(parser, never()).parse(any(Reader.class));
        verify(userRepository, never()).findByUsernameIgnoreCase(any());
        verify(cacheService, never()).putUserMapping(any(), any(), any());
        verify(cacheService, never()).deleteUserKey(any());
        verify(cacheService, never()).setLastUpdateAt(any(), any());
        verify(cacheService, never()).deleteStaleUserKeys(any());
    }

    @Test
    void refreshesCacheAndUpdatesLastModifiedWhenFileHasChanged() throws Exception {
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot("2025-01-02T03:04:05.678",
                                                new java.io.ByteArrayInputStream(new byte[0])));
        when(cacheService.hasLastUpdateAt()).thenReturn(true);
        when(cacheService.getLastUpdateAt()).thenReturn("2025-01-01T00:00:00.000");
        when(properties.getUserTtl()).thenReturn(Duration.ofHours(24));
        when(properties.getLastUpdateTtl()).thenReturn(Duration.ofHours(1));

        ParseResult parseResult = new ParseResult(
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

        when(parser.parse(any(Reader.class))).thenReturn(parseResult);

        when(userRepository.findByUsernameIgnoreCase("user1@test.com"))
            .thenReturn(Optional.of(user("user1@test.com", "AS1")));
        when(userRepository.findByUsernameIgnoreCase("user2@test.com"))
            .thenReturn(Optional.of(user("user2@test.com", "AS2")));
        when(userRepository.findByUsernameIgnoreCase("baduser@test.com"))
            .thenReturn(Optional.of(user("baduser@test.com", "AS9")));

        refreshService.refreshMappings();

        verify(cacheService, times(1)).putUserMapping(
            "ROLE_MAPPING_USER_AS1",
            Map.of(
                "BU1", Set.of("R1", "R2"),
                "BU2", Set.of("R3")
            ),
            Duration.ofHours(24)
        );

        verify(cacheService, times(1)).putUserMapping(
            "ROLE_MAPPING_USER_AS2",
            Map.of("BU4", Set.of("R4")),
            Duration.ofHours(24)
        );

        verify(cacheService, times(1)).deleteUserKey("ROLE_MAPPING_USER_AS9");
        verify(cacheService).deleteStaleUserKeys(Set.of("ROLE_MAPPING_USER_AS1", "ROLE_MAPPING_USER_AS2"));
        verify(cacheService).setLastUpdateAt("2025-01-02T03:04:05.678", Duration.ofHours(1));
    }

    @Test
    void refreshesCacheAndSkipsUsersMissingInDb() throws Exception {
        when(mappingFileClient.readSnapshot())
            .thenReturn(new MappingFileSnapshot("2025-01-02T03:04:05.678",
                                                new java.io.ByteArrayInputStream(new byte[0])));
        when(cacheService.hasLastUpdateAt()).thenReturn(false);
        when(properties.getUserTtl()).thenReturn(Duration.ofHours(24));
        when(properties.getLastUpdateTtl()).thenReturn(Duration.ofHours(1));

        ParseResult parseResult = new ParseResult(
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

        when(parser.parse(any(Reader.class))).thenReturn(parseResult);

        when(userRepository.findByUsernameIgnoreCase("user1@test.com"))
            .thenReturn(Optional.of(user("user1@test.com", "AS1")));
        when(userRepository.findByUsernameIgnoreCase("missing@test.com"))
            .thenReturn(Optional.empty());

        refreshService.refreshMappings();

        verify(cacheService, times(1)).putUserMapping(
            "ROLE_MAPPING_USER_AS1",
            Map.of("BU1", Set.of("R1")),
            Duration.ofHours(24)
        );

        verify(cacheService, never()).putUserMapping(
            "ROLE_MAPPING_USER_missing@test.com",
            Map.of("BU2", Set.of("R2")),
            Duration.ofHours(24)
        );

        verify(cacheService).deleteStaleUserKeys(Set.of("ROLE_MAPPING_USER_AS1"));
        verify(cacheService).setLastUpdateAt("2025-01-02T03:04:05.678", Duration.ofHours(1));
    }

    private UserEntity user(String preferredUsername, String tokenSubject) {
        UserEntity user = new UserEntity();
        user.setUsername(preferredUsername);
        user.setTokenSubject(tokenSubject);
        return user;
    }
}
