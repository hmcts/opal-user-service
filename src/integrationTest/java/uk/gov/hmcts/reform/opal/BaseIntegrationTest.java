package uk.gov.hmcts.reform.opal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static uk.gov.hmcts.reform.opal.TestContainerConfig.REDIS_CONTAINER;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {Application.class, TestContainerConfig.class}
)
@ActiveProfiles({"integration"})
@SuppressWarnings("HideUtilityClassConstructor")
@AutoConfigureMockMvc
public class BaseIntegrationTest {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainerConfig.POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", TestContainerConfig.POSTGRES_CONTAINER::getPassword);
        registry.add("spring.data.redis.url", REDIS_CONTAINER::getRedisURI);
    }

}
