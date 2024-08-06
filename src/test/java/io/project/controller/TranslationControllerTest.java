package io.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;


    @Test
    @Order(1)
    @DisplayName("Test get supported languages")
    public void getSupportedLanguagesTest() throws Exception {
        var result = mockMvc.perform(get("/supported-languages"))
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray();

        Thread.sleep(2000);
    }

    @Test
    @Order(2)
    @DisplayName("Test translate 1")
    public void translateSuccessTestOneLang() throws Exception {
        String requestDTO = """
                {
                "inputText": "Hello world",
                 "sourceLanguage": "en",
                 "targetLanguage": "ru"
                }""";
        String expected = "Привет мир";
        var result = mockMvc.perform(post("/translate")
                        .content(requestDTO)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = om.readTree(body);
        String actual = json.get("translatedText").asText();
        assertThat(actual).isEqualTo(expected);

        Thread.sleep(2000);
    }

    @Test
    @Order(3)
    @DisplayName("Test translate 2")
    public void translateSuccessTestSecondLang() throws Exception {
        String requestDTO = """
                {
                "inputText": "Soy un programador principiante",
                 "sourceLanguage": "es",
                 "targetLanguage": "en"
                }""";
        String expected = "Am a programmer beginner";
        var result = mockMvc.perform(post("/translate")
                        .content(requestDTO)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = om.readTree(body);
        String actual = json.get("translatedText").asText();
        assertThat(actual).isEqualTo(expected);

        Thread.sleep(2000);
    }
}
