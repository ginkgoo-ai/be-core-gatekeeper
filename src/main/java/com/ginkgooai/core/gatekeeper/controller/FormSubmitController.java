package com.ginkgooai.core.gatekeeper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.FormSubmissionDTO;
import com.ginkgooai.core.gatekeeper.dto.FormSubmissionResultDTO;
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
@RequestMapping("/api/gatekeeper/v1/submit")
@Tag(name = "Form Submission", description = "APIs for submitting form data")
@Slf4j
public class FormSubmitController {

	private final FormDefinitionService formDefinitionService;

	private final FormValidationService formValidationService;

	private final QuestionnaireService questionnaireService;

	private final ObjectMapper objectMapper;

	private final RestTemplate restTemplate;

	@Autowired
	public FormSubmitController(FormDefinitionService formDefinitionService,
			FormValidationService formValidationService, ObjectMapper objectMapper, RestTemplate restTemplate,
			QuestionnaireService questionnaireService) {
		this.formDefinitionService = formDefinitionService;
		this.formValidationService = formValidationService;
		this.objectMapper = objectMapper;
		this.restTemplate = restTemplate;
		this.questionnaireService = questionnaireService;
	}

	@PostMapping("/forms/{formIdentifier}")
	@Operation(summary = "Submit form data",
			description = "Validates and submits form data based on the form definition",
			parameters = { @Parameter(name = "formIdentifier", description = "Form name or ID", required = true),
					@Parameter(name = "userId", description = "User ID associated with the submission") },
			responses = {
					@ApiResponse(responseCode = "200", description = "Form data submitted successfully",
							content = @Content(schema = @Schema(implementation = FormSubmissionResultDTO.class))),
					@ApiResponse(responseCode = "400", description = "Invalid form data"),
					@ApiResponse(responseCode = "404", description = "Form definition not found"),
					@ApiResponse(responseCode = "500", description = "Server error") })
	public ResponseEntity<?> submitForm(@PathVariable String formIdentifier,
			@RequestParam(required = false) String userId, @RequestBody Map<String, Object> formData,
			@RequestHeader(value = "Authorization", required = false) String authHeader,
			@RequestParam Map<String, String> allRequestParams) {

		try {
			log.info("Form submission received: form={}, userId={}", formIdentifier, userId);

			// 1. Get form definition - first try as ID
			Optional<FormDefinitionDTO> formDefinitionOpt = formDefinitionService
				.findFormDefinitionById(formIdentifier);

			// If not found by ID, try by name
			if (formDefinitionOpt.isEmpty()) {
				// Try to find by name
				formDefinitionOpt = formDefinitionService
					.findFormDefinitions(null, formIdentifier, FormDefinition.FormStatus.PUBLISHED)
					.getContent()
					.stream()
					.findFirst();
			}

			// If still not found, return 404
			if (formDefinitionOpt.isEmpty()) {
				log.warn("Form definition not found for submission: {}", formIdentifier);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Form definition not found: " + formIdentifier);
			}

			FormDefinitionDTO formDefinition = formDefinitionOpt.get();

			// 2. Verify form status
			// if (formDefinition.getStatus() != FormDefinition.FormStatus.PUBLISHED) {
			// log.warn("Attempt to submit to non-active form: {} with status {}",
			// formIdentifier,
			// formDefinition.getStatus());
			// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			// .body("Form is not active for submission. Current status: "
			// + formDefinition.getStatus());
			// }

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
			// if (formDefinition.getSubmissionLogic() != null) {
			return processSubmission(formDefinition, formData, userId, authHeader, allRequestParams);
			// }

		}
		catch (Exception e) {
			log.error("Error processing form submission: {}", formIdentifier, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error processing form submission: " + e.getMessage());
		}
	}

	/**
	 * Process form submission according to the form definition's submission logic
	 */
	private ResponseEntity<?> processSubmission(FormDefinitionDTO formDefinition, Map<String, Object> formData,
			String userId, String authHeader, Map<String, String> requestParams) {

		// 从formData中获取前端传来的queryParams
		@SuppressWarnings("unchecked")
		Map<String, String> queryParams = (Map<String, String>) formData.get("queryParams");
		if (queryParams != null) {
			// 合并queryParams到requestParams
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
			submissionDTO.setResponses((Map) formData.get("responseData"));

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
