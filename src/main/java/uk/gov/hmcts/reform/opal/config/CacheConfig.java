package uk.gov.hmcts.reform.opal.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.MaintNotificationsConfig;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.health.DataRedisHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import uk.gov.hmcts.reform.opal.config.properties.CacheConfiguration;

@Slf4j(topic = "opal.CacheConfig")
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheConfiguration cacheConfiguration;
    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisURI redisUri = RedisURI.create(redisUrl);
        RedisConfiguration redisConfiguration = LettuceConnectionFactory.createRedisConfiguration(redisUri);
        LettuceClientConfigurationBuilder clientConfigurationBuilder = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                               .maintNotificationsConfig(MaintNotificationsConfig.disabled())
                               .build());
        if (redisUri.isSsl()) {
            clientConfigurationBuilder.useSsl();
        }
        return new LettuceConnectionFactory(redisConfiguration, clientConfigurationBuilder.build());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        RedisSerializer<String> keySerializer = redisKeySerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        RedisSerializer<Object> valueSerializer = redisValueSerializer();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(redisTtlDuration())
            .serializeKeysWith(SerializationPair.fromSerializer(redisKeySerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(redisValueSerializer()));

        return logCacheDetails(RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .build());
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("redis")
    public DataRedisHealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return new DataRedisHealthIndicator(redisConnectionFactory);
    }

    @Bean("KeyGeneratorForOptionalList")
    public KeyGenerator generateKeyFromList() {
        return (target, method, params) -> Arrays.stream(params)
            .flatMap(param -> {
                if (param instanceof Optional<?> optional) {
                    return optional.map(this::generateKeyParts)
                        .orElseGet(() -> Stream.of("noFilter"));
                }
                return generateKeyParts(param);
            })
            .collect(Collectors.joining("_"));
    }

    public CacheManager logCacheDetails(CacheManager cacheManager) {
        log.info("------------------------------");
        log.info("Cache Configuration Details:");
        log.info("Redis Url: {}", redisUrl);
        log.info("Redis TTL (duration): {}", redisTtlDuration());
        log.info("Cache Manager: {}", cacheManager.getClass().getName());
        if (cacheManager instanceof RedisCacheManager) {
            log.info("Using Redis Cache Manager");
        }
        log.info("Available Caches:");
        cacheManager.getCacheNames().forEach(cacheName -> log.debug("- {}", cacheName));
        log.info("------------------------------");
        return cacheManager;
    }

    private RedisSerializer<Object> redisValueSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build())
            .writer((mapper, source) -> {
                // Required to account for List types that are unmodifiable.
                Object value = source instanceof List<?> list ? new ArrayList<>(list) : source;
                return mapper.writeValueAsBytes(value);
            })
            .build();
    }

    private RedisSerializer<String> redisKeySerializer() {
        return RedisSerializer.string();
    }

    private Duration redisTtlDuration() {
        return Duration.ofMinutes(cacheConfiguration.getUserStateTimeoutMinutes());
    }

    private Stream<String> generateKeyParts(Object filter) {
        return switch (filter) {
            case String stringValue -> Stream.of(stringValue);
            case List<?> listValue -> listValue.stream().map(Object::toString).sorted();
            default -> Stream.empty();
        };
    }
}
