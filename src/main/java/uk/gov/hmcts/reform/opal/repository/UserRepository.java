package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String>,
    JpaSpecificationExecutor<UserEntity> {

    UserEntity findByUsername(String username);
}
