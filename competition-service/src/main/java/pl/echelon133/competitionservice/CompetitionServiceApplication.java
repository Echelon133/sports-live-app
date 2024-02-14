package pl.echelon133.competitionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableFeignClients
@EnableAsync
public class CompetitionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompetitionServiceApplication.class, args);
    }

    @Bean
    public Executor executor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("async-executor-");
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.initialize();
        return executor;
    }
}
