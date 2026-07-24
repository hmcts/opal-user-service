package uk.gov.hmcts.reform.opal.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.ClientOptions;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import uk.gov.hmcts.reform.opal.config.properties.CacheConfiguration;

class CacheConfigTest {

    @Test
    void redisConnectionFactory_shouldEnableSsl_whenUsingRedissUrl() throws Exception {
        CacheConfig config = cacheConfigWith("rediss://:password@opal-stg.redis.cache.windows.net:6380?tls=true");

        LettuceConnectionFactory factory = (LettuceConnectionFactory) config.redisConnectionFactory();

        assertThat(factory.isUseSsl()).isTrue();
    }

    @Test
    void redisConnectionFactory_shouldDisableSsl_whenUsingRedisUrl() throws Exception {
        CacheConfig config = cacheConfigWith("redis://localhost:6379");

        LettuceConnectionFactory factory = (LettuceConnectionFactory) config.redisConnectionFactory();

        assertThat(factory.isUseSsl()).isFalse();
    }

    @Test
    void redisConnectionFactory_shouldDisableMaintenanceNotifications() throws Exception {
        CacheConfig config = cacheConfigWith("redis://localhost:6379");

        LettuceConnectionFactory factory = (LettuceConnectionFactory) config.redisConnectionFactory();

        assertThat(factory.getClientConfiguration().getClientOptions())
            .isPresent()
            .map(ClientOptions::getMaintNotificationsConfig)
            .map(configValue -> configValue.maintNotificationsEnabled())
            .hasValue(false);
    }

    @Test
    void redisCacheManager_shouldReturnRedisCacheManager() throws Exception {
        CacheConfig config = cacheConfigWith("redis://localhost:6379");
        RedisConnectionFactory redisConnectionFactory = config.redisConnectionFactory();

        CacheManager cacheManager = config.redisCacheManager(redisConnectionFactory);

        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void generateKeyFromList_shouldCreateStableKeysForOptionalAndListInputs() throws Exception {
        CacheConfig config = cacheConfigWith("redis://localhost:6379");
        KeyGenerator keyGenerator = config.generateKeyFromList();

        Object generatedKey = keyGenerator.generate(this, sampleMethodReference(),
            Optional.of(List.of("beta", "alpha")), Optional.empty(), "user");

        assertThat(generatedKey).isEqualTo("alpha_beta_noFilter_user");
    }

    private CacheConfig cacheConfigWith(String redisUrl) throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setUserStateTimeoutMinutes(30L);
        CacheConfig cacheConfig = new CacheConfig(cacheConfiguration);
        setField(cacheConfig, "redisUrl", redisUrl);
        return cacheConfig;
    }

    private Method sampleMethodReference() throws NoSuchMethodException {
        return getClass().getDeclaredMethod("sampleMethod");
    }

    @SuppressWarnings("unused")
    private void sampleMethod() {
        // Method handle used only to satisfy the KeyGenerator signature.
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
