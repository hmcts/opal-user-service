package uk.gov.hmcts.reform.opal;

import java.util.Set;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

public class HibernateIntegrationSchemaFilterProvider implements SchemaFilterProvider {

    private static final Set<String> IGNORED_TABLES = Set.of("log_actions", "log_audit_details");
    private static final Set<String> IGNORED_SEQUENCES = Set.of("log_action_id_seq", "log_audit_detail_id_seq");

    private static final SchemaFilter FILTER = new SchemaFilter() {
        @Override
        public boolean includeNamespace(Namespace namespace) {
            return true;
        }

        @Override
        public boolean includeTable(Table table) {
            return !IGNORED_TABLES.contains(table.getName());
        }

        @Override
        public boolean includeSequence(Sequence sequence) {
            return !IGNORED_SEQUENCES.contains(sequence.getName().getSequenceName().getText());
        }
    };

    @Override
    public SchemaFilter getCreateFilter() {
        return FILTER;
    }

    @Override
    public SchemaFilter getDropFilter() {
        return FILTER;
    }

    @Override
    public SchemaFilter getTruncatorFilter() {
        return FILTER;
    }

    @Override
    public SchemaFilter getMigrateFilter() {
        return FILTER;
    }

    @Override
    public SchemaFilter getValidateFilter() {
        return FILTER;
    }
}
