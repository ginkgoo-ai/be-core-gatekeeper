package com.ginkgooai.core.gatekeeper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.client.identity.UserClient;
import com.ginkgooai.core.gatekeeper.client.identity.dto.UserInfo;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.*;
import com.ginkgooai.core.gatekeeper.dto.request.QuestionnaireSubmissionRequest;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import com.ginkgooai.core.gatekeeper.service.FormValidationService;
import com.ginkgooai.core.gatekeeper.service.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
@Tag(name = "Form Submission", description = "APIs for submitting form data")
@Slf4j
public class FormController {

	private final FormDefinitionService formDefinitionService;

	private final FormValidationService formValidationService;

	private final QuestionnaireService questionnaireService;

	private final UserClient userClient;
	
	private final RestTemplate restTemplate;

	@Autowired
	public FormController(FormDefinitionService formDefinitionService, FormValidationService formValidationService,
			UserClient userClient, RestTemplate restTemplate, QuestionnaireService questionnaireService) {
		this.formDefinitionService = formDefinitionService;
		this.formValidationService = formValidationService;
		this.userClient = userClient;
		this.restTemplate = restTemplate;
		this.questionnaireService = questionnaireService;
	}

	@GetMapping("/forms/{formId}")
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
	public ResponseEntity<?> getFormForRendering(@PathVariable("formId") String formId,
			@RequestParam(required = false) String userId, @RequestParam(required = false) String version,
			@RequestHeader(value = "Authorization", required = false) String authHeader) {

		try {
			log.info("Requesting form for rendering: identifier={}, userId={}, version={}", formId, userId, version);

			Optional<FormDefinitionDTO> formDefinitionOpt = formDefinitionService.findFormDefinitionById(formId);

			if (formDefinitionOpt.isEmpty()) {
				log.debug("Form not found by ID, trying by name: {}", formId);
				formDefinitionOpt = formDefinitionService
					.findFormDefinitions(null, formId, FormDefinition.FormStatus.PUBLISHED)
					.getContent()
					.stream()
					.findFirst();
			}

			if (formDefinitionOpt.isEmpty()) {
				log.warn("Form definition not found: {}", formId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Form definition not found: " + formId);
			}

			FormDefinitionDTO formDefinition = formDefinitionOpt.get();

			FormRenderDTO renderDTO = new FormRenderDTO(formDefinition);

			if (userId != null && formDefinition.getInitialLogic() != null) {
				applyInitialLogic(renderDTO, formDefinition.getInitialLogic(), userId);
			}

			log.info("Successfully prepared form for rendering: {}", formDefinition.getName());
			return ResponseEntity.ok(renderDTO);

		}
		catch (Exception e) {
			log.error("Error preparing form for rendering: {}", formId, e);
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

		return new HashMap<>();
	}

	@PostMapping("/forms/{formId}/results")
	@Operation(summary = "Submit form data",
			description = "Validates and submits form data based on the form definition",
			parameters = { @Parameter(name = "formId", description = "Form name or ID", required = true),
					@Parameter(name = "userId", description = "User ID associated with the submission") },
			responses = {
					@ApiResponse(responseCode = "200", description = "Form data submitted successfully",
							content = @Content(schema = @Schema(implementation = FormSubmissionResultDTO.class))),
					@ApiResponse(responseCode = "400", description = "Invalid form data"),
					@ApiResponse(responseCode = "404", description = "Form definition not found"),
					@ApiResponse(responseCode = "500", description = "Server error") })
	public ResponseEntity<?> submitForm(@PathVariable("formId") String formId,
			@RequestParam(required = false) String userId, @RequestBody Map<String, Object> formData,
			@RequestHeader(value = "Authorization", required = false) String authHeader,
			@RequestParam Map<String, String> allRequestParams) {

		try {
			log.info("Form submission received: form={}, userId={}", formId, userId);

			// 1. Get form definition - first try as ID
			Optional<FormDefinitionDTO> formDefinitionOpt = formDefinitionService
				.findFormDefinitionById(formId);

			// If not found by ID, try by name
			if (formDefinitionOpt.isEmpty()) {
				// Try to find by name
				formDefinitionOpt = formDefinitionService
					.findFormDefinitions(null, formId, FormDefinition.FormStatus.PUBLISHED)
					.getContent()
					.stream()
					.findFirst();
			}

			// If still not found, return 404
			if (formDefinitionOpt.isEmpty()) {
				log.warn("Form definition not found for submission: {}", formId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Form definition not found: " + formId);
			}

			FormDefinitionDTO formDefinition = formDefinitionOpt.get();

			// 3. Validate form data using FormValidationService
			Map<String, String> validationErrors = formValidationService.validateFormData(formDefinition,
					(Map<String, Object>) formData.get("responseData"));
			if (!validationErrors.isEmpty()) {
				log.warn("Form validation failed: {} errors", validationErrors.size());
				Map<String, Object> response = new HashMap<>();
				response.put("success", false);
				response.put("message", "Form validation failed");
				response.put("errors", validationErrors);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			// 4. Process submission according to submissionLogic
			return processSubmission(formDefinition, formData, userId, authHeader, allRequestParams);

		}
		catch (Exception e) {
			log.error("Error processing form submission: {}", formId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error processing form submission: " + e.getMessage());
		}
	}

	/**
	 * Convert name-based response data to label-based format
	 * @param formDefinition The form definition containing field metadata
	 * @param nameBasedResponses Response data using field names as keys
	 * @return Response data using field labels as keys
	 */
	private Map<String, Object> convertNameToLabelBasedResponses(FormDefinitionDTO formDefinition,
			Map<String, Object> nameBasedResponses) {
		if (nameBasedResponses == null || formDefinition == null || formDefinition.getSections() == null) {
			return nameBasedResponses;
		}

		// Create name to label mapping
		Map<String, String> nameToLabelMap = new HashMap<>();

		// Iterate all sections and fields to build name->label mapping
		for (SectionDefinitionDTO section : formDefinition.getSections()) {
			if (section == null || section.getFields() == null) {
				continue;
			}

			for (FieldDefinitionDTO field : section.getFields()) {
				if (field == null) {
					continue;
				}

				String name = field.getName();
				String label = field.getLabel();

				if (name != null && label != null) {
					nameToLabelMap.put(name, label);
					log.debug("Mapping field.name -> field.label: {} -> {}", name, label);
				}
			}
		}

		// Create label-based response data using the mapping
		Map<String, Object> labelBasedResponses = new HashMap<>();
		for (Map.Entry<String, Object> entry : nameBasedResponses.entrySet()) {
			String fieldName = entry.getKey();
			Object value = entry.getValue();

			// Use label as key if found; otherwise keep original name
			String key = nameToLabelMap.getOrDefault(fieldName, fieldName);
			labelBasedResponses.put(key, value);
		}

		log.info("Converted response data format: from name-based to label-based, field count: {}",
				labelBasedResponses.size());
		return labelBasedResponses;
	}
	
	/**
	 * Process form submission according to the form definition's submission logic
	 */
	private ResponseEntity<?> processSubmission(FormDefinitionDTO formDefinition, Map<String, Object> formData,
			String userId, String authHeader, Map<String, String> requestParams) {

		// Get queryParams from the form data submitted by client
		@SuppressWarnings("unchecked")
		Map<String, String> queryParams = (Map<String, String>) formData.get("queryParams");
		if (queryParams != null) {
			// Merge queryParams into requestParams
			requestParams.putAll(queryParams);
			log.info("Added URL query parameters from client: {}", queryParams);
		}

		try {
			// Handle Questionnaire Type
			// if (formDefinition.getFormType() == FormType.QUESTIONNAIRE) {
			log.info("Processing submission for QUESTIONNAIRE form: {}", formDefinition.getName());

			QuestionnaireSubmissionRequest submissionRequest = new QuestionnaireSubmissionRequest();
			submissionRequest.setFormDefinitionId(formDefinition.getId());
			submissionRequest.setUserId(userId);
			// Assuming the actual form responses are within a "responseData" key in the
			// formData
			Map<String, Object> responseData = (Map<String, Object>) formData.get("responseData");
			if (responseData == null) {
				log.warn("Form data for questionnaire {} is missing 'responseData' field.", formDefinition.getName());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Questionnaire submission data is missing 'responseData' field.");
			}
			submissionRequest.setResponseData(responseData);

			com.ginkgooai.core.gatekeeper.domain.QuestionnaireResult savedResponse = questionnaireService
				.saveResponse(submissionRequest);

			FormSubmissionResultDTO result = new FormSubmissionResultDTO();
			result.setSuccess(true);
			result.setMessage("Questionnaire submitted and saved successfully.");
			result.setTimestamp(new Date());
			result.setFormId(formDefinition.getId());
			// Convert savedResponse (domain object) to Map or a suitable DTO if needed
			// for the result data
			// For now, let's put the ID of the saved response.
			Map<String, Object> resultData = new HashMap<>();
			resultData.put("questionnaireResponseId", savedResponse.getId());
			resultData.put("submittedData", responseData); // Include submitted data for
															// confirmation
			result.setData(resultData);

			log.info("Questionnaire response saved successfully for form: {}, response ID: {}",
					formDefinition.getName(), savedResponse.getId());
			// return ResponseEntity.status(HttpStatus.CREATED).body(result);
			// }

			// Existing logic for other form types or if submissionLogic is present
			JsonNode submissionLogic = formDefinition.getSubmissionLogic();
			String targetService = null;

			if (submissionLogic == null || !submissionLogic.has("targetService")) {
				return ResponseEntity.status(HttpStatus.CREATED).body(result);
			}

			targetService = submissionLogic.get("targetService").asText();
			if (ObjectUtils.isEmpty(targetService)) {
				// If no targetService and not a questionnaire, return a generic success
				log.info(
						"Form {} has no specific submission logic (targetService) and is not a questionnaire. Returning generic success.",
						formDefinition.getName());
				result.setSuccess(true);
				result.setMessage("Form submitted successfully (no specific processing defined).");
				result.setTimestamp(new Date());
				result.setFormId(formDefinition.getId());
				result.setData(formData); // Return original form data
				return ResponseEntity.ok(result);
			}

			// 处理URL中的动态参数，替换{paramName}占位符
			if (targetService.contains("{") && targetService.contains("}")) {
				log.info("Detected dynamic parameters in targetService URL: {}", targetService);
				for (Map.Entry<String, String> param : requestParams.entrySet()) {
					String placeholder = "{" + param.getKey() + "}";
					if (targetService.contains(placeholder)) {
						targetService = targetService.replace(placeholder, param.getValue());
						log.info("Replaced placeholder {} with value {}", placeholder, param.getValue());
					}
				}
				log.info("Final targetService URL after parameter substitution: {}", targetService);
			}

			// Build submission DTO
			FormSubmissionDTO submissionDTO = new FormSubmissionDTO();
			submissionDTO.setQuestionnaireId(formDefinition.getId());
			submissionDTO.setQuestionnaireName(formDefinition.getName());
			submissionDTO.setUserId(userId);
			submissionDTO.setSubmittedAt(new Date());

			// Get original response data
			@SuppressWarnings("unchecked")
			Map<String, Object> originalResponses = (Map<String, Object>) formData.get("responseData");

			// Convert to format using field.label as keys
			Map<String, Object> labelBasedResponses = convertNameToLabelBasedResponses(formDefinition,
					originalResponses);
			submissionDTO.setResponses(labelBasedResponses);

			// Prepare HTTP request
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			if (authHeader != null && !authHeader.isEmpty()) {
				headers.set("Authorization", authHeader);
			}

			HttpEntity<FormSubmissionDTO> requestEntity = new HttpEntity<>(submissionDTO, headers);

			// Send request to target service
			log.info("Forwarding form submission to target service: {}", targetService);
			ResponseEntity<Map> response = restTemplate.postForEntity(targetService, requestEntity, Map.class);

			// Handle target service response
			if (response.getStatusCode().is2xxSuccessful()) {
				log.info("Target service processed submission successfully: {}", formDefinition.getName());
				result.setSuccess(true);
				result.setMessage("Form data processed successfully");
				result.setTimestamp(new Date());
				result.setFormId(formDefinition.getId());
				result.setData(response.getBody());

				return ResponseEntity.ok(result);
			}
			else {
				log.warn("Target service returned non-success status: {}", response.getStatusCodeValue());
				return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
			}

		}
		catch (Exception e) {
			log.error("Error forwarding submission to target service: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error forwarding form submission: " + e.getMessage());
		}
	}

}
