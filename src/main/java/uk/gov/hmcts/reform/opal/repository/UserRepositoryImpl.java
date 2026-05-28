package uk.gov.hmcts.reform.opal.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void refresh(UserEntity user) {
        entityManager.refresh(user);
    }
}
