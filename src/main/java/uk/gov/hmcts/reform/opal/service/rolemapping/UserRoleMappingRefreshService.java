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
    private final RoleMappingCacheProperties properties;
    private final UserRepository userRepository;

    public void refreshMappings() throws IOException {
        MappingFileSnapshot snapshot = mappingFileClient.readSnapshot();
        String lastModifiedAt = snapshot.lastModifiedAt();

        if (cacheService.hasLastUpdateAt()) {
            String cachedLastUpdateAt = cacheService.getLastUpdateAt();
            if (lastModifiedAt != null && lastModifiedAt.equals(cachedLastUpdateAt)) {
                cacheService.refreshAllTtls(properties.getUserTtl(), properties.getLastUpdateTtl());
                log.info(
                    "Role Mapping file cache refresh, skipped as file has not changed since last update: "
                        + " Last Updated at {}",
                    lastModifiedAt
                );
                return;
            }
        }

        try (Reader reader = new InputStreamReader(snapshot.content(), StandardCharsets.UTF_8)) {
            ParseResult parseResult = parser.parse(reader);

            Set<String> refreshedKeys = new LinkedHashSet<>();
            int refreshedCount = 0;

            for (ParsedUserMapping userMapping : parseResult.validUsers()) {
                Optional<UserEntity> userOpt = userRepository
                    .findByUsernameIgnoreCase(userMapping.emailAddress());

                if (userOpt.isEmpty()) {
                    log.error("No user found in DB for email {}, skipping cache write", userMapping.emailAddress());
                    continue;
                }

                String tokenSubject = userOpt.get().getTokenSubject();
                if (tokenSubject == null || tokenSubject.isBlank()) {
                    log.error("User found for email {} but token_subject is blank, skipping cache write",
                              userMapping.emailAddress());
                    continue;
                }

                String cacheKey = UserRoleMappingCacheService.ROLE_MAPPING_USER_PREFIX + tokenSubject;

                cacheService.putUserMapping(
                    cacheKey,
                    userMapping.businessUnitToRoles(),
                    properties.getUserTtl()
                );

                refreshedKeys.add(cacheKey);
                refreshedCount++;
            }

            for (String invalidEmail : parseResult.invalidEmails()) {
                userRepository.findByUsernameIgnoreCase(invalidEmail)
                    .ifPresent(user -> {
                        String tokenSubject = user.getTokenSubject();
                        if (tokenSubject != null && !tokenSubject.isBlank()) {
                            String cacheKey = UserRoleMappingCacheService.ROLE_MAPPING_USER_PREFIX + tokenSubject;
                            cacheService.deleteUserKey(cacheKey);
                            log.error("Invalid repeated CSV rows for email {}, deleted cache key {}",
                                      invalidEmail, cacheKey);
                        } else {
                            log.error("Invalid repeated CSV rows for email {}, but token_subject is blank",
                                      invalidEmail);
                        }
                    });
            }

            cacheService.deleteStaleUserKeys(refreshedKeys);
            cacheService.setLastUpdateAt(lastModifiedAt, properties.getLastUpdateTtl());

            log.info(
                "Role Mapping file cache refresh completed successfully {} users have been refreshed. "
                    + "Last Updated at {}",
                refreshedCount,
                lastModifiedAt
            );
        }
    }
}
