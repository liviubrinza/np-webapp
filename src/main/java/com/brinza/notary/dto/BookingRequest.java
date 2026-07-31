package com.brinza.notary.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class BookingRequest {

    @NotBlank(message = "{book.clientName.required}")
    @Size(max = 100)
    private String clientName;

    @NotBlank(message = "{book.email.required}")
    @Email(message = "{book.email.invalid}")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "{book.phone.required}")
    @Size(max = 20)
    private String phone;

    @NotNull(message = "{book.service.required}")
    private Long serviceId;

    @NotNull(message = "{book.requestedAt.required}")
    @Future(message = "{book.requestedAt.future}")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime requestedAt;

    @Size(max = 2000, message = "{book.notes.tooLong}")
    private String notes;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    @AssertTrue(message = "{book.requestedAt.halfHour}")
    public boolean isRequestedAtOnHalfHour() {
        if (requestedAt == null) {
            return true;
        }
        int hour = requestedAt.getHour();
        int minute = requestedAt.getMinute();
        if (requestedAt.getSecond() != 0 || (minute != 0 && minute != 30)) {
            return false;
        }
        return hour >= 9 && hour <= 17 && !(hour == 17 && minute == 30);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
