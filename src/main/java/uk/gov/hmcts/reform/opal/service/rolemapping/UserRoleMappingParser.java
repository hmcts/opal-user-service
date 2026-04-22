package uk.gov.hmcts.reform.opal.service.rolemapping;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class UserRoleMappingParser {

    public ParseResult parse(Reader reader) throws IOException {
        Map<String, UserAccumulator> validAccumulators = new LinkedHashMap<>();
        Set<String> invalidEmails = new LinkedHashSet<>();
        Set<String> seenEmails = new LinkedHashSet<>();
        String previousEmail = null;

        try (CSVParser parser = CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build()
            .parse(reader)) {

            for (CSVRecord record : parser) {
                if (record.size() < 3) {
                    throw new IllegalArgumentException("Invalid CSV row at line " + record.getRecordNumber());
                }

                String email = normalizeEmail(record.get("email_address"));
                String businessUnitId = normalizeValue(record.get("business_unit_id"));
                String roleId = normalizeValue(record.get("role_id"));

                if (email.isEmpty() || businessUnitId.isEmpty() || roleId.isEmpty()) {
                    throw new IllegalArgumentException("CSV row contains blank required values at line "
                                                           + record.getRecordNumber());
                }

                if (!email.equals(previousEmail)) {
                    if (seenEmails.contains(email)) {
                        invalidEmails.add(email);
                        validAccumulators.remove(email);
                        previousEmail = email;
                        continue;
                    }
                    seenEmails.add(email);
                    previousEmail = email;
                }

                if (invalidEmails.contains(email)) {
                    continue;
                }

                validAccumulators
                    .computeIfAbsent(email, UserAccumulator::new)
                    .add(businessUnitId, roleId);
            }
        }

        List<ParsedUserMapping> validUsers = validAccumulators.values().stream()
            .map(UserAccumulator::toParsedUserMapping)
            .toList();

        return new ParseResult(validUsers, invalidEmails);
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class UserAccumulator {
        private final String emailAddress;
        private final Map<String, Set<String>> businessUnitToRoles = new LinkedHashMap<>();

        private UserAccumulator(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        private void add(String businessUnitId, String roleId) {
            businessUnitToRoles
                .computeIfAbsent(businessUnitId, ignored -> new LinkedHashSet<>())
                .add(roleId);
        }

        private ParsedUserMapping toParsedUserMapping() {
            return new ParsedUserMapping(emailAddress, businessUnitToRoles);
        }
    }
}
