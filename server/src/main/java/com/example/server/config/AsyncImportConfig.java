package com.example.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(ImportProperties.class)
public class AsyncImportConfig {

    @Bean(name = "importTaskExecutor")
    public TaskExecutor importTaskExecutor(ImportProperties importProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(importProperties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(importProperties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(importProperties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix(importProperties.getAsync().getThreadNamePrefix());
        executor.initialize();
        return executor;
    }
}
