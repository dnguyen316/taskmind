package com.taskmind.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class ChatSessionStoreConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(InMemoryChatSessionStore.class, RedisChatSessionStore.class)
            .withBean(ObjectMapper.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

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
