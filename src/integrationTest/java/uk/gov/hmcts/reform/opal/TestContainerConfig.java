package uk.gov.hmcts.reform.opal;

import com.redis.testcontainers.RedisContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Slf4j
public class TestContainerConfig {

    public static final PostgreSQLContainer POSTGRES_CONTAINER;
    public static final RedisContainer REDIS_CONTAINER;
    public static final GenericContainer<?> AZURITE_CONTAINER;

    private static final String CONTAINER_NAME = "role-mapping";
    private static final String BLOB_NAME = "user-mapping.csv";

    @DynamicPropertySource
    static void registerBlobProperties(DynamicPropertyRegistry registry) {
        registry.add("blob.mapping-file.connection-string", TestContainerConfig::connectionString);
        registry.add("blob.mapping-file.container-name", () -> CONTAINER_NAME);
        registry.add("blob.mapping-file.blob-name", () -> BLOB_NAME);
    }

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:17.5"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

        POSTGRES_CONTAINER.start();

        REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:6.2.6"))
            .withExposedPorts(6379);
        REDIS_CONTAINER.start();

        AZURITE_CONTAINER = new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite")
            .withCommand("azurite-blob --blobHost 0.0.0.0 --blobPort 10000")
            .withExposedPorts(10000);
        AZURITE_CONTAINER.start();

    }

    private static String connectionString() {
        return "UseDevelopmentStorage=true";
    }
}
