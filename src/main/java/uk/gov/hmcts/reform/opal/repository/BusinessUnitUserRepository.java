package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;

import java.util.List;

@Repository
public interface BusinessUnitUserRepository extends JpaRepository<BusinessUnitUserEntity, String>,
    JpaSpecificationExecutor<BusinessUnitUserEntity> {

    List<BusinessUnitUserEntity> findAllByUser_UserId(Long userId);
}
