package org.example.jobautomation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService jobDiscoveryExecutor() {
        return Executors.newFixedThreadPool(8);
    }
    // Dedicated pool for AI scoring: small on purpose to respect OpenAI rate limits
    @Bean(destroyMethod = "shutdown")
    public ExecutorService jobScoringExecutor() {
        return Executors.newFixedThreadPool(5);
    }

    // Dedicated pool for per-board HTTP fetches (Greenhouse/Lever). Sized to cover all boards in one wave.
    @Bean(destroyMethod = "shutdown")
    public ExecutorService jobSourceFetchExecutor() {
        return Executors.newFixedThreadPool(16);
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService applyExecutor() {
        return Executors.newFixedThreadPool(2); // browsers are heavy (only 2 threads
    }

}