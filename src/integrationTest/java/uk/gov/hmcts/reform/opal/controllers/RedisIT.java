package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles({"integration"})
@Slf4j(topic = "opal.RedisIT")
@DisplayName("RedisIT - just checking Redis is available to our ITs")
class RedisIT extends AbstractIntegrationTest {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void storeAndRetrieve() {
        redisTemplate.opsForValue().set("key", "value");
        assertThat("value").isEqualTo(redisTemplate.opsForValue().get("key"));
    }
}
