package com.brinza.notary.domain;

public enum AppointmentStatus {
    PENDING("În așteptare"),
    CONFIRMED("Confirmată"),
    CANCELLED("Anulată"),
    COMPLETED("Finalizată");

    private final String displayName;

    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
