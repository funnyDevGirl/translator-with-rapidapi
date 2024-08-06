package io.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.project.exception.LanguageNotFoundException;
import io.project.exception.TranslationResourceAccessException;
import io.project.model.Translation;
import io.project.repository.TranslationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import static io.project.model.SupportedLanguagesResponse.Language;


@Service
public class GoogleTranslateService {

    @Value("${api-key}")
    private String apiKey;

    @Value("${translate-url}")
    private String translateUrl;

    @Value("${api-url}")
    private String apiUrl;

    private static final String API_HOST = "google-translator9.p.rapidapi.com";
    private final ObjectMapper om;
    private final RestTemplate restTemplate;
    private final TranslationRepository repository;
    private static final int MAX_THREADS = 10;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    public GoogleTranslateService(RestTemplate restTemplate,
                                  TranslationRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
        this.om = new ObjectMapper();
    }

    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public String translate(String inputText, String sourceLang, String targetLang, String ipAddress) {

        // validation
        Set<Language> supportedLanguages = fetchSupportedLanguages();

        if (isNotContainsLang(sourceLang, supportedLanguages)) {
            throw new LanguageNotFoundException("Не найден исходный язык: " + sourceLang);
        }
        if (isNotContainsLang(targetLang, supportedLanguages)) {
            throw new LanguageNotFoundException("Не найден целевой язык: " + targetLang);
        }

        List<CompletableFuture<String>> translationFutures = new ArrayList<>();

        // translate words
        for (String word : inputText.split(" ")) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return translateWord(word, sourceLang, targetLang);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor);
            translationFutures.add(future);
        }

        // formation of the translated text
        String finishedText = translationFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(" "));

        // save to DB
        Translation request = new Translation();
        request.setIpAddress(ipAddress);
        request.setInputText(inputText);
        request.setTranslatedText(finishedText);
        repository.save(request);

        return finishedText;
    }

    public String translateWord(String text, String sourceLanguage, String targetLanguage)
            throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey);
        headers.set("x-rapidapi-host", API_HOST);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("q", text);
        requestBody.put("source", sourceLanguage);
        requestBody.put("target", targetLanguage);

        String jsonBody = om.writeValueAsString(requestBody);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                translateUrl, HttpMethod.POST, entity, String.class);


        if (response.getStatusCode() == HttpStatus.OK) {
            return parseTranslatedWord(response.getBody());
        } else {
            throw new TranslationResourceAccessException("Ошибка доступа к ресурсу перевода"
                    + response.getStatusCode());
        }
    }

    private String parseTranslatedWord(String responseBody) {
        try {
            JsonNode rootNode = om.readTree(responseBody);
            JsonNode translationsNode = rootNode.path("data").path("translations");
            if (translationsNode.isArray() && !translationsNode.isEmpty()) {
                var text = translationsNode.get(0).path("translatedText").asText();
                return URLDecoder.decode(text, StandardCharsets.UTF_8);
            } else {
                throw new TranslationResourceAccessException(
                    "Ошибка доступа к ресурсу перевода: неверный формат ответа");
            }
        } catch (Exception e) {
            throw new TranslationResourceAccessException("Ошибка доступа к ресурсу перевода: "
                + e.getMessage());
        }
    }

    private boolean isNotContainsLang(String lang, Set<Language> languages) {
        return languages.stream().noneMatch(l -> l.getLanguage().equals(lang));
    }

    public Set<Language> fetchSupportedLanguages() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey);
        headers.set("x-rapidapi-host", API_HOST);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode js = om.readTree(response.getBody());
                JsonNode l = js.path("data").path("languages");

                Set<Language> languages = new HashSet<>();

                for (JsonNode n : l) {
                    Language language = new Language();
                    language.setLanguage(n.path("language").asText());
                    language.setName(n.path("name").asText());
                    languages.add(language);
                }
                return languages;

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new RuntimeException("Не удалось получить список поддерживаемых языков: "
                    + response.getStatusCode());
        }
    }
}
