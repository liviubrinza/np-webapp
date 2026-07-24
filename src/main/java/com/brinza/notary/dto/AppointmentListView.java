package com.brinza.notary.dto;

import java.util.List;

public record AppointmentListView(
        List<AppointmentListItemView> pending,
        List<AppointmentListItemView> others) {
}
