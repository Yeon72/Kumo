package net.kumo.kumo.domain.dto; // domain 추가

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslationRequestDTO {
    private List<String> text;
    private String target_lang;
}