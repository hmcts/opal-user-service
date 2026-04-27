package uk.gov.hmcts.reform.opal;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static uk.gov.hmcts.reform.opal.TestContainerConfig.REDIS_CONTAINER;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"integration"})
@ContextConfiguration(classes = {TestContainerConfig.class})
@SuppressWarnings("HideUtilityClassConstructor")
public class BaseIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainerConfig.POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", TestContainerConfig.POSTGRES_CONTAINER::getPassword);
        registry.add("spring.data.redis.url", REDIS_CONTAINER::getRedisURI);
    }

}
