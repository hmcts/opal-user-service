package uk.gov.hmcts.reform.opal.dev;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfiguration {

    @Bean
    @ServiceConnection
    @RestartScope
    PostgreSQLContainer databaseContainer() {
        return new PostgreSQLContainer("postgres:17.5")
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withName("testcontainers-postgres");
                cmd.withHostConfig(
                    new HostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(5433), new ExposedPort(5432))
                    )
                );
            })
            .withExposedPorts(5432)
            .withDatabaseName("opal-user-db")
            .withUsername("opal-db-user")
            .withPassword("opal-db-password")
            .withReuse(true);
    }

    @Bean
    @ServiceConnection
    @RestartScope
    RedisContainer redisContainer() {
        RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:6.2.6"))
            .withExposedPorts(6379)
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withName("testcontainers-redis");
                cmd.withHostConfig(
                    new HostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(6379), new ExposedPort(6379))
                    )
                );
            });
        redisContainer.start();
        return redisContainer;
    }

}
