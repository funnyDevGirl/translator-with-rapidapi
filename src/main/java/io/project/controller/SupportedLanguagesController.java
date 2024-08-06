package io.project.controller;

import io.project.dto.TranslationRequestDTO;
import io.project.dto.TranslationResponseDTO;
import io.project.model.SupportedLanguagesResponse.Language;
import io.project.service.GoogleTranslateService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Set;


@RestController
@RequestMapping
@AllArgsConstructor
public class SupportedLanguagesController {

    private final GoogleTranslateService translationService;

    @PostMapping("/translate")
    public ResponseEntity<TranslationResponseDTO> translate(@RequestBody TranslationRequestDTO requestDTO,
                                                            @RequestHeader(value = "X-Forwarded-For",
                                                                    defaultValue = "127.0.0.1") String ipAddress) {

        String translatedText = translationService.translate(requestDTO.getInputText(),
                requestDTO.getSourceLanguage(),
                requestDTO.getTargetLanguage(),
                ipAddress);

        TranslationResponseDTO responseDTO = new TranslationResponseDTO();
        responseDTO.setTranslatedText(translatedText);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/supported-languages")
    public Set<Language> getSupportedLanguages() {
        return translationService.fetchSupportedLanguages();
    }
}
