package com.beautyfinder.b2b.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
@EnableScheduling
class AsyncConfig {

    @Bean("auditExecutor")
    fun auditExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 5
        queueCapacity = 1000
        setThreadNamePrefix("audit-")
        setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        initialize()
    }
}
