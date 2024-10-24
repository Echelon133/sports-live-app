package ml.echelon133.matchservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ml.echelon133.common.constants.DateFormatConstants;
import ml.echelon133.common.event.MatchEventDetailsMixIn;
import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEventMixIn;
import ml.echelon133.matchservice.event.model.dto.InsertMatchEvent;
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
		var mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		// add information about serialization/deserialization of InsertMatchEvent's subclasses
		mapper.addMixIn(InsertMatchEvent.class, InsertMatchEventMixIn.class);

		// add information about serialization/deserialization of MatchEventDetails' subclasses
		mapper.addMixIn(MatchEventDetails.class, MatchEventDetailsMixIn.class);

		// enable java time module
		mapper.registerModules(new JavaTimeModule());

		return mapper;
	}

	@Bean
	public static MatchCriteriaValidator matchCriteriaValidator() {
		return new MatchCriteriaValidator(DateFormatConstants.DATE_FORMAT);
	}
}
