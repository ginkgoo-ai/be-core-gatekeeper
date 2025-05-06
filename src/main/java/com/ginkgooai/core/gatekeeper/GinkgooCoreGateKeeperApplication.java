package com.ginkgooai.core.gatekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
public class GinkgooCoreGateKeeperApplication {

	public static void main(String[] args) {
		SpringApplication.run(GinkgooCoreGateKeeperApplication.class, args);
	}

}
