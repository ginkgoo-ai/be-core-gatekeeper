package com.ginkgooai.core.gatekeeper.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ginkgooai.core.gatekeeper.domain.FormDefinition;
import com.ginkgooai.core.gatekeeper.dto.request.CreateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.UpdateFormDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.SectionDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.FieldDefinitionDTO;
import com.ginkgooai.core.gatekeeper.dto.ValidationRuleDTO;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateSectionDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateFieldDefinitionRequest;
import com.ginkgooai.core.gatekeeper.dto.request.UpdateValidationRuleRequest;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import com.ginkgooai.core.gatekeeper.service.FormImageProcessingService;
import com.ginkgooai.core.gatekeeper.util.FormRenderingHelper;
import com.ginkgooai.core.gatekeeper.util.JsonValidationUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/forms")
public class FormAdminController {

	private static final Logger log = LoggerFactory.getLogger(FormAdminController.class);

	private final FormDefinitionService formDefinitionService;

	private final FormImageProcessingService formImageProcessingService;

	private final FormRenderingHelper formRenderingHelper;

	private final ObjectMapper objectMapper;

	@Autowired
	public FormAdminController(FormDefinitionService formDefinitionService,
			FormImageProcessingService formImageProcessingService, FormRenderingHelper formRenderingHelper,
			ObjectMapper objectMapper) {
		this.formDefinitionService = formDefinitionService;
		this.formImageProcessingService = formImageProcessingService;
		this.formRenderingHelper = formRenderingHelper;
		this.objectMapper = objectMapper;
	}

	@GetMapping
	public String listForms(Model model, @RequestParam(required = false) String name,
			@RequestParam(required = false) FormDefinition.FormStatus status,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		log.debug("Admin request to list forms. Name: {}, Status: {}, Pageable: {}", name, status, pageable);
		Page<FormDefinitionDTO> formPage = formDefinitionService.findFormDefinitions(pageable, name, status);
		model.addAttribute("formPage", formPage);
		model.addAttribute("formStatuses", FormDefinition.FormStatus.values()); // For
																				// status
																				// filter
																				// dropdown
		model.addAttribute("currentName", name);
		model.addAttribute("currentStatus", status);
		return "admin/forms-list-buffer"; // Thymeleaf template name
	}

	@GetMapping("/new")
	public String showCreateFormPage(Model model) {
		log.debug("Admin request to show create new form page.");
		model.addAttribute("createRequest", new CreateFormDefinitionRequest());

		// 添加枚举类型到模型中
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
			// 尝试将JSON解析为CreateFormDefinitionRequest对象
			CreateFormDefinitionRequest formRequest = objectMapper.readValue(jsonContent,
					CreateFormDefinitionRequest.class);

			// 验证基本结构是否符合要求
			boolean isValid = formRequest.getName() != null && !formRequest.getName().isEmpty();

			if (isValid) {
				response.put("valid", true);
				response.put("message", "JSON格式有效");

				// 提供表单的基本信息作为预览
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
	public String previewForm(@PathVariable String formId, Model model) {
		log.debug("Admin request to preview form with ID: {}", formId);
		try {
			FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(formId)
				.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", formId));
			addEnumTypesToModel(model);
			model.addAttribute("formId", formId); // Pass for JS (though it already gets
			model.addAttribute("viewMode", "preview"); // Explicitly set mode

			return "questionnaire/dynamic_form_renderer";
		}
		catch (ResourceNotFoundException e) {
			log.warn("Form definition not found for preview with ID: {}", formId);
			// Optionally, redirect to an error page or the list page with an error
			// message
			model.addAttribute("errorMessage", "Form definition with ID " + formId + " not found.");
			return "admin/forms-list-buffer"; // Or a dedicated error view
		}
	}

	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable String id, Model model) {
		log.debug("Admin request to edit form with ID: {}", id);
		try {
			FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(id)
				.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", id));

			// 创建更新请求对象
			UpdateFormDefinitionRequest updateRequest = new UpdateFormDefinitionRequest();

			// 安全地访问字段
			if (formDefinition.getDescription() != null) {
				updateRequest.setDescription(formDefinition.getDescription());
			}

			if (formDefinition.getStatus() != null) {
				updateRequest.setStatus(formDefinition.getStatus());
			}

			// 设置targetAudience
			if (formDefinition.getTargetAudience() != null) {
				updateRequest.setTargetAudience(formDefinition.getTargetAudience());
			}

			// 设置initialLogic
			if (formDefinition.getInitialLogic() != null) {
				updateRequest.setInitialLogic(formDefinition.getInitialLogic());
			}

			// 设置submissionLogic
			if (formDefinition.getSubmissionLogic() != null) {
				updateRequest.setSubmissionLogic(formDefinition.getSubmissionLogic());
			}

			// 设置sections
			if (formDefinition.getSections() != null && !formDefinition.getSections().isEmpty()) {
				List<UpdateSectionDefinitionRequest> sectionRequests = new ArrayList<>();

				for (SectionDefinitionDTO sectionDTO : formDefinition.getSections()) {
					UpdateSectionDefinitionRequest sectionRequest = new UpdateSectionDefinitionRequest();
					sectionRequest.setId(sectionDTO.getId());
					sectionRequest.setTitle(sectionDTO.getTitle());
					sectionRequest.setOrder(sectionDTO.getOrder());
					sectionRequest.setCondition(sectionDTO.getCondition());

					// 设置fields
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

							// 设置validations
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

			model.addAttribute("formDefinition", formDefinition);
			model.addAttribute("updateRequest", updateRequest);
			model.addAttribute("formStatuses", FormDefinition.FormStatus.values());

			// 添加枚举类型到model中，避免在Thymeleaf模板中使用T()引用
			addEnumTypesToModel(model);

			return "admin/form-edit-buffer";
		}
		catch (ResourceNotFoundException e) {
			log.warn("Form definition not found for editing with ID: {}", id);
			model.addAttribute("errorMessage", "Form definition with ID " + id + " not found.");
			return "redirect:/admin/forms?error=notfound";
		}
	}

	@PostMapping("/edit/{id}")
	public String updateForm(@PathVariable String id,
			@Valid @ModelAttribute("updateRequest") UpdateFormDefinitionRequest request, BindingResult bindingResult,
			Model model) {
		log.info("Admin request to update form with ID: {}", id);

		if (bindingResult.hasErrors()) {
			log.warn("Validation errors when updating form: {}", bindingResult.getAllErrors());
			// 获取当前表单定义重新显示编辑页面
			try {
				FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(id)
					.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", id));
				model.addAttribute("formDefinition", formDefinition);
				model.addAttribute("formStatuses", FormDefinition.FormStatus.values());

				// 添加枚举类型到model中
				addEnumTypesToModel(model);

			}
			catch (ResourceNotFoundException e) {
				model.addAttribute("errorMessage", "Form definition with ID " + id + " not found.");
				return "redirect:/admin/forms?error=notfound";
			}
			return "admin/form-edit-buffer";
		}

		try {
			// The JSON validation try-catch block for initialLogic and submissionLogic
			// will
			// be removed.
			// The fields in 'request' are already JsonNode if binding was successful.

			formDefinitionService.updateFormDefinition(id, request);
			log.info("Successfully updated form with ID: {}", id);
			return "redirect:/admin/forms?success=updated";
		}
		catch (Exception e) {
			log.error("Error updating form: {}", e.getMessage(), e);
			model.addAttribute("errorMessage", "Error updating form: " + e.getMessage());

			// 重新获取表单定义重新显示编辑页面
			try {
				FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(id)
					.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", id));
				model.addAttribute("formDefinition", formDefinition);
				model.addAttribute("formStatuses", FormDefinition.FormStatus.values());

				// 添加枚举类型到model中
				addEnumTypesToModel(model);

			}
			catch (ResourceNotFoundException ex) {
				return "redirect:/admin/forms?error=notfound";
			}

			return "admin/form-edit-buffer";
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

			// 添加枚举类型到model中
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

	@PostMapping(value = "/import-image", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody // Important: This endpoint returns JSON directly
	public ResponseEntity<?> handleImageImport(@RequestParam("imageFile") MultipartFile imageFile) {
		if (imageFile.isEmpty()) {
			return ResponseEntity.badRequest().body("{\"error\": \"Image file is empty\"}");
		}

		try {
			log.info("Processing image import: {}", imageFile.getOriginalFilename());
			String generatedJson = formImageProcessingService.processImageToFormJson(imageFile);
			log.info("Generated JSON from image: {}", generatedJson);
			// The client-side JS expects a JSON object or a string that is JSON.
			// We return the raw string from the LLM, assuming it's valid JSON.
			return ResponseEntity.ok(generatedJson);
		}
		catch (IOException e) {
			log.error("IO Error processing image: {}", imageFile.getOriginalFilename(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"error\": \"IO error processing image: " + e.getMessage() + "\"}");
		}
		catch (Exception e) {
			log.error("Error processing image with AI: {}", imageFile.getOriginalFilename(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"error\": \"Failed to process image with AI: " + e.getMessage() + "\"}");
		}
	}

	/**
	 * 添加常用枚举类型到Model中，避免在Thymeleaf模板中使用T()引用， 解决"Instantiation of new objects and access
	 * to static classes or parameters is forbidden in this context"错误
	 */
	private void addEnumTypesToModel(Model model) {
		model.addAttribute("FieldType", com.ginkgooai.core.gatekeeper.domain.types.FieldType.class);
		model.addAttribute("ValidationRuleType", com.ginkgooai.core.gatekeeper.domain.types.ValidationRuleType.class);
		model.addAttribute("OptionsSourceType", com.ginkgooai.core.gatekeeper.domain.types.OptionsSourceType.class);
	}

}
