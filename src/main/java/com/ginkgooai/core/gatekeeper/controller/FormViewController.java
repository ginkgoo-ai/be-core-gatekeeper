package com.ginkgooai.core.gatekeeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@Tag(name = "Form Submission", description = "APIs for submitting form data")
@Slf4j
public class FormViewController {

	@GetMapping("/forms-ui/{formId}")
	public String showQuestionnaireFillPage(@PathVariable("formId") String formId, Model model) {
		log.info("Request to show questionnaire fill page for formId: {}", formId);

		model.addAttribute("formId", formId);

		return "dynamic_form_renderer"; // Path to your Thymeleaf template
	}

}
