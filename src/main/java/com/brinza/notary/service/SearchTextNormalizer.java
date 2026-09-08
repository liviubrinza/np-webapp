package com.brinza.notary.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Folds accented letters onto their plain ASCII counterparts so a search for "Molnar" also
 * matches "Molnár" (and the other way round). Both the stored client name and the search term
 * are folded with this, which keeps the behaviour identical on H2 and PostgreSQL — unlike
 * PostgreSQL's {@code unaccent()}, which H2 has no equivalent for.
 */
public final class SearchTextNormalizer {

    /** Combining marks left behind by the NFD decomposition (the accents themselves). */
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /**
     * Letters that carry their mark inside the glyph instead of as a combining accent, so NFD
     * leaves them untouched. Vowels first, since those are what people actually type around.
     */
    private static final String[][] NON_DECOMPOSING = {
            {"ø", "o"}, {"æ", "ae"}, {"œ", "oe"}, {"ı", "i"},
            {"ß", "ss"}, {"đ", "d"}, {"ð", "d"}, {"þ", "th"}, {"ł", "l"}, {"ħ", "h"}, {"ŧ", "t"},
    };

    private SearchTextNormalizer() {
    }

    /**
     * @return the value trimmed, lower-cased and stripped of accents, or {@code null} when it is
     * {@code null} or blank — the queries treat {@code null} as "no filter on this field".
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String folded = value.trim().toLowerCase(Locale.ROOT);
        for (String[] pair : NON_DECOMPOSING) {
            folded = folded.replace(pair[0], pair[1]);
        }
        return COMBINING_MARKS.matcher(Normalizer.normalize(folded, Normalizer.Form.NFD)).replaceAll("");
    }
}
