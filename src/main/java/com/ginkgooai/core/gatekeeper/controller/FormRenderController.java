package com.ginkgooai.core.gatekeeper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.client.identity.UserClient;
import com.ginkgooai.core.gatekeeper.client.identity.dto.UserInfo;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.FormRenderDTO;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import com.ginkgooai.core.gatekeeper.util.FormRenderingHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/gatekeeper/v1/render")
@Tag(name = "Form Rendering", description = "APIs for rendering forms")
@Slf4j
public class FormRenderController {

	private final FormDefinitionService formDefinitionService;

	private final FormRenderingHelper formRenderingHelper;

	private final ObjectMapper objectMapper;

	private final UserClient userClient;

	@Autowired
	public FormRenderController(FormDefinitionService formDefinitionService, FormRenderingHelper formRenderingHelper,
			ObjectMapper objectMapper, @Autowired(required = false) UserClient userClient) {
		this.formDefinitionService = formDefinitionService;
		this.formRenderingHelper = formRenderingHelper;
		this.objectMapper = objectMapper;
		this.userClient = userClient;
	}

	@GetMapping("/forms/{formIdentifier}")
	@Operation(summary = "Get form definition for rendering",
			description = "Retrieves a form definition by its identifier (name or ID) optimized for frontend rendering",
			parameters = { @Parameter(name = "formIdentifier", description = "Form name or ID", required = true),
					@Parameter(name = "userId", description = "User ID for prefilling form data"),
					@Parameter(name = "version", description = "Optional form version, defaults to latest") },
			responses = {
					@ApiResponse(responseCode = "200", description = "Form definition found",
							content = @Content(schema = @Schema(implementation = FormRenderDTO.class))),
					@ApiResponse(responseCode = "404", description = "Form definition not found"),
					@ApiResponse(responseCode = "500", description = "Server error") })
	public ResponseEntity<?> getFormForRendering(@PathVariable String formIdentifier,
			@RequestParam(required = false) String userId, @RequestParam(required = false) String version,
			@RequestHeader(value = "Authorization", required = false) String authHeader) {

		try {
			log.info("Requesting form for rendering: identifier={}, userId={}, version={}", formIdentifier, userId,
					version);

			// 1. Find form definition - first try as ID
			Optional<FormDefinitionDTO> formDefinitionOpt = formDefinitionService
				.findFormDefinitionById(formIdentifier);

			// If not found by ID, try by name
			if (formDefinitionOpt.isEmpty()) {
				log.debug("Form not found by ID, trying by name: {}", formIdentifier);
				// TODO: Implement a service method to find by name and version
				// formDefinitionOpt =
				// formDefinitionService.findFormDefinitionByNameAndVersion(formIdentifier,
				// version);

				// Temporary simplified logic
				formDefinitionOpt = formDefinitionService
					.findFormDefinitions(null, formIdentifier, FormDefinition.FormStatus.PUBLISHED)
					.getContent()
					.stream()
					.findFirst();
			}

			// If still not found, return 404
			if (formDefinitionOpt.isEmpty()) {
				log.warn("Form definition not found: {}", formIdentifier);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Form definition not found: " + formIdentifier);
			}

			FormDefinitionDTO formDefinition = formDefinitionOpt.get();

			// 2. Check form status
			// if (formDefinition.getStatus() != FormDefinition.FormStatus.PUBLISHED) {
			// log.warn("Attempt to render non-active form: {} with status {}",
			// formIdentifier,
			// formDefinition.getStatus());
			// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			// .body("Form is not active for rendering. Current status: "
			// + formDefinition.getStatus());
			// }

			// 3. Convert form to rendering-friendly format
			FormRenderDTO renderDTO = new FormRenderDTO(formDefinition);

			// 4. Apply initialization logic
			if (userId != null && formDefinition.getInitialLogic() != null) {
				applyInitialLogic(renderDTO, formDefinition.getInitialLogic(), userId);
			}

			log.info("Successfully prepared form for rendering: {}", formDefinition.getName());
			return ResponseEntity.ok(renderDTO);

		}
		catch (Exception e) {
			log.error("Error preparing form for rendering: {}", formIdentifier, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error preparing form for rendering: " + e.getMessage());
		}
	}

	/**
	 * Apply form initialization logic, including data prefilling
	 */
	private void applyInitialLogic(FormRenderDTO renderDTO, JsonNode initialLogic, String userId) {
		try {
			// Process prefill fields
			if (initialLogic.has("prefillFields") && initialLogic.has("prefillSource")) {
				String prefillSource = initialLogic.get("prefillSource").asText();

				// Replace URL parameters
				prefillSource = prefillSource.replace("{userId}", userId);

				// Get prefill data
				Map<String, Object> prefillData = fetchPrefillData(prefillSource, userId);
				if (prefillData != null && !prefillData.isEmpty()) {
					renderDTO.setPrefillData(prefillData);
				}
			}

			// Process other initialization logic...

		}
		catch (Exception e) {
			log.error("Error applying initial logic: {}", e.getMessage(), e);
		}
	}

	/**
	 * Fetch prefill data from the specified source
	 */
	private Map<String, Object> fetchPrefillData(String prefillSource, String userId) {
		// Example: Handle internal user API
		if (prefillSource.contains("/api/users/") && userClient != null) {
			try {
				ResponseEntity<UserInfo> response = userClient.getUserById(userId);
				if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
					UserInfo userInfo = response.getBody();

					Map<String, Object> prefillData = new HashMap<>();
					prefillData.put("firstName", userInfo.getFirstName());
					prefillData.put("lastName", userInfo.getLastName());
					prefillData.put("email", userInfo.getEmail());
					// Add more fields as needed

					return prefillData;
				}
			}
			catch (Exception e) {
				log.warn("Error fetching user data for prefill: {}", e.getMessage());
			}
		}

		// Handle other data sources here...

		return new HashMap<>();
	}

}
