package io.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslationDTO {
    private String ipAddress;
    private String inputText;
    private String translatedText;
}
