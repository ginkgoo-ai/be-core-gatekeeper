package com.ginkgooai.core.gatekeeper.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.*;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
// Import new request DTOs
import com.ginkgooai.core.gatekeeper.dto.request.CreateFieldDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.CreateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.CreateSectionDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.CreateValidationRuleRequest;
import com.ginkgooai.core.gatekeeper.dto.UpdateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateSectionDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateFieldDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateValidationRuleRequest;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import com.ginkgooai.core.gatekeeper.repository.FormDefinitionRepository;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Slf4j
public class FormDefinitionServiceImpl implements FormDefinitionService {

	private final FormDefinitionRepository formDefinitionRepository;

	private final ObjectMapper objectMapper;

	@Autowired
	public FormDefinitionServiceImpl(FormDefinitionRepository formDefinitionRepository, ObjectMapper objectMapper) {
		this.formDefinitionRepository = formDefinitionRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public FormDefinitionDTO createFormDefinition(CreateFormDefinitionRequest request) {
		log.info("Creating new form definition with name: {}", request.getName());

		formDefinitionRepository.findByName(request.getName()).ifPresent(fd -> {
			log.warn("Attempted to create form definition with duplicate name: {}", request.getName());
			throw new DataIntegrityViolationException("Form definition name must be unique: " + request.getName());
		});

		FormDefinition newFormDef = new FormDefinition();
		newFormDef.setName(request.getName());
		newFormDef.setVersion(StringUtils.hasText(request.getVersion()) ? request.getVersion() : "1.0.0");
		newFormDef.setDescription(request.getDescription());
		newFormDef.setTargetAudience(request.getTargetAudience());
		newFormDef.setInitialLogic(request.getInitialLogic());
		newFormDef.setSubmissionLogic(request.getSubmissionLogic());

		if (request.getSections() != null) {
			for (CreateSectionDefinitionRequest sectionRequest : request.getSections()) {
				SectionDefinition sectionDef = new SectionDefinition();
				sectionDef.setTitle(sectionRequest.getTitle());
				sectionDef.setOrder(sectionRequest.getOrder());
				sectionDef.setCondition(sectionRequest.getCondition());

				if (sectionRequest.getFields() != null) {
					for (CreateFieldDefinitionRequest fieldRequest : sectionRequest.getFields()) {
						FieldDefinition fieldDef = new FieldDefinition();
						fieldDef.setName(fieldRequest.getName());
						fieldDef.setLabel(fieldRequest.getLabel());
						fieldDef.setFieldType(fieldRequest.getFieldType());
						fieldDef.setPlaceholder(fieldRequest.getPlaceholder());
						fieldDef.setDefaultValue(fieldRequest.getDefaultValue());
						fieldDef.setOptionsSourceType(fieldRequest.getOptionsSourceType());

						setJsonNodeField(fieldDef, "staticOptions", fieldRequest.getStaticOptions());

						fieldDef.setApiEndpoint(fieldRequest.getApiEndpoint());

						setJsonNodeField(fieldDef, "uiProperties", fieldRequest.getUiProperties());
						setJsonNodeField(fieldDef, "dependencies", fieldRequest.getDependencies());

						fieldDef.setCondition(fieldRequest.getCondition());
						fieldDef.setOrder(fieldRequest.getOrder());

						if (fieldRequest.getValidations() != null) {
							for (CreateValidationRuleRequest ruleRequest : fieldRequest.getValidations()) {
								ValidationRule rule = new ValidationRule();
								rule.setType(ruleRequest.getType());
								rule.setValue(ruleRequest.getValue());
								rule.setErrorMessage(ruleRequest.getErrorMessage());
								rule.setCustomFunction(ruleRequest.getCustomFunction());
								fieldDef.addValidationRule(rule);
							}
						}
						sectionDef.addField(fieldDef);
					}
				}
				newFormDef.addSection(sectionDef);
			}
		}

		FormDefinition savedFormDef = formDefinitionRepository.save(newFormDef);
		log.info("Successfully created form definition with ID: {}", savedFormDef.getId());
		return FormDefinitionDTO.fromEntity(savedFormDef);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<FormDefinitionDTO> findFormDefinitionById(String id) {
		log.debug("Finding form definition by ID: {}", id);
		return formDefinitionRepository.findById(id).map(FormDefinitionDTO::fromEntity);
	}

	@Override
	@Transactional
	public FormDefinitionDTO updateFormDefinition(String id, UpdateFormDefinitionRequest request) {
		log.info("Updating form definition with ID: {}", id);

		FormDefinition existingFormDef = formDefinitionRepository.findById(id).orElseThrow(() -> {
			log.warn("Form definition not found for update with ID: {}", id);
			return new ResourceNotFoundException("FormDefinition", "id", id);
		});

		// 更新基本字段
		existingFormDef.setDescription(request.getDescription());
		existingFormDef.setStatus(request.getStatus());
		existingFormDef.setTargetAudience(request.getTargetAudience());

		// 处理JSON字段，确保它们有效
		validateAndSetJsonFields(existingFormDef, request.getInitialLogic(), request.getSubmissionLogic());

		// 处理sections的更新，采用Map来追踪现有section
		Map<String, SectionDefinition> existingSectionsMap = existingFormDef.getSections()
			.stream()
			.collect(Collectors.toMap(SectionDefinition::getId, section -> section));

		// 清空现有sections列表，我们将使用新的列表重新填充
		existingFormDef.getSections().clear();

		// 处理请求中的sections
		if (request.getSections() != null) {
			for (UpdateSectionDefinitionRequest sectionRequest : request.getSections()) {
				SectionDefinition sectionDef;

				// 如果section有ID，尝试查找现有section
				if (sectionRequest.getId() != null && existingSectionsMap.containsKey(sectionRequest.getId())) {
					// 更新现有section
					sectionDef = existingSectionsMap.get(sectionRequest.getId());
					sectionDef.setTitle(sectionRequest.getTitle());
					sectionDef.setOrder(sectionRequest.getOrder());
					sectionDef.setCondition(sectionRequest.getCondition());

					// 清空现有fields列表，我们将使用新的列表重新填充
					Map<String, FieldDefinition> existingFieldsMap = sectionDef.getFields()
						.stream()
						.collect(Collectors.toMap(FieldDefinition::getId, field -> field));
					sectionDef.getFields().clear();

					// 处理fields
					if (sectionRequest.getFields() != null) {
						processFields(sectionDef, sectionRequest.getFields(), existingFieldsMap);
					}
				}
				else {
					// 创建新的section
					sectionDef = new SectionDefinition();
					sectionDef.setTitle(sectionRequest.getTitle());
					sectionDef.setOrder(sectionRequest.getOrder());
					sectionDef.setCondition(sectionRequest.getCondition());

					// 处理fields
					if (sectionRequest.getFields() != null) {
						processFields(sectionDef, sectionRequest.getFields(), new HashMap<>());
					}
				}

				// 添加到form
				existingFormDef.addSection(sectionDef);
			}
		}

		FormDefinition updatedFormDef = formDefinitionRepository.save(existingFormDef);
		log.info("Successfully updated form definition with ID: {}", updatedFormDef.getId());
		return FormDefinitionDTO.fromEntity(updatedFormDef);
	}

	/**
	 * 验证并设置JSON字段
	 */
	private void validateAndSetJsonFields(FormDefinition formDef, JsonNode initialLogic, JsonNode submissionLogic) {
		// 处理initialLogic字段
		if (initialLogic != null && !initialLogic.isNull()) {
			formDef.setInitialLogic(initialLogic);
		}
		else {
			formDef.setInitialLogic(null);
		}

		// 处理submissionLogic字段
		if (submissionLogic != null && !submissionLogic.isNull()) {
			formDef.setSubmissionLogic(submissionLogic);
		}
		else {
			formDef.setSubmissionLogic(null);
		}
	}

	/**
	 * Sets a JsonNode field on a target object using reflection.
	 */
	private void setJsonNodeField(Object target, String fieldName, JsonNode jsonNode) {
		if (jsonNode != null && !jsonNode.isNull()) {
			try {
				java.lang.reflect.Method setter = target.getClass()
					.getMethod("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1),
							JsonNode.class);
				setter.invoke(target, jsonNode);
			}
			catch (Exception e) {
				log.error("Error setting JsonNode field {} on {}: {}", fieldName, target.getClass().getSimpleName(),
						e.getMessage());
				throw new IllegalStateException("Could not set JsonNode field: " + fieldName, e);
			}
		}
		else {
			// If jsonNode is null or represents JSON null, set the field to null.
			try {
				java.lang.reflect.Method setter = target.getClass()
					.getMethod("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1),
							JsonNode.class);
				setter.invoke(target, (JsonNode) null);
			}
			catch (Exception e) {
				log.error("Error setting JsonNode field {} to null on {}: {}", fieldName,
						target.getClass().getSimpleName(), e.getMessage());
				// Decide if this should throw an exception or if setting to null is
				// acceptable
				// silently.
			}
		}
	}

	/**
	 * 处理字段(fields)的更新或创建
	 */
	private void processFields(SectionDefinition sectionDef, List<UpdateFieldDefinitionRequest> fieldRequests,
			Map<String, FieldDefinition> existingFieldsMap) {
		for (UpdateFieldDefinitionRequest fieldRequest : fieldRequests) {
			FieldDefinition fieldDef;

			if (fieldRequest.getId() != null && existingFieldsMap.containsKey(fieldRequest.getId())) {
				fieldDef = existingFieldsMap.get(fieldRequest.getId());
				updateExistingField(fieldDef, fieldRequest);

				Map<String, ValidationRule> existingRulesMap = fieldDef.getValidations()
					.stream()
					.collect(Collectors.toMap(ValidationRule::getId, rule -> rule));
				fieldDef.getValidations().clear();

				if (fieldRequest.getValidations() != null) {
					processValidationRules(fieldDef, fieldRequest.getValidations(), existingRulesMap);
				}
			}
			else {
				fieldDef = new FieldDefinition();
				fieldDef.setName(fieldRequest.getName());
				fieldDef.setLabel(fieldRequest.getLabel());
				fieldDef.setFieldType(fieldRequest.getFieldType());
				fieldDef.setPlaceholder(fieldRequest.getPlaceholder());
				fieldDef.setDefaultValue(fieldRequest.getDefaultValue());
				fieldDef.setOptionsSourceType(fieldRequest.getOptionsSourceType());

				setJsonNodeField(fieldDef, "staticOptions", fieldRequest.getStaticOptions());

				fieldDef.setApiEndpoint(fieldRequest.getApiEndpoint());

				setJsonNodeField(fieldDef, "uiProperties", fieldRequest.getUiProperties());
				setJsonNodeField(fieldDef, "dependencies", fieldRequest.getDependencies());

				fieldDef.setCondition(fieldRequest.getCondition());
				fieldDef.setOrder(fieldRequest.getOrder());

				if (fieldRequest.getValidations() != null) {
					for (UpdateValidationRuleRequest ruleRequest : fieldRequest.getValidations()) {
						ValidationRule rule = new ValidationRule();
						rule.setType(ruleRequest.getType());
						rule.setValue(ruleRequest.getValue());
						rule.setErrorMessage(ruleRequest.getErrorMessage());
						rule.setCustomFunction(ruleRequest.getCustomFunction());
						fieldDef.addValidationRule(rule);
					}
				}
			}
			sectionDef.addField(fieldDef);
		}
	}

	/**
	 * 更新现有字段
	 */
	private void updateExistingField(FieldDefinition fieldDef, UpdateFieldDefinitionRequest fieldRequest) {
		fieldDef.setName(fieldRequest.getName());
		fieldDef.setLabel(fieldRequest.getLabel());
		fieldDef.setFieldType(fieldRequest.getFieldType());
		fieldDef.setPlaceholder(fieldRequest.getPlaceholder());
		fieldDef.setDefaultValue(fieldRequest.getDefaultValue());
		fieldDef.setOptionsSourceType(fieldRequest.getOptionsSourceType());

		setJsonNodeField(fieldDef, "staticOptions", fieldRequest.getStaticOptions());

		fieldDef.setApiEndpoint(fieldRequest.getApiEndpoint());

		setJsonNodeField(fieldDef, "uiProperties", fieldRequest.getUiProperties());
		setJsonNodeField(fieldDef, "dependencies", fieldRequest.getDependencies());

		fieldDef.setCondition(fieldRequest.getCondition());
		fieldDef.setOrder(fieldRequest.getOrder());
	}

	/**
	 * 处理验证规则的更新或创建
	 */
	private void processValidationRules(FieldDefinition fieldDef, List<UpdateValidationRuleRequest> ruleRequests,
			Map<String, ValidationRule> existingRulesMap) {
		for (UpdateValidationRuleRequest ruleRequest : ruleRequests) {
			ValidationRule rule;
			if (ruleRequest.getId() != null && existingRulesMap.containsKey(ruleRequest.getId())) {
				rule = existingRulesMap.get(ruleRequest.getId());
			}
			else {
				rule = new ValidationRule();
			}
			rule.setType(ruleRequest.getType());
			rule.setValue(ruleRequest.getValue());
			rule.setErrorMessage(ruleRequest.getErrorMessage());
			rule.setCustomFunction(ruleRequest.getCustomFunction());
			fieldDef.addValidationRule(rule);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<FormDefinitionDTO> findFormDefinitions(Pageable pageable, String name,
			FormDefinition.FormStatus status) {
		log.debug("Finding form definitions with criteria - name: {}, status: {}", name, status);
		Specification<FormDefinition> spec = buildSearchSpecification(name, status);
		return formDefinitionRepository.findAll(spec, pageable).map(FormDefinitionDTO::fromEntity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FormDefinitionDTO> findAllPublishedForms() {
		log.debug("Finding all published forms");
		return formDefinitionRepository.findByStatus(FormDefinition.FormStatus.PUBLISHED)
			.stream()
			.map(FormDefinitionDTO::fromEntity)
			.collect(Collectors.toList());
	}

	private Specification<FormDefinition> buildSearchSpecification(String name, FormDefinition.FormStatus status) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (StringUtils.hasText(name)) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
			}

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	@Override
	@Transactional
	public void deleteFormDefinition(String id) {
		log.info("Deleting form definition with ID: {}", id);
		if (!formDefinitionRepository.existsById(id)) {
			log.warn("Form definition not found for deletion with ID: {}", id);
			throw new ResourceNotFoundException("FormDefinition", "id", id);
		}
		formDefinitionRepository.deleteById(id);
		log.info("Successfully deleted form definition with ID: {}", id);
	}

	@Override
	@Transactional
	public FormDefinitionDTO updateFormStatus(String formId, FormDefinition.FormStatus newStatus) {
		log.info("Updating status for form definition ID: {} to {}", formId, newStatus);
		FormDefinition formDefinition = formDefinitionRepository.findById(formId).orElseThrow(() -> {
			log.warn("Form definition not found for status update with ID: {}", formId);
			return new ResourceNotFoundException("FormDefinition", "id", formId);
		});

		formDefinition.setStatus(newStatus);
		FormDefinition updatedFormDef = formDefinitionRepository.save(formDefinition);
		log.info("Successfully updated status for form definition ID: {} to {}", updatedFormDef.getId(),
				updatedFormDef.getStatus());
		return FormDefinitionDTO.fromEntity(updatedFormDef);
	}

}
