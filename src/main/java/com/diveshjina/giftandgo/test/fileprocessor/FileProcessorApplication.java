package com.diveshjina.giftandgo.test.fileprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.diveshjina.giftandgo.test.fileprocessor.configuration.BlockedProperties;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(BlockedProperties.class)
public class FileProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileProcessorApplication.class, args);
	}

}
