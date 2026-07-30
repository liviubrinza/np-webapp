package com.brinza.notary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ServiceForm {

    @NotBlank(message = "Codul serviciului este obligatoriu.")
    @Size(max = 100, message = "Codul serviciului trebuie să aibă cel mult 100 de caractere.")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Codul poate conține doar litere mici, cifre și cratimă.")
    private String code;

    @NotNull(message = "Durata este obligatorie.")
    @Positive(message = "Durata trebuie să fie un număr pozitiv de minute.")
    private Integer durationMinutes;

    private boolean active = true;

    @NotBlank(message = "Numele în engleză este obligatoriu.")
    @Size(max = 255)
    private String nameEn;

    @Size(max = 2000, message = "Descrierea trebuie să aibă cel mult 2000 de caractere.")
    private String descriptionEn;

    @NotBlank(message = "Numele în română este obligatoriu.")
    @Size(max = 255)
    private String nameRo;

    @Size(max = 2000, message = "Descrierea trebuie să aibă cel mult 2000 de caractere.")
    private String descriptionRo;

    @NotBlank(message = "Numele în maghiară este obligatoriu.")
    @Size(max = 255)
    private String nameHu;

    @Size(max = 2000, message = "Descrierea trebuie să aibă cel mult 2000 de caractere.")
    private String descriptionHu;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    public String getNameRo() {
        return nameRo;
    }

    public void setNameRo(String nameRo) {
        this.nameRo = nameRo;
    }

    public String getDescriptionRo() {
        return descriptionRo;
    }

    public void setDescriptionRo(String descriptionRo) {
        this.descriptionRo = descriptionRo;
    }

    public String getNameHu() {
        return nameHu;
    }

    public void setNameHu(String nameHu) {
        this.nameHu = nameHu;
    }

    public String getDescriptionHu() {
        return descriptionHu;
    }

    public void setDescriptionHu(String descriptionHu) {
        this.descriptionHu = descriptionHu;
    }
}
