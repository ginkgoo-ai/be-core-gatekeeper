package com.ginkgooai.core.gatekeeper;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

public class TextInput {

	// Base64编码器
	private static final Base64.Encoder b64encoder = Base64.getEncoder();

	/**
	 * 从URL读取字节数据
	 * @param imageUrl 图片URL
	 * @return 字节数组
	 * @throws IOException 如果读取失败
	 */
	private static byte[] readBytes(String imageUrl) throws IOException {
		URL url = new URL(imageUrl);
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");

		try (InputStream inputStream = connection.getInputStream()) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			return outputStream.toByteArray();
		}
	}

	public static void main(String[] args) throws IOException {
		// PNG of the cute colorful parrot mascot of the LangChain4j project
		String base64Img = b64encoder
			.encodeToString(readBytes("https://avatars.githubusercontent.com/u/132277850?v=4"));

		// 构建Gemini模型
		ChatModel gemini = GoogleAiGeminiChatModel.builder()
			.apiKey("AIzaSyCdyea4K9o0I1jYeFqwj8wvkMDT1aGiCgo")
			.modelName("gemini-1.5-flash")
			.temperature(0.7)
			.maxOutputTokens(2048)
			.build();

		// 创建包含图片和文本的用户消息
		UserMessage userMessage = UserMessage.from(ImageContent.from(base64Img, "image/png"), TextContent.from("""
				Do you think this logo fits well
				with the project description?
				"""));

		// 发送请求并获取响应
		ChatResponse response = gemini.chat(userMessage);

		// 打印响应内容
		System.out.println("Gemini's response:");
		System.out.println(response.aiMessage());

		// 如果需要获取更多响应信息
		System.out.println("\nToken usage information:");
		System.out.println("Input tokens: " + response.tokenUsage().inputTokenCount());
		System.out.println("Output tokens: " + response.tokenUsage().outputTokenCount());
		System.out.println("Total tokens: " + response.tokenUsage().totalTokenCount());
	}

}
