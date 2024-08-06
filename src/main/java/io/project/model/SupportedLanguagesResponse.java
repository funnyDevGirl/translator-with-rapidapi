package io.project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.Set;


@Setter
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportedLanguagesResponse {

    @JsonProperty("languages")
    private Set<Language> languages;


    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode
    public static class Language {

        @ToString.Include
        @NotBlank()
        @Size(max = 3)
        @JsonProperty("language")
        private String language;

        @ToString.Include
        @JsonProperty("name")
        private String name;
    }
}
