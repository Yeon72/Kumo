package net.kumo.kumo.domain.dto; // domain 추가

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class TranslationResponseDTO {
    private List<TranslationData> translations;
}

@Getter
@Setter
class TranslationData {
    private String text;
}