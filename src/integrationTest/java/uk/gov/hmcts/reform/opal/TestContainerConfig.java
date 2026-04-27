package uk.gov.hmcts.reform.opal;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainerConfig {

    public static final PostgreSQLContainer POSTGRES_CONTAINER;
    public static final RedisContainer REDIS_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:17.5"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

        POSTGRES_CONTAINER.start();

        REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:6.2.6"))
            .withExposedPorts(6379);
        REDIS_CONTAINER.start();
    }
}
