package ml.echelon133.matchservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ml.echelon133.common.constants.DateFormatConstants;
import ml.echelon133.matchservice.match.controller.validators.MatchCriteriaValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class MatchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchServiceApplication.class, args);
	}

	@Bean
	public static ObjectMapper objectMapper() {
		return new ObjectMapper().registerModules(new JavaTimeModule());
	}

	@Bean
	public static MatchCriteriaValidator matchCriteriaValidator() {
		return new MatchCriteriaValidator(DateFormatConstants.DATE_FORMAT);
	}
}
