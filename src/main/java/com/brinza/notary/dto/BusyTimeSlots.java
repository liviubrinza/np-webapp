package com.brinza.notary.dto;

import java.util.Set;

/**
 * Which "HH:mm" candidate times a reschedule form should grey out for the start/end dropdowns
 * on a given date, so the resulting appointment can't be saved overlapping a CONFIRMED one.
 * Start and end use different boundary rules (see
 * {@code AppointmentManagementService.findBusyTimeSlots}): a start exactly matching another
 * confirmed appointment's start is already an overlap, but an end exactly matching another's
 * start is not (they're adjacent) - so the two sets are never simply the same values.
 */
public record BusyTimeSlots(Set<String> startTimes, Set<String> endTimes) {
}
