package uk.gov.hmcts.reform.opal.repository.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.opal.dto.search.LogActionSearchDto;
import uk.gov.hmcts.reform.opal.entity.LogActionEntity;
import uk.gov.hmcts.reform.opal.entity.LogActionEntity_;

public class LogActionSpecs extends EntitySpecs<LogActionEntity> {

    public Specification<LogActionEntity> findBySearchCriteria(LogActionSearchDto criteria) {
        return Specification.allOf(specificationList(
            numericShort(criteria.getLogActionId()).map(LogActionSpecs::equalsLogActionId),
            notBlank(criteria.getLogActionName()).map(LogActionSpecs::equalsLogActionName)
        ));
    }

    public static Specification<LogActionEntity> equalsLogActionId(Short logActionId) {
        return (root, query, builder) -> equalsLogActionIdPredicate(root, builder, logActionId);
    }

    public static Specification<LogActionEntity> equalsLogActionName(String logActionName) {
        return (root, query, builder) -> likeLogActionNamePredicate(root, builder, logActionName);
    }

    public static Predicate equalsLogActionIdPredicate(
        From<?, LogActionEntity> from, CriteriaBuilder builder, Short logActionId) {
        return builder.equal(from.get(LogActionEntity_.logActionId), logActionId);
    }

    public static Predicate likeLogActionNamePredicate(
        From<?, LogActionEntity> from, CriteriaBuilder builder, String logActionName) {
        return likeWildcardPredicate(from.get(LogActionEntity_.logActionName), builder, logActionName);
    }

}
