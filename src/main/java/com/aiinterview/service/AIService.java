package com.aiinterview.service;

import com.aiinterview.model.EvaluationResponse;
import com.aiinterview.model.InterviewRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AIService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIService.class);
    private static final String FALLBACK_MODE = "Demo/Fallback AI mode";
    private static final Map<String, List<String>> FALLBACK_QUESTIONS = Map.of(
            "java", List.of(
                    "Explain the four pillars of object-oriented programming with a Java example.",
                    "What is the difference between an abstract class and an interface in Java?",
                    "How do checked and unchecked exceptions differ, and when would you use each?",
                    "Explain how HashMap stores and retrieves key-value pairs.",
                    "What is the difference between == and equals() in Java?",
                    "How does Java garbage collection work at a high level?",
                    "Compare ArrayList and LinkedList, including a use case for each.",
                    "What is the purpose of the final keyword for variables, methods, and classes?",
                    "How do processes and threads differ in Java?",
                    "What are Java streams, and how do map and filter work?"
            ),
            "python", List.of(
                    "What is the difference between a list and a tuple in Python?",
                    "Explain Python's mutable and immutable object behavior with examples.",
                    "What are decorators, and how would you use one?",
                    "How do list comprehensions work, and when might you avoid them?",
                    "What is the difference between an iterator and a generator?",
                    "Explain Python exception handling and the purpose of finally.",
                    "What is the Global Interpreter Lock, and how does it affect threads?",
                    "How do dictionaries handle key lookup?",
                    "What is the difference between a shallow copy and a deep copy?",
                    "How would you organize a reusable Python package?"
            ),
            "sql", List.of(
                    "Explain the difference between INNER JOIN and LEFT JOIN.",
                    "What is database normalization, and why is it useful?",
                    "How do WHERE and HAVING differ in a grouped query?",
                    "What is an index, and what trade-offs does it introduce?",
                    "Explain the ACID properties of a database transaction.",
                    "How would you find the second-highest salary in a table?",
                    "What is the difference between a primary key and a unique constraint?",
                    "When would you use GROUP BY and an aggregate function?",
                    "What is a database transaction, and when should it be rolled back?",
                    "How can you prevent SQL injection in an application?"
            ),
            "react", List.of(
                    "What is the virtual DOM, and how does React use it?",
                    "When would you use state versus props in a React component?",
                    "Explain how the useEffect hook works and how its dependencies matter.",
                    "What is a controlled form component?",
                    "Why are keys important when rendering a list in React?",
                    "How does lifting state up help sibling components share data?",
                    "What is the purpose of React Context, and when should it be avoided?",
                    "How can you reduce unnecessary React component re-renders?",
                    "What is the difference between a client component and server rendering?",
                    "How would you handle loading and error states for a data request?"
            ),
            "spring boot", List.of(
                    "What problem does Spring Boot solve compared with traditional Spring setup?",
                    "Explain dependency injection and constructor injection in Spring.",
                    "What is the purpose of Spring Boot auto-configuration?",
                    "How would you build and validate a REST endpoint in Spring Boot?",
                    "What are the differences between @Component, @Service, and @Repository?",
                    "How do you externalize configuration in a Spring Boot application?",
                    "How would you handle exceptions consistently in a REST API?",
                    "What is the role of a Spring Bean and its application context?",
                    "How would you protect a Spring Boot API from unauthorized access?",
                    "How can you test a Spring MVC controller?"
            ),
            "javascript", List.of(
                    "What is the difference between let, const, and var?",
                    "Explain closures and give a practical use case.",
                    "How do promises differ from async and await syntax?",
                    "What is event bubbling, and how can event delegation use it?",
                    "How do == and === differ in JavaScript?",
                    "What are the JavaScript event loop and task queue?",
                    "What is the difference between null and undefined?",
                    "How does this behave in an arrow function compared with a regular function?",
                    "What are common ways to copy an object, and what are their limitations?",
                    "How would you handle a failed fetch request?"
            ),
            "html/css", List.of(
                    "What is semantic HTML, and why is it important?",
                    "Explain the CSS box model and how box-sizing changes it.",
                    "How do Flexbox and CSS Grid differ, and when would you choose each?",
                    "What are responsive design and media queries?",
                    "How does CSS specificity determine which rule wins?",
                    "What is the difference between an id selector and a class selector?",
                    "How can you make a form accessible to keyboard and screen-reader users?",
                    "What are the differences between block, inline, and inline-block elements?",
                    "How do relative, absolute, fixed, and sticky positioning differ?",
                    "What is the purpose of alt text on an image?"
            ),
            "data structures", List.of(
                    "How do arrays and linked lists differ in access and insertion costs?",
                    "What is a hash table, and how are collisions handled?",
                    "Explain stack and queue behavior with a real-world use case for each.",
                    "How does binary search work, and what precondition does it require?",
                    "What is the difference between a binary tree and a binary search tree?",
                    "How does breadth-first search differ from depth-first search?",
                    "What does Big O notation describe? Compare O(n) and O(log n).",
                    "When might a heap be more appropriate than sorting a full collection?",
                    "What is a graph, and how could you represent it in memory?",
                    "How would you detect a cycle in a linked list?"
            )
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AIService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey.trim();
        this.model = model;
    }

    public Map<String, String> generateQuestion(String topic, String difficulty, List<String> askedQuestions) {
        String normalizedTopic = normalizeTopic(topic);
        if (isGeminiAvailable()) {
            try {
                String prompt = "Write exactly one concise, original " + difficulty + " difficulty interview question for a college placement interview about "
                        + topic + ". Return only the question, without numbering or quotation marks. Avoid these previously asked questions: "
                        + String.join(" | ", askedQuestions);
                String generated = requestGemini(prompt).trim().replaceAll("^[\\d.)\\s*`]+", "").replaceAll("[\\r\\n]+", " ");
                if (!generated.isBlank() && askedQuestions.stream().noneMatch(previous -> previous.equalsIgnoreCase(generated))) {
                    return Map.of("question", generated, "mode", "Gemini AI");
                }
                LOGGER.info("Gemini returned an empty or repeated question; using the local question bank.");
            } catch (RestClientException | JsonProcessingException | IllegalStateException exception) {
                LOGGER.warn("Gemini question generation failed; using the local question bank: {}", exception.getMessage());
            }
        }
        return Map.of("question", pickFallbackQuestion(normalizedTopic, askedQuestions), "mode", FALLBACK_MODE);
    }

    public EvaluationResponse evaluateAnswer(InterviewRequest request) {
        if (isGeminiAvailable()) {
            try {
                String prompt = """
                        Evaluate this college placement interview answer. Return ONLY valid JSON with exactly these keys:
                        score (integer from 0 to 10), strengths (string), weaknesses (string), suggestions (string), modelAnswer (string).
                        Consider correctness, technical accuracy, completeness, clarity, and relevance. Reward a concise correct answer.
                        Topic: %s
                        Question: %s
                        Candidate answer: %s
                        """.formatted(request.topic(), request.question(), request.answer());
                JsonNode result = objectMapper.readTree(requestGemini(prompt));
                int score = Math.max(0, Math.min(10, result.path("score").asInt(-1)));
                if (score >= 0 && hasText(result, "strengths") && hasText(result, "weaknesses")
                        && hasText(result, "suggestions") && hasText(result, "modelAnswer")) {
                    return new EvaluationResponse(score, result.path("strengths").asText(),
                            result.path("weaknesses").asText(), result.path("suggestions").asText(),
                            result.path("modelAnswer").asText(), "Gemini AI");
                }
                LOGGER.info("Gemini evaluation response did not match the required shape; using local evaluation.");
            } catch (RestClientException | JsonProcessingException | IllegalStateException exception) {
                LOGGER.warn("Gemini answer evaluation failed; using local evaluation: {}", exception.getMessage());
            }
        }
        return evaluateLocally(request);
    }

    private String requestGemini(String prompt) throws JsonProcessingException {
        Map<String, Object> payload = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        String response = restClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty response.");
        }
        JsonNode root = objectMapper.readTree(response);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini response did not contain generated text.");
        }
        return text;
    }

    private String pickFallbackQuestion(String topic, List<String> askedQuestions) {
        List<String> questions = FALLBACK_QUESTIONS.get(topic);
        if (questions == null) {
            questions = FALLBACK_QUESTIONS.get("java");
        }
        for (String question : questions) {
            if (askedQuestions.stream().noneMatch(question::equalsIgnoreCase)) {
                return question;
            }
        }
        return "Give a practical example of an important " + topic + " concept and explain the trade-offs involved.";
    }

    private EvaluationResponse evaluateLocally(InterviewRequest request) {
        String answer = request.answer().trim();
        String lowerAnswer = answer.toLowerCase(Locale.ROOT);
        String[] words = answer.split("\\s+");
        int wordCount = words.length;
        int score = wordCount < 5 ? 1 : wordCount < 15 ? 3 : wordCount < 35 ? 5 : 6;
        String[] technicalTerms = request.topic().toLowerCase(Locale.ROOT).split("[\\s/]+");
        int relevantTerms = 0;
        for (String term : technicalTerms) {
            if (term.length() > 2 && lowerAnswer.contains(term)) {
                relevantTerms++;
            }
        }
        if (relevantTerms > 0) {
            score++;
        }
        if (Pattern.compile("\\b(for example|because|therefore|such as|which|when)\\b").matcher(lowerAnswer).find()) {
            score++;
        }
        score = Math.min(8, score);

        String strengths = wordCount >= 15
                ? "You addressed the question in your own words and provided a substantive response."
                : "You made an attempt and kept the response focused on the question.";
        String weaknesses = wordCount < 20
                ? "The answer is brief and may be missing important detail or an example."
                : "A demo evaluator cannot verify every technical claim; check that each point directly answers the question.";
        String suggestions = "Define the key concept, explain how it works, and add a short practical example or trade-off.";
        String modelAnswer = "A strong answer should directly define the relevant " + request.topic()
                + " concept, explain its behavior and trade-offs, and connect it to a concise practical example for: "
                + request.question();
        return new EvaluationResponse(score, strengths, weaknesses, suggestions, modelAnswer, FALLBACK_MODE);
    }

    private boolean isGeminiAvailable() {
        return !apiKey.isBlank();
    }

    private String normalizeTopic(String topic) {
        String normalized = topic.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return switch (normalized) {
            case "html", "css", "html css", "html and css" -> "html/css";
            case "dsa", "data-structures", "data structure" -> "data structures";
            case "springboot", "spring" -> "spring boot";
            default -> normalized;
        };
    }

    private boolean hasText(JsonNode node, String field) {
        return node.path(field).isTextual() && !node.path(field).asText().isBlank();
    }
}
