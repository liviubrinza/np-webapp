package com.brinza.notary.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * The public pages tracked by {@link TrafficStatsService}, identified by their URL path with the
 * {@code /en|ro|hu} locale prefix stripped - the same page is tracked as one entry regardless of
 * which locale a visitor viewed it in.
 */
public enum PublicPage {

    HOME("", "Home"),
    SERVICES("/services", "Services"),
    CONTACT("/contact", "Contact"),
    BOOK("/book", "Book"),
    BOOK_CONFIRMATION("/book/confirmation", "Book confirmation");

    private final String pathAfterLocale;
    private final String label;

    PublicPage(String pathAfterLocale, String label) {
        this.pathAfterLocale = pathAfterLocale;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<PublicPage> fromPathAfterLocale(String pathAfterLocale) {
        return Arrays.stream(values())
                .filter(page -> page.pathAfterLocale.equals(pathAfterLocale))
                .findFirst();
    }
}
