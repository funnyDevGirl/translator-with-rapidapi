package io.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslationRequestDTO {
    private String inputText;
    private String sourceLanguage;
    private String targetLanguage;
}
