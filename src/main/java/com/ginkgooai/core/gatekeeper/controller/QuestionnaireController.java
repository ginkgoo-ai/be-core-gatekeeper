package com.ginkgooai.core.gatekeeper.controller;

import com.ginkgooai.core.gatekeeper.dto.request.QuestionnaireSubmissionRequest;
import com.ginkgooai.core.gatekeeper.domain.QuestionnaireResult;
import com.ginkgooai.core.gatekeeper.service.QuestionnaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.ginkgooai.core.gatekeeper.dto.FormDefinitionDTO;
import com.ginkgooai.core.gatekeeper.service.FormDefinitionService;
import com.ginkgooai.core.gatekeeper.repository.QuestionnaireResponseRepository;
import com.ginkgooai.core.gatekeeper.exception.ResourceNotFoundException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequiredArgsConstructor
@Slf4j
// Consider a base path like @RequestMapping("/questionnaires") if all questionnaire
// related endpoints start with it.
public class QuestionnaireController {

	private final QuestionnaireService questionnaireService;

	private final FormDefinitionService formDefinitionService;

	private final QuestionnaireResponseRepository questionnaireResponseRepository;

	private final ObjectMapper objectMapper;

	/**
	 * Displays the questionnaire filling page. The formId is passed via query parameter
	 * and added to the model so it can be used by the fill_questionnaire.html JavaScript
	 * to fetch the form definition.
	 * @param formId The ID of the form definition to render.
	 * @param model The Spring MVC model.
	 * @return The path to the questionnaire fill template.
	 */
	@GetMapping("/questionnaires/fill")
	public String showQuestionnaireFillPage(@RequestParam("formId") String formId, Model model) {
		log.info("Request to show questionnaire fill page for formId: {}", formId);
		// We don't fetch the form definition here directly. The HTML page's JavaScript
		// will call /api/forms/render/{formId} to get the form structure.
		// We just need to pass the formId to the page.
		model.addAttribute("formId", formId);
		// The JavaScript in fill_questionnaire.html expects to pick up 'formId' from URL
		// params,
		// but having it in the model is also good practice if Thymeleaf needs it
		// directly.
		return "questionnaire/dynamic_form_renderer"; // Path to your Thymeleaf template
	}

	/**
	 * Handles the submission of a questionnaire.
	 * @param submissionRequest The DTO containing the form ID and response data.
	 * @return ResponseEntity with the saved QuestionnaireResponse or an error.
	 */
	@PostMapping("/api/questionnaire/submit")
	@ResponseBody // Or use @RestController at class level if all methods return data
	public ResponseEntity<?> submitQuestionnaire(@Valid @RequestBody QuestionnaireSubmissionRequest submissionRequest) {
		log.info("Received questionnaire submission for form ID: {}", submissionRequest.getFormDefinitionId());
		try {
			QuestionnaireResult savedResponse = questionnaireService.saveResponse(submissionRequest);
			// Return a simple success message or the created resource representation
			return ResponseEntity.status(HttpStatus.CREATED).body(savedResponse);
		}
		catch (IllegalArgumentException e) {
			log.warn("Invalid submission for form ID {}: {}", submissionRequest.getFormDefinitionId(), e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		catch (Exception e) {
			log.error("Error submitting questionnaire for form ID {}: {}", submissionRequest.getFormDefinitionId(),
					e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
		}
	}

	@GetMapping("/admin/questionnaires/{formId}/results")
	public String showQuestionnaireResultsPage(@PathVariable String formId, Model model) {
		log.info("Request to show results for questionnaire formId: {}", formId);
		try {
			FormDefinitionDTO formDefinition = formDefinitionService.findFormDefinitionById(formId)
				.orElseThrow(() -> new ResourceNotFoundException("FormDefinition", "id", formId));

			if (formDefinition.getFormType() != com.ginkgooai.core.gatekeeper.enums.FormType.QUESTIONNAIRE) {
				log.warn("Attempted to view results for a non-questionnaire form: {} (Type: {})", formId,
						formDefinition.getFormType());
				// Optionally, redirect to an error page or show a message
				model.addAttribute("errorMessage",
						"This form is not a questionnaire, so results cannot be displayed in this view.");
				// return "error-page"; // Or a generic error view
			}

			List<QuestionnaireResult> results = questionnaireResponseRepository.findByFormDefinitionId(formId);

			model.addAttribute("formDefinition", formDefinition);
			model.addAttribute("questionnaireResults", results);
			model.addAttribute("objectMapper", objectMapper); // To use for pretty
																// printing JSON in
																// Thymeleaf

			return "admin/questionnaire-results";
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
			// return "error-page"; // Or a generic error view
			return "redirect:/admin/forms?error=servererror";
		}
	}

}