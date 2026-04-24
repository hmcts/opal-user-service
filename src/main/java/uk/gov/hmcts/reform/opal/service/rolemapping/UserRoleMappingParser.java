package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleMappingParser {

    // --- Column constants ---
    private static final String EMAIL_ADDRESS = "email_address";
    private static final String BUSINESS_UNIT_ID = "business_unit_id";
    private static final String ROLE_ID = "role_id";

    public MappingFileProcessingResult parse(Reader reader) throws IOException {

        Map<String, ParsedUserMappingBuilder> userMappings = new LinkedHashMap<>();
        Set<String> invalidEmails = new LinkedHashSet<>();

        try (CSVParser parser = CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader)) {

            String previousEmail = null;

            for (CSVRecord record : parser) {

                // --- Skip malformed rows ---
                if (record.size() < 3) {
                    log.warn("Skipping malformed CSV row {}: {}", record.getRecordNumber(), record);
                    continue;
                }

                String email = record.get(EMAIL_ADDRESS).trim().toLowerCase();
                String businessUnit = record.get(BUSINESS_UNIT_ID).trim();
                String role = record.get(ROLE_ID).trim();

                // --- Detect non-contiguous user ---
                if (previousEmail != null
                    && !email.equals(previousEmail)
                    && userMappings.containsKey(email)) {

                    log.warn("Email {} appears non-contiguously - marking as invalid", email);

                    invalidEmails.add(email);
                    userMappings.remove(email);
                }

                // --- Skip if already invalid ---
                if (invalidEmails.contains(email)) {
                    previousEmail = email;
                    continue;
                }

                // --- Add mapping ---
                userMappings
                    .computeIfAbsent(email, ParsedUserMappingBuilder::new)
                    .add(businessUnit, role);

                previousEmail = email;
            }
        }

        return new MappingFileProcessingResult(
            userMappings.values().stream()
                .map(ParsedUserMappingBuilder::build)
                .toList(),
            invalidEmails
        );
    }

    // --- Helper builder ---
    private static class ParsedUserMappingBuilder {

        private final String email;
        private final Map<String, Set<String>> businessUnitToRoles = new LinkedHashMap<>();

        ParsedUserMappingBuilder(String email) {
            this.email = email;
        }

        void add(String businessUnit, String role) {
            businessUnitToRoles
                .computeIfAbsent(businessUnit, k -> new LinkedHashSet<>())
                .add(role);
        }

        ParsedUserMapping build() {
            return new ParsedUserMapping(email, businessUnitToRoles);
        }
    }
}
