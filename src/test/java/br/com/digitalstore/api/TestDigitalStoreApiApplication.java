package br.com.digitalstore.api;

import org.springframework.boot.SpringApplication;

public class TestDigitalStoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(DigitalStoreApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
