package pl.echelon133.competitionservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
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
    public static ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        // enable java time module
        mapper.registerModules(new JavaTimeModule());
        // these have to be configured manually, otherwise deserializing Page objects received via the feign client
        // fails and throws an exception
        mapper.registerModules(new PageJacksonModule());
        mapper.registerModule(new SortJacksonModule());
        return mapper;
    }

    @Bean
    public Executor executor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("async-task-executor-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(6);
        executor.initialize();
        return executor;
    }
}
