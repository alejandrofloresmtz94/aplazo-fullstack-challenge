package com.aplazo.challenge.aplazo_fullstack_challenge;

import org.springframework.boot.SpringApplication;

public class TestAplazoFullstackChallengeApplication {

	public static void main(String[] args) {
		SpringApplication.from(AplazoFullstackChallengeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
