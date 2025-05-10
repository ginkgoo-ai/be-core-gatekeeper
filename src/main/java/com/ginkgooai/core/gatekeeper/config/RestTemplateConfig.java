package com.ginkgooai.core.gatekeeper.config;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for RestTemplate Uses the latest Spring 6 APIs for configuring HTTP
 * clients
 */
@Configuration
@Slf4j
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

		return restTemplateBuilder.requestFactory(() -> factory)
			.additionalInterceptors(new LoggingInterceptor())
			.errorHandler(new CustomResponseErrorHandler())
			.build();
	}

	/**
	 * Custom HTTP request/response logging interceptor
	 */
	static class LoggingInterceptor implements ClientHttpRequestInterceptor {

		private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			// Log request
			if (log.isDebugEnabled()) {
				log.debug("HTTP Request: {} {}", request.getMethod(), request.getURI());
				log.debug("Request Headers: {}", request.getHeaders());
			}

			// Execute request and capture start time
			long startTime = System.currentTimeMillis();
			ClientHttpResponse response = execution.execute(request, body);
			long duration = System.currentTimeMillis() - startTime;

			// Log response
			if (log.isDebugEnabled()) {
				log.debug("HTTP Response: {} ({}ms)", response.getStatusCode(), duration);
				log.debug("Response Headers: {}", response.getHeaders());
			}

			return response;
		}

	}

	/**
	 * Custom error handler for RestTemplate
	 */
	static class CustomResponseErrorHandler extends DefaultResponseErrorHandler {

		private static final Logger log = LoggerFactory.getLogger(CustomResponseErrorHandler.class);

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			// Log detailed error information
			log.error("RestTemplate Error: {} {}", response.getStatusCode(), response.getStatusText());

			// Delegate to default handler for exception throwing
			super.handleError(response);
		}

	}

}
