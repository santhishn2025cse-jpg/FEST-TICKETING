package com.festpass.dto;

import jakarta.validation.constraints.NotNull;

public class PurchaseTicketRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Attendee ID is required")
    private Long attendeeId;

    public PurchaseTicketRequest() {
    }

    public PurchaseTicketRequest(Long eventId, Long attendeeId) {
        this.eventId = eventId;
        this.attendeeId = attendeeId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }
}
