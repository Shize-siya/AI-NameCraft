package com.deepseek;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;

    public DeepSeekClient(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public String generateClassName(String description) throws IOException {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }

        String prompt = "Based on the following functional description, generate a suitable Java class name. " +
                "Follow these rules:\n" +
                "1. Return ONLY the class name, no other text or explanations\n" +
                "2. Use PascalCase (capitalize first letter of each word)\n" +
                "3. Make it descriptive and meaningful\n" +
                "4. Avoid generic names like 'MyClass' or 'TestClass'\n" +
                "5. Ensure it's a valid Java identifier\n\n" +
                "Description: " + description;

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "deepseek-chat");
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 100);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("top_p", 0.9);

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new IOException("API request failed: " + response.code() + " - " + response.message() +
                        ". Response: " + errorBody);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response from API");
            }

            String responseString = responseBody.string();
            return extractClassNameFromResponse(responseString);
        } catch (Exception e) {
            throw new IOException("Failed to call DeepSeek API: " + e.getMessage(), e);
        }
    }

    private String extractClassNameFromResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!jsonResponse.has("choices")) {
                throw new RuntimeException("Invalid API response format: no choices field");
            }

            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices.size() == 0) {
                throw new RuntimeException("No choices in API response");
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            if (!firstChoice.has("message")) {
                throw new RuntimeException("Invalid API response format: no message field");
            }

            JsonObject message = firstChoice.getAsJsonObject("message");
            if (!message.has("content")) {
                throw new RuntimeException("Invalid API response format: no content field");
            }

            String content = message.get("content").getAsString().trim();

            content = cleanClassName(content);

            if (!isValidJavaClassName(content)) {
                content = generateFallbackClassName();
            }

            return content;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse API response: " + e.getMessage() +
                    ". Response: " + responseBody, e);
        }
    }

    private String cleanClassName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return generateFallbackClassName();
        }

        className = className.replaceAll("(?i)```(java)?|```", "").trim();

        String[] lines = className.split("\n");
        if (lines.length > 0) {
            className = lines[0].trim();
        }

        className = className.replaceAll("[^a-zA-Z0-9]", "");

        if (!className.isEmpty() && Character.isLowerCase(className.charAt(0))) {
            className = Character.toUpperCase(className.charAt(0)) + className.substring(1);
        }

        if (className.isEmpty()) {
            return generateFallbackClassName();
        }

        return className;
    }

    private boolean isValidJavaClassName(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }

        if (!className.matches("^[A-Z][a-zA-Z0-9]*$")) {
            return false;
        }

        String[] reservedKeywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double",
                "else", "enum", "extends", "final", "finally", "float", "for",
                "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private",
                "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while"
        };

        for (String keyword : reservedKeywords) {
            if (className.equalsIgnoreCase(keyword)) {
                return false;
            }
        }

        return true;
    }

    private String generateFallbackClassName() {
        return "GeneratedClass";
    }


    public boolean testConnection() {
        try {
            String testDescription = "user authentication service";
            String className = generateClassName(testDescription);
            return className != null && !className.isEmpty() && isValidJavaClassName(className);
        } catch (Exception e) {
            return false;
        }
    }

    public String getApiUrl() {
        return API_URL;
    }
}

