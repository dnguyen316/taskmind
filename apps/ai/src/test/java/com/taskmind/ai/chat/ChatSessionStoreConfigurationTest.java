package com.taskmind.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class ChatSessionStoreConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(InMemoryChatSessionStore.class, RedisChatSessionStore.class)
            .withBean(ObjectMapper.class)
            .withBean(StringRedisTemplate.class, ChatSessionStoreConfigurationTest::redisTemplate);

    private static StringRedisTemplate redisTemplate() {
        RedisConnectionFactory connectionFactory = (RedisConnectionFactory) Proxy.newProxyInstance(
                RedisConnectionFactory.class.getClassLoader(),
                new Class<?>[] {RedisConnectionFactory.class},
                (proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
        return new StringRedisTemplate(connectionFactory);
    }

    @Test
    void usesInMemoryStoreWhenRedisPropertyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChatSessionStore.class);
            assertThat(context.getBean(ChatSessionStore.class))
                    .isInstanceOf(InMemoryChatSessionStore.class);
        });
    }

    @Test
    void usesRedisStoreWhenRedisPropertyIsEnabled() {
        contextRunner
                .withPropertyValues("taskmind.ai.chat.redis-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatSessionStore.class);
                    assertThat(context.getBean(ChatSessionStore.class))
                            .isInstanceOf(RedisChatSessionStore.class);
                });
    }
}
