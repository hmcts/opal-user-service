package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                        "R1", Set.of("BU1", "BU2"),
                        "R2", Set.of("BU1")
                    )
                ),
                new ParsedUserMapping(
                    "user2@test.com",
                    Map.of("R4", Set.of("BU4"))
                )
            ),
            Set.of("baduser@test.com")
        );

        when(parser.parse(any(Reader.class))).thenReturn(mappingResult);

        // ACT
        refreshService.refreshMappings();

        // ASSERT
        verify(cacheService).putUserMapping(
            "user1@test.com",
            Map.of(
                "R1", Set.of("BU1", "BU2"),
                "R2", Set.of("BU1")
            )
        );

        verify(cacheService).putUserMapping(
            "user2@test.com",
            Map.of("R4", Set.of("BU4"))
        );

        verify(cacheService).deleteUserMapping("baduser@test.com");

        verify(cacheService).deleteStaleUserMappings(Set.of("user1@test.com", "user2@test.com"));

        verify(cacheService).setLastUpdateAt(LAST_UPDATE_AT);
    }
}
