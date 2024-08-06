package io.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.project.exception.LanguageNotFoundException;
import io.project.exception.TranslationResourceAccessException;
import io.project.model.SupportedLanguagesResponse;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    private static final String API_HOST = "google-translate1.p.rapidapi.com";

    private final RestTemplate restTemplate;
    private final TranslationRepository repository;
    private static final int MAX_THREADS = 10;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    public GoogleTranslateService(RestTemplate restTemplate,
                                  TranslationRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public String translate(String inputText, String sourceLang, String targetLang, String ipAddress) {

        // validation
        Set<Language> supportedLanguages = fetchSupportedLanguages();

        if (isContainsLang(sourceLang, supportedLanguages)) {
            throw new LanguageNotFoundException("Не найден исходный язык: " + sourceLang);
        }
        if (isContainsLang(targetLang, supportedLanguages)) {
            throw new LanguageNotFoundException("Не найден целевой язык: " + targetLang);
        }

        List<CompletableFuture<String>> translationFutures = new ArrayList<>();

        // translate words
        for (String word : inputText.split(" ")) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> translateWord(word, sourceLang, targetLang), executor);
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

        executor.shutdown();
        return finishedText;
    }

    public String translateWord(String text, String sourceLanguage, String targetLanguage) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey);
        headers.set("x-rapidapi-host", API_HOST);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("q", text);
        body.add("target", targetLanguage);
        body.add("source", sourceLanguage);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return parseTranslatedWord(response.getBody());
        } else {
            throw new TranslationResourceAccessException("Ошибка доступа к ресурсу перевода"
                    + response.getStatusCode());
        }
    }


    private String parseTranslatedWord(String responseBody) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode translationsNode = rootNode.path("translations");

            if (translationsNode.isArray() && !translationsNode.isEmpty()) {
                return translationsNode.get(0).path("text").asText();
            } else {
                throw new TranslationResourceAccessException(
                        "Ошибка доступа к ресурсу перевода: неверный формат ответа");
            }
        } catch (Exception e) {
            throw new TranslationResourceAccessException("Ошибка доступа к ресурсу перевода: "
                    + e.getMessage());
        }
    }

    private boolean isContainsLang(String lang, Set<Language> languages) {
        return languages.stream().anyMatch(l -> l.getLanguage().equals(lang));
    }

    public Set<Language> fetchSupportedLanguages() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey);
        headers.set("x-rapidapi-host", API_HOST);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<SupportedLanguagesResponse> response = Objects.requireNonNull(restTemplate).exchange(
                Objects.requireNonNull(apiUrl), HttpMethod.GET, entity, SupportedLanguagesResponse.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return Objects.requireNonNull(response.getBody()).getLanguages();
        } else {
            throw new RuntimeException("Не удалось получить список поддерживаемых языков: "
                    + response.getStatusCode());
        }
    }
}
