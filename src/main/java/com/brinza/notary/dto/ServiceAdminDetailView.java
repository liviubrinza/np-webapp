package com.brinza.notary.dto;

public record ServiceAdminDetailView(Long id, String code, int durationMinutes, boolean active,
                                      String nameEn, String descriptionEn,
                                      String nameRo, String descriptionRo,
                                      String nameHu, String descriptionHu) {
}
