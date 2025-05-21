package com.ginkgooai.core.gatekeeper.service;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormImageProcessingService {

	private final GoogleAiGeminiChatModel chatModel;

	public FormImageProcessingService(@Value("${langchain4j.vertex-ai.chat.api-key}") String apiKey,
			@Value("${langchain4j.vertex-ai.chat.model-name}") String modelName) {

		this.chatModel = GoogleAiGeminiChatModel.builder().apiKey(apiKey).modelName(modelName).build();
	}

	public String processImageToFormJson(MultipartFile imageFile) throws IOException {
		byte[] imageBytes = imageFile.getBytes();
		String base64ImageData = Base64.getEncoder().encodeToString(imageBytes);
		String mimeType = imageFile.getContentType();

		Image image = Image.builder().base64Data(base64ImageData).mimeType(mimeType).build();

		// CRITICAL: This prompt needs to be carefully engineered.
		// It must instruct the LLM to return JSON matching your
		// CreateFormDefinitionRequest structure.
		String promptText = """
				Analyze the provided image, which represents a form structure.
				Generate a JSON object that represents this form. The JSON should be compatible with the following structure:
				{
				  "name": "Form Name (deduce from image or use a placeholder)",
				  "version": "1.0.0",
				  "description": "Form Description (deduce from image or leave empty)",
				  "targetAudience": "",
				  "formType": "QUESTIONNAIRE", // Default or deduce if possible, e.g. GENERAL, SURVEY, QUESTIONNAIRE
				  "initialLogic": {},
				  "submissionLogic": {},
				  "sections": [
				    {
				      "title": "Section Title",
				      "order": 1,
				      "condition": "",
				      "fields": [
				        {
				          "name": "field_key_name (unique, snake_case)",
				          "label": "Field Label Text",
				          "fieldType": "TEXT", // e.g., TEXT, TEXTAREA, RADIO, DATE, NUMBER, SELECT
				          "staticOptions": [ // Only for RADIO, SELECT, CHECKBOX types
				            {"value": "option_value_1", "label": "Option Label 1"},
				            {"value": "option_value_2", "label": "Option Label 2"}
				          ],
				          "order": 1,
				          "placeholder": "Placeholder text (if any)",
				          "defaultValue": "",
				          "optionsSourceType": "STATIC", // For fields with options
				          "apiEndpoint": "",
				          "uiProperties": {},
				          "condition": "",
				          "dependencies": [],
				          "validations": [ // Attempt to deduce basic validations if possible
				            {
				              "type": "REQUIRED", // e.g. REQUIRED, MIN_LENGTH, MAX_LENGTH, PATTERN
				              "value": "true", // or length number, or regex string
				              "errorMessage": "This field is required."
				            }
				          ]
				        }
				      ]
				    }
				  ]
				}
				Pay close attention to field types (TEXT, TEXTAREA, RADIO, DATE, NUMBER). For RADIO or SELECT types, list the options.
				If the image contains multiple sections, represent them accordingly.
				Ensure field names (keys) are unique and use snake_case.
				If a field appears to be mandatory, add a REQUIRED validation.
				Field labels should not end with a colon (:) character.
				The output should be ONLY the JSON object string. Do not include any other text or explanations.
				""";

		UserMessage userMessage = UserMessage.from(ImageContent.from(image), TextContent.from(promptText));

		ChatResponse response = chatModel.chat(userMessage);

		System.out.println("Gemini's response:");
		System.out.println(response.aiMessage());

		return extractJsonFromMarkdown(response.aiMessage().text());
	}

	private String extractJsonFromMarkdown(String aiText) {
		if (aiText == null)
			return null;
		Pattern pattern = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```");
		Matcher matcher = pattern.matcher(aiText);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return aiText.trim();
	}

}