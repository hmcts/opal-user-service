package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleMappingRefreshService {

    private final MappingFileClient mappingFileClient;
    private final UserRoleMappingParser parser;
    private final UserRoleMappingCacheService cacheService;
    private final UserRepository userRepository;

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

                Optional<UserEntity> userOpt =
                    userRepository.findByUsernameIgnoreCase(userMapping.emailAddress());

                if (userOpt.isEmpty()) {
                    log.error("No user found in DB for email {}, skipping", userMapping.emailAddress());
                    failureCount++;
                    continue;
                }

                String tokenSubject = userOpt.get().getTokenSubject();

                if (tokenSubject == null || tokenSubject.isBlank()) {
                    log.error("User {} has blank token_subject, skipping", userMapping.emailAddress());
                    failureCount++;
                    continue;
                }

                try {
                    cacheService.putUserMapping(tokenSubject, userMapping.businessUnitToRoles());
                    refreshedSubjects.add(tokenSubject);

                } catch (Exception e) {
                    log.error(
                        "Failed to cache mapping for user {} (subject {}), skipping",
                        userMapping.emailAddress(),
                        tokenSubject,
                        e
                    );
                    failureCount++;
                }
            }

            // --- Handle invalid users ---
            for (String invalidEmail : mappingResult.invalidEmails()) {

                userRepository.findByUsernameIgnoreCase(invalidEmail)
                    .ifPresentOrElse(user -> {
                        String tokenSubject = user.getTokenSubject();

                        if (tokenSubject != null && !tokenSubject.isBlank()) {
                            cacheService.deleteUserMapping(tokenSubject);

                            log.warn(
                                "Invalid CSV structure for email {}, cache entry removed",
                                invalidEmail
                            );
                        } else {
                            log.warn(
                                "Invalid CSV structure for email {}, token_subject missing",
                                invalidEmail
                            );
                        }
                    }, () -> log.warn(
                        "Invalid CSV structure for email {}, user not found in DB",
                        invalidEmail
                    ));
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
