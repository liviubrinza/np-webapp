package com.brinza.notary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            // Hungarian
            "Molnár, molnar",
            "Kovács Ödön, kovacs odon",
            "Tűzoltó, tuzolto",
            // Romanian (comma-below letters, plus â/î/ă)
            "Ștefan Țărână, stefan tarana",
            "Mihăiță, mihaita",
            // German / Nordic / other Latin scripts
            "Müller, muller",
            "Straße, strasse",
            "Søren Kjærgaard, soren kjaergaard",
            "Łukasz, lukasz",
            // already plain, and mixed case
            "Molnar, molnar",
            "MOLNÁR, molnar",
    })
    void foldsAccentsOntoTheirPlainCounterparts(String input, String expected) {
        assertThat(SearchTextNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    void accentedAndPlainSpellingsFoldToTheSameValue() {
        assertThat(SearchTextNormalizer.normalize("Molnár")).isEqualTo(SearchTextNormalizer.normalize("Molnar"));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(SearchTextNormalizer.normalize("  Molnár  ")).isEqualTo("molnar");
    }

    @Test
    void treatsNullAndBlankAsNoFilter() {
        assertThat(SearchTextNormalizer.normalize(null)).isNull();
        assertThat(SearchTextNormalizer.normalize("   ")).isNull();
    }
}
