package com.shu.backend.global.moderation;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class TextNormalizationService {

    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);

        normalized = normalized
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't')
                .replace('@', 'a')
                .replace('$', 's');

        normalized = normalized.replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}\\p{So}]", "");
        return collapseRepeatedCharacters(normalized);
    }

    private String collapseRepeatedCharacters(String input) {
        StringBuilder result = new StringBuilder(input.length());
        int repeatCount = 0;
        int previousCodePoint = -1;

        for (int i = 0; i < input.length(); ) {
            int codePoint = input.codePointAt(i);
            if (codePoint == previousCodePoint) {
                repeatCount++;
            } else {
                repeatCount = 1;
                previousCodePoint = codePoint;
            }

            if (repeatCount <= 2) {
                result.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }

        return result.toString();
    }
}
