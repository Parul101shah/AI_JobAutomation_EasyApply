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
}
