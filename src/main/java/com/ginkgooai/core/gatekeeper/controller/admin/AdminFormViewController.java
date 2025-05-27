package com.ginkgooai.core.gatekeeper.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.domain.QuestionnaireResult;
import com.ginkgooai.core.gatekeeper.dto.*;
import com.ginkgooai.core.gatekeeper.dto.request.CreateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateFieldDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateSectionDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateValidationRuleRequest;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import com.ginkgooai.core.gatekeeper.repository.QuestionnaireResponseRepository;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/forms-ui")
public class AdminFormViewController {

	private static final Logger log = LoggerFactory.getLogger(AdminFormViewController.class);

	private final FormDefinitionService formDefinitionService;

	private final ObjectMapper objectMapper;

	private final QuestionnaireResponseRepository questionnaireResponseRepository;

	@Autowired
	public AdminFormViewController(FormDefinitionService formDefinitionService,
			QuestionnaireResponseRepository questionnaireResponseRepository, ObjectMapper objectMapper) {
		this.formDefinitionService = formDefinitionService;
		this.questionnaireResponseRepository = questionnaireResponseRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping
	public String listForms(Model model, @RequestParam(required = false) String name,
			@RequestParam(required = false) FormDefinition.FormStatus status,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		log.debug("Admin request to list forms. Name: {}, Status: {}, Pageable: {}", name, status, pageable);
		Page<FormDefinitionDTO> formPage = formDefinitionService.findFormDefinitions(pageable, name, status);
		model.addAttribute("formPage", formPage);
		model.addAttribute("formStatuses", FormDefinition.FormStatus.values());
		model.addAttribute("currentName", name);
		model.addAttribute("currentStatus", status);
		return "admin/forms-list-buffer"; // Thymeleaf template name
	}

	@GetMapping("/new")
	public String showCreateFormPage(Model model) {
		log.debug("Admin request to show create new form page.");
		model.addAttribute("createRequest", new CreateFormDefinitionRequest());

		addEnumTypesToModel(model);

		return "admin/form-create-buffer"; // Thymeleaf template name
	}

	/**
	 * 预验证JSON格式的表单定义数据，并返回验证结果 用于前端JSON导入功能
	 */
	@PostMapping("/validate-json")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> validateJsonForm(@RequestBody String jsonContent) {
		Map<String, Object> response = new HashMap<>();

		try {
			CreateFormDefinitionRequest formRequest = objectMapper.readValue(jsonContent,
					CreateFormDefinitionRequest.class);

			boolean isValid = formRequest.getName() != null && !formRequest.getName().isEmpty();

			if (isValid) {
				response.put("valid", true);
				response.put("message", "JSON格式有效");

				Map<String, Object> preview = new HashMap<>();
				preview.put("name", formRequest.getName());
				preview.put("sectionsCount", formRequest.getSections() != null ? formRequest.getSections().size() : 0);
				int fieldsCount = 0;
				if (formRequest.getSections() != null) {
					for (var section : formRequest.getSections()) {
						if (section.getFields() != null) {
							fieldsCount += section.getFields().size();
						}
					}
				}
				preview.put("fieldsCount", fieldsCount);
				response.put("preview", preview);
			}
			else {
				response.put("valid", false);
				response.put("message", "JSON结构不完整，表单必须至少包含名称");
			}
		}
		catch (JsonProcessingException e) {
			log.warn("Invalid JSON format: {}", e.getMessage());
			response.put("valid", false);
			response.put("message", "无效的JSON格式: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Error validating form JSON: {}", e.getMessage(), e);
			response.put("valid", false);
			response.put("message", "验证过程中发生错误: " + e.getMessage());
		}

		return ResponseEntity.ok(response);
	}

	@GetMapping("/preview/{formId}") // Or just {formId}/preview
	public String previewForm(@PathVariable String formId, Model model, RedirectAttributes redirectAttributes) {
		log.debug("Admin request to preview form with ID: {}", formId);
		try {
			FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(formId)
				.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", formId));
			addEnumTypesToModel(model);
			model.addAttribute("formId", formId);
			model.addAttribute("viewMode", "preview");

			return "dynamic_form_renderer";
		}
		catch (ResourceNotFoundException e) {
			log.warn("Form definition not found for preview with ID: {}. Redirecting.", formId, e);
			redirectAttributes.addFlashAttribute("errorMessage", "Form definition with ID " + formId + " not found.");
			return "redirect:/admin/forms";
		}
	}

	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
		log.debug("Admin request to edit form with ID: {}", id);
		try {
			return populateFormEditModel(id, model, null);
		}
		catch (ResourceNotFoundException e) {
			log.warn("Form definition not found for editing with ID: {}. Redirecting.", id, e);
			redirectAttributes.addFlashAttribute("errorMessage", "Form definition with ID " + id + " not found.");
			return "redirect:/admin/forms";
		}
	}

	@PostMapping("/edit/{id}")
	public String updateForm(@PathVariable String id,
			@Valid @ModelAttribute("updateRequest") UpdateFormDefinitionRequest request, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		log.info("Admin request to update form with ID: {}", id);

		if (bindingResult.hasErrors()) {
			log.warn("Validation errors when updating form: {}", bindingResult.getAllErrors());
			// Populate model for returning to edit page with validation errors
			try {
				populateFormEditModel(id, model, request); // Pass existing request to
															// retain user input
				return "admin/form-edit-buffer"; // Return to edit page with errors
			}
			catch (ResourceNotFoundException e) {
				log.warn(
						"Form definition not found (ID: {}) while trying to show edit page after validation errors. Redirecting.",
						id, e);
				// It's unlikely to hit this if the form existed for the initial GET,
				// but handling defensively.
				redirectAttributes.addFlashAttribute("errorMessage", "Form definition with ID " + id + " not found.");
				return "redirect:/admin/forms";
			}
		}

		try {
			formDefinitionService.updateFormDefinition(id, request);
			log.info("Successfully updated form with ID: {}", id);
			return "redirect:/admin/forms?success=updated";
		}
		catch (Exception e) {
			log.error("Error updating form: {}", e.getMessage(), e);
			model.addAttribute("errorMessage", "Error updating form: " + e.getMessage());

			// Populate model for returning to edit page after an exception
			try {
				populateFormEditModel(id, model, request); // Pass existing request
				return "admin/form-edit-buffer"; // Return to edit page
			}
			catch (ResourceNotFoundException rnfe) {
				log.warn(
						"Form definition not found (ID: {}) while trying to show edit page after update exception. Redirecting.",
						id, rnfe);
				redirectAttributes.addFlashAttribute("errorMessage",
						"Form definition with ID " + id + " not found. Update failed.");
				return "redirect:/admin/forms";
			}
		}
	}

	@PostMapping
	public String createForm(@Valid @ModelAttribute("createRequest") CreateFormDefinitionRequest request,
			BindingResult bindingResult, Model model) {
		log.info("Admin request to create new form: {}", request);
		if (bindingResult.hasErrors()) {
			log.warn("Validation errors when creating form: {}", bindingResult.getAllErrors());
			// Return to the form with errors

			// 添加枚举类型到model中
			addEnumTypesToModel(model);

			return "admin/form-create-buffer";
		}

		try {
			// The JSON validation try-catch block for initialLogic and submissionLogic
			// will
			// be removed.
			// The fields in 'request' are already JsonNode if binding was successful.

			FormDefinitionDTO createdForm = formDefinitionService.createFormDefinition(request);
			log.info("Successfully created form: {}", createdForm);
			// Optionally add a success message via RedirectAttributes
			return "redirect:/admin/forms"; // Redirect to the list page
		}
		catch (Exception e) {
			log.error("Error creating form: {}", e.getMessage(), e);
			model.addAttribute("errorMessage", "Error creating form: " + e.getMessage());
			// Ensure createRequest is still available in the model if returning to form
			if (!model.containsAttribute("createRequest")) {
				model.addAttribute("createRequest", request);
			}

			addEnumTypesToModel(model);

			return "admin/form-create-buffer";
		}
	}

	@PostMapping("/delete/{id}")
	public String deleteForm(@PathVariable String id, RedirectAttributes redirectAttributes) {
		log.info("Admin request to delete form with ID: {}", id);
		try {
			formDefinitionService.deleteFormDefinition(id);
			log.info("Successfully deleted form with ID: {}", id);
			redirectAttributes.addFlashAttribute("successMessage",
					"Form definition with ID " + id + " deleted successfully.");
		}
		catch (ResourceNotFoundException e) {
			log.warn("Attempted to delete non-existent form with ID: {}", id);
			redirectAttributes.addFlashAttribute("errorMessage", "Form definition with ID " + id + " not found.");
		}
		catch (RuntimeException e) { // Catch runtime exceptions from the service layer
			log.error("Error deleting form with ID {}: {}", id, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/admin/forms";
	}

	@PostMapping("/{id}/status")
	public String updateFormStatus(@PathVariable String id, @RequestParam("status") FormDefinition.FormStatus newStatus,
			RedirectAttributes redirectAttributes) {
		log.info("Admin request to update status of form ID: {} to {}", id, newStatus);
		try {
			formDefinitionService.updateFormStatus(id, newStatus);
			redirectAttributes.addFlashAttribute("successMessage",
					"Status for form " + id + " updated to " + newStatus + " successfully.");
		}
		catch (ResourceNotFoundException e) {
			log.warn("Form definition not found for status update: {}", id);
			redirectAttributes.addFlashAttribute("errorMessage", "Form definition with ID " + id + " not found.");
		}
		catch (Exception e) {
			log.error("Error updating status for form ID {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
		}
		return "redirect:/admin/forms";
	}
	/**
	 * Populates the model with form definition details for the edit view. This method
	 * fetches the form definition, sets up the update request object if not provided, and
	 * adds necessary attributes (form definition, update request, statuses, enum types)
	 * to the model.
	 * @param id The ID of the form definition to fetch.
	 * @param model The Spring MVC model to populate.
	 * @param existingUpdateRequest An optional existing UpdateFormDefinitionRequest
	 * (e.g., from a failed submission). If null, a new one will be created based on the
	 * fetched form definition.
	 * @return The view name to render ("admin/form-edit-buffer" on success).
	 * @throws ResourceNotFoundException if the form definition with the given ID is not
	 * found.
	 */
	private String populateFormEditModel(String id, Model model, UpdateFormDefinitionRequest existingUpdateRequest)
			throws ResourceNotFoundException {
		FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(id).orElseThrow(() -> {
			// Log here as this is the point of origin for this specific failure context
			log.warn("Form definition not found for ID: {} in populateFormEditModel", id);
			return new ResourceNotFoundException("FormDefinition", "id", id);
		});

		UpdateFormDefinitionRequest updateRequest;
		if (existingUpdateRequest != null) {
			updateRequest = existingUpdateRequest;
		}
		else {
			updateRequest = new UpdateFormDefinitionRequest();
			// Populate updateRequest from formDefinition
			if (formDefinition.getDescription() != null) {
				updateRequest.setDescription(formDefinition.getDescription());
			}
			if (formDefinition.getStatus() != null) {
				updateRequest.setStatus(formDefinition.getStatus());
			}
			if (formDefinition.getTargetAudience() != null) {
				updateRequest.setTargetAudience(formDefinition.getTargetAudience());
			}
			if (formDefinition.getInitialLogic() != null) {
				updateRequest.setInitialLogic(formDefinition.getInitialLogic());
			}
			if (formDefinition.getSubmissionLogic() != null) {
				updateRequest.setSubmissionLogic(formDefinition.getSubmissionLogic());
			}
			if (formDefinition.getSections() != null && !formDefinition.getSections().isEmpty()) {
				List<UpdateSectionDefinitionRequest> sectionRequests = new ArrayList<>();
				for (SectionDefinitionDTO sectionDTO : formDefinition.getSections()) {
					UpdateSectionDefinitionRequest sectionRequest = new UpdateSectionDefinitionRequest();
					sectionRequest.setId(sectionDTO.getId());
					sectionRequest.setTitle(sectionDTO.getTitle());
					sectionRequest.setOrder(sectionDTO.getOrder());
					sectionRequest.setCondition(sectionDTO.getCondition());

					if (sectionDTO.getFields() != null && !sectionDTO.getFields().isEmpty()) {
						List<UpdateFieldDefinitionRequest> fieldRequests = new ArrayList<>();
						for (FieldDefinitionDTO fieldDTO : sectionDTO.getFields()) {
							UpdateFieldDefinitionRequest fieldRequest = new UpdateFieldDefinitionRequest();
							fieldRequest.setId(fieldDTO.getId());
							fieldRequest.setName(fieldDTO.getName());
							fieldRequest.setLabel(fieldDTO.getLabel());
							fieldRequest.setFieldType(fieldDTO.getFieldType());
							fieldRequest.setPlaceholder(fieldDTO.getPlaceholder());
							fieldRequest.setDefaultValue(fieldDTO.getDefaultValue());
							fieldRequest.setOptionsSourceType(fieldDTO.getOptionsSourceType());
							fieldRequest.setStaticOptions(fieldDTO.getStaticOptions());
							fieldRequest.setApiEndpoint(fieldDTO.getApiEndpoint());
							fieldRequest.setUiProperties(fieldDTO.getUiProperties());
							fieldRequest.setCondition(fieldDTO.getCondition());
							fieldRequest.setDependencies(fieldDTO.getDependencies());
							fieldRequest.setOrder(fieldDTO.getOrder());

							if (fieldDTO.getValidations() != null && !fieldDTO.getValidations().isEmpty()) {
								List<UpdateValidationRuleRequest> ruleRequests = new ArrayList<>();
								for (ValidationRuleDTO ruleDTO : fieldDTO.getValidations()) {
									UpdateValidationRuleRequest ruleRequest = new UpdateValidationRuleRequest();
									ruleRequest.setId(ruleDTO.getId());
									ruleRequest.setType(ruleDTO.getType());
									ruleRequest.setValue(ruleDTO.getValue());
									ruleRequest.setErrorMessage(ruleDTO.getErrorMessage());
									ruleRequest.setCustomFunction(ruleDTO.getCustomFunction());
									ruleRequests.add(ruleRequest);
								}
								fieldRequest.setValidations(ruleRequests);
							}
							fieldRequests.add(fieldRequest);
						}
						sectionRequest.setFields(fieldRequests);
					}
					sectionRequests.add(sectionRequest);
				}
				updateRequest.setSections(sectionRequests);
			}
		}

		model.addAttribute("formDefinition", formDefinition);
		model.addAttribute("updateRequest", updateRequest);
		model.addAttribute("formStatuses", FormDefinition.FormStatus.values());
		addEnumTypesToModel(model);

		return "admin/form-edit-buffer";
	}

	@GetMapping("/{formId}/results")
	public String showFormResultsPage(@PathVariable String formId, Model model) {
		log.info("Request to show results for questionnaire formId: {}", formId);
		try {
			FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(formId)
				.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", formId));

			if (formDefinition.getFormType() != com.ginkgooai.core.gatekeeper.enums.FormType.QUESTIONNAIRE) {
				log.warn("Attempted to view results for a non-questionnaire form: {} (Type: {})", formId,
						formDefinition.getFormType());
				model.addAttribute("errorMessage",
						"This form is not a questionnaire, so results cannot be displayed in this view.");
			}

			List<QuestionnaireResult> results = questionnaireResponseRepository.findByFormDefinitionId(formId);

			model.addAttribute("formDefinition", formDefinition);
			model.addAttribute("questionnaireResults", results);
			model.addAttribute("objectMapper", objectMapper);
			return "admin/form-results";
		}
		catch (ResourceNotFoundException e) {
			log.warn("Form definition not found when trying to show results: {}", formId);
			model.addAttribute("errorMessage", "Form definition with ID " + formId + " not found.");
			return "redirect:/admin/forms?error=notfound"; // Redirect to form list or an
			// error page
		}
		catch (Exception e) {
			log.error("Error retrieving questionnaire results for formId {}: {}", formId, e.getMessage(), e);
			model.addAttribute("errorMessage", "An unexpected error occurred while retrieving results.");
			return "redirect:/admin/forms?error=servererror";
		}
	}

	/**
	 * Adds commonly used enum types to the Model. This avoids using T() expressions in
	 * Thymeleaf templates, addressing potential issues with Spring Security or template
	 * engine restrictions.
	 * @param model The Spring MVC model to which enum types will be added.
	 */
	private void addEnumTypesToModel(Model model) {
		model.addAttribute("FieldType", com.ginkgooai.core.gatekeeper.domain.types.FieldType.class);
		model.addAttribute("ValidationRuleType", com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType.class);
		model.addAttribute("OptionsSourceType", com.ginkgooai.core.gatekeeper.domain.types.OptionsSourceType.class);
	}

}
