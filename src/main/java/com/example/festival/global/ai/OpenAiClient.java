package com.example.festival.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OpenAiClient {

    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper om = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public String generateText(String systemPrompt, String userPrompt, int maxOutputTokens) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("OPENAI_API_KEY가 비어있음");
            }

            ObjectNode body = om.createObjectNode();
            body.put("model", model);
            body.put("max_output_tokens", maxOutputTokens);

            // 최신 Responses API: JSON 출력 강제
            ObjectNode text = body.putObject("text");
            ObjectNode format = text.putObject("format");
            format.put("type", "json_object");

            // input은 문자열 content로 (가장 안전)
            var inputArr = body.putArray("input");
            ObjectNode sys = om.createObjectNode();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            inputArr.add(sys);

            ObjectNode usr = om.createObjectNode();
            usr.put("role", "user");
            usr.put("content", userPrompt);
            inputArr.add(usr);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(RESPONSES_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI HTTP " + resp.statusCode() + " / body=" + resp.body());
            }

            JsonNode root = om.readTree(resp.body());

            JsonNode outText = root.get("output_text");
            if (outText != null && !outText.isNull() && !outText.asText().isBlank()) {
                return outText.asText();
            }

            // fallback: output 배열 순회
            StringBuilder sb = new StringBuilder();
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.path("content");
                    if (content.isArray()) {
                        for (JsonNode c : content) {
                            String type = c.path("type").asText("");
                            if ("output_text".equals(type) || "text".equals(type)) {
                                String txt = c.path("text").asText("");
                                if (!txt.isBlank()) sb.append(txt);
                            }
                        }
                    }
                }
            }

            String merged = sb.toString().trim();
            if (!merged.isBlank()) return merged;

            return resp.body();

        } catch (Exception e) {
            throw new RuntimeException("OpenAiClient failed: " + e.getMessage(), e);
        }
    }
}
