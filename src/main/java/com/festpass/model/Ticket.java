package com.festpass.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private FestEvent event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attendee_id", nullable = false)
    private Attendee attendee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.ACTIVE;

    @Column(name = "checked_in", nullable = false)
    private boolean isUsed = false;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    public Ticket() {
    }

    public Ticket(Long id, String qrCode, FestEvent event, Attendee attendee, TicketStatus status, boolean isUsed, LocalDateTime purchasedAt, LocalDateTime validatedAt) {
        this.id = id;
        this.qrCode = qrCode;
        this.event = event;
        this.attendee = attendee;
        this.status = status;
        this.isUsed = isUsed;
        this.purchasedAt = purchasedAt;
        this.validatedAt = validatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public FestEvent getEvent() {
        return event;
    }

    public void setEvent(FestEvent event) {
        this.event = event;
    }

    public Attendee getAttendee() {
        return attendee;
    }

    public void setAttendee(Attendee attendee) {
        this.attendee = attendee;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(LocalDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }
}
