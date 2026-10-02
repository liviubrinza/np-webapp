package com.brinza.notary.domain;

import java.util.Locale;

/**
 * Per-user admin panel colour theme. {@link #attributeValue()} is what goes into the
 * {@code data-bs-theme} attribute on {@code <html>}, which switches Bootstrap's own colour
 * mode and the dark overrides in {@code style.css}.
 */
public enum AdminTheme {
    LIGHT("Luminoasă"),
    DARK("Întunecată");

    private final String displayName;

    AdminTheme(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String attributeValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
