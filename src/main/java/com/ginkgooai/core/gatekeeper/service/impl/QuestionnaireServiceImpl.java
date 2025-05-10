package com.ginkgooai.core.gatekeeper.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.domain.QuestionnaireResult;
import com.ginkgooai.core.gatekeeper.dto.request.QuestionnaireSubmissionRequest;
import com.ginkgooai.core.gatekeeper.enums.FormType;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import com.ginkgooai.core.gatekeeper.repository.FormDefinitionRepository;
import com.ginkgooai.core.gatekeeper.repository.QuestionnaireResponseRepository;
import com.ginkgooai.core.gatekeeper.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireServiceImpl implements QuestionnaireService {

	private final QuestionnaireResponseRepository questionnaireResponseRepository;

	private final FormDefinitionRepository formDefinitionRepository;

	private final ObjectMapper objectMapper; // For converting Map to JsonNode

	@Override
	@Transactional
	public QuestionnaireResult saveResponse(QuestionnaireSubmissionRequest submissionRequest) {
		log.info("Attempting to save questionnaire response for form ID: {}", submissionRequest.getFormDefinitionId());

		// 1. Validate FormDefinition exists and is of type QUESTIONNAIRE
		FormDefinition formDef = formDefinitionRepository.findById(submissionRequest.getFormDefinitionId())
			.orElseThrow(() -> {
				log.warn("FormDefinition not found for ID: {}", submissionRequest.getFormDefinitionId());
				return new ResourceNotFoundException("FormDefinition", "id", submissionRequest.getFormDefinitionId());
			});

		// 2. Convert responseData Map to JsonNode
		JsonNode responseDataJson = objectMapper.convertValue(submissionRequest.getResponseData(), JsonNode.class);

		// 3. Create and save QuestionnaireResponse entity
		QuestionnaireResult response = new QuestionnaireResult();
		response.setFormDefinitionId(submissionRequest.getFormDefinitionId());
		response.setResponseData(responseDataJson);

		// Optionally set userId if provided and your application logic requires it
		if (submissionRequest.getUserId() != null && !submissionRequest.getUserId().isBlank()) {
			response.setUserId(submissionRequest.getUserId());
		}
		response.setUserId("default-user");
		// Note: createdAt and updatedAt are typically handled by BaseAuditableEntity

		QuestionnaireResult savedResponse = questionnaireResponseRepository.save(response);
		log.info("Successfully saved questionnaire response with ID: {} for form ID: {}", savedResponse.getId(),
				submissionRequest.getFormDefinitionId());
		return savedResponse;
	}

}