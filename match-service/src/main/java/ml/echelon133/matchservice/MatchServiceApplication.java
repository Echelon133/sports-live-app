package ml.echelon133.matchservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ml.echelon133.common.constants.DateFormatConstants;
import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.matchservice.event.model.dto.UpsertMatchEvent;
import ml.echelon133.matchservice.match.controller.validators.MatchCriteriaValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class MatchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchServiceApplication.class, args);
	}

	@Bean
	public static ObjectMapper objectMapper() {
		var mapper = new ObjectMapper();

		// add information about serialization/deserialization of UpsertMatchEvent's subtypes
		mapper.addMixIn(UpsertMatchEvent.class, UpsertMatchEvent.class);

		// add information about serialization/deserialization of MatchEventDetails' subtypes
		mapper.addMixIn(MatchEventDetails.class, MatchEventDetails.class);

		// enable java time module
		mapper.registerModules(new JavaTimeModule());

		return mapper;
	}

	@Bean
	public static MatchCriteriaValidator matchCriteriaValidator() {
		return new MatchCriteriaValidator(DateFormatConstants.DATE_FORMAT);
	}

	@Bean
	public static Clock clock() {
		return Clock.systemUTC();
	}
}
