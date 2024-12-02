package pl.echelon133.competitionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
// TODO: Remove this autoconfiguration import when spring-cloud.version gets bumped and this fix is not required anymore
// Fixes https://stackoverflow.com/questions/74593433/consider-defining-a-bean-of-type-org-springframework-cloud-openfeign-feignconte
@ImportAutoConfiguration({FeignAutoConfiguration.class})
@EnableFeignClients
public class CompetitionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompetitionServiceApplication.class, args);
    }
}
