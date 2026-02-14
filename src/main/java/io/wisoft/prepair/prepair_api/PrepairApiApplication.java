package io.wisoft.prepair.prepair_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class PrepairApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrepairApiApplication.class, args);
	}

}
