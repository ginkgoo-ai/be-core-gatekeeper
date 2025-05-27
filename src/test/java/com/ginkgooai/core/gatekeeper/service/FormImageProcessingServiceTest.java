package com.ginkgooai.core.gatekeeper.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FormImageProcessingServiceTest {

	@Mock
	private GoogleAiGeminiChatModel chatModel;

	private FormImageProcessingService formImageProcessingService;

	@BeforeEach
	void setUp() {
		// Create the service with mocked dependencies
		formImageProcessingService = new FormImageProcessingService("fake-api-key", "gemini-pro-vision");

		// Replace the real chatModel with our mock
		ReflectionTestUtils.setField(formImageProcessingService, "chatModel", chatModel);
	}

	// @Test
	void testProcessImageToFormJson() throws IOException {
		// Sample JSON response that would come from the AI model
		String expectedJson = """
				{
				  "name": "Patient Registration Form",
				  "version": "1.0.0",
				  "description": "Form for collecting patient information",
				  "targetAudience": "New patients",
				  "formType": "QUESTIONNAIRE",
				  "initialLogic": {},
				  "submissionLogic": {},
				  "sections": [
				    {
				      "title": "Personal Information",
				      "order": 1,
				      "condition": "",
				      "fields": [
				        {
				          "name": "full_name",
				          "label": "Full Name",
				          "fieldType": "TEXT",
				          "order": 1,
				          "placeholder": "Enter your full name",
				          "defaultValue": "",
				          "optionsSourceType": "STATIC",
				          "apiEndpoint": "",
				          "uiProperties": {},
				          "condition": "",
				          "dependencies": [],
				          "validations": [
				            {
				              "type": "REQUIRED",
				              "value": "true",
				              "errorMessage": "This field is required."
				            }
				          ]
				        }
				      ]
				    }
				  ]
				}
				""";

		// Create a mock MultipartFile (simulating an uploaded image)
		MockMultipartFile mockImageFile = new MockMultipartFile("image", "test-form.jpg", "image/jpeg",
				"test image content".getBytes());

		// Mock the AI response
		ChatResponse mockChatResponse = mock(ChatResponse.class);
		AiMessage mockAiMessage = mock(AiMessage.class);

		when(mockChatResponse.aiMessage()).thenReturn(mockAiMessage);
		when(mockAiMessage.text()).thenReturn(expectedJson);
		// when(chatModel.chat(any())).thenReturn(mockChatResponse);

		// Call the method being tested
		String result = formImageProcessingService.processImageToFormJson(mockImageFile);

		// Verify the result
		assertNotNull(result);
		assertEquals(expectedJson, result);
	}

	/**
	 * This test uses an actual image file for testing. Note: This test will be ignored by
	 * default since it requires a real API key and an actual image file. Uncomment and
	 * configure to run manually.
	 */
	// @Test
	void testWithRealImage() throws IOException {
		// To run this test:
		// 1. Place a test image in src/test/resources
		// 2. Configure a real API key
		// 3. Uncomment this test

		/*
		 * // Load a real image file from resources Path imagePath =
		 * Paths.get("src/test/resources/test-form.jpg"); byte[] imageBytes =
		 * Files.readAllBytes(imagePath);
		 * 
		 * MockMultipartFile mockImageFile = new MockMultipartFile( "image",
		 * "test-form.jpg", "image/jpeg", imageBytes);
		 * 
		 * // Create a real service instance with your actual API key
		 * FormImageProcessingService realService = new
		 * FormImageProcessingService("YOUR_REAL_API_KEY", "gemini-pro-vision");
		 * 
		 * // Process the image String result =
		 * realService.processImageToFormJson(mockImageFile);
		 * 
		 * // Just verify we got some result assertNotNull(result);
		 * System.out.println("AI Generated Form JSON:"); System.out.println(result);
		 */
	}

}