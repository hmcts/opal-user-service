package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleMappingRefreshService {

    private final MappingFileClient mappingFileClient;
    private final UserRoleMappingParser parser;
    private final UserRoleMappingCacheService cacheService;

    public void refreshMappings() throws IOException {

        MappingFileSnapshot snapshot = mappingFileClient.readSnapshot();
        String lastModifiedAt = snapshot.lastModifiedAt();

        // --- Skip if unchanged ---
        if (cacheService.hasLastUpdateAt()) {
            String cachedLastUpdateAt = cacheService.getLastUpdateAt();

            if (lastModifiedAt != null && lastModifiedAt.equals(cachedLastUpdateAt)) {
                cacheService.refreshAllTtls();

                log.info(
                    "Role Mapping file cache refresh skipped - file unchanged. Last Updated at {}",
                    lastModifiedAt
                );
                return;
            }
        }

        try (Reader reader = new InputStreamReader(snapshot.content(), StandardCharsets.UTF_8)) {

            MappingFileProcessingResult mappingResult = parser.parse(reader);

            Set<String> refreshedSubjects = new LinkedHashSet<>();
            int failureCount = 0;

            // --- Process valid users ---
            for (ParsedUserMapping userMapping : mappingResult.validUsers()) {
                try {
                    String email = userMapping.emailAddress();
                    cacheService.putUserMapping(email, userMapping.roleToBusinessUnits());
                    refreshedSubjects.add(email);

                } catch (Exception e) {
                    log.error(
                        "Failed to cache mapping for user {}, skipping",
                        userMapping.emailAddress(),
                        e
                    );
                    failureCount++;
                }
            }

            // --- Handle invalid users ---
            for (String invalidEmail : mappingResult.invalidEmails()) {
                cacheService.deleteUserMapping(invalidEmail);
                log.warn(
                    "Invalid CSV structure for email {}, cache entry removed",
                    invalidEmail
                );
            }

            // --- Cleanup ---
            cacheService.deleteStaleUserMappings(refreshedSubjects);
            cacheService.setLastUpdateAt(lastModifiedAt);

            log.info(
                "Role Mapping cache refresh completed: {} users refreshed, {} failures. Last Updated at {}",
                refreshedSubjects.size(),
                failureCount,
                lastModifiedAt
            );
        }
    }
}
