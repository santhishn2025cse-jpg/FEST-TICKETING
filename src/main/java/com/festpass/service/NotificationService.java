package com.festpass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void notifyTicketIssued(String qrCode, String eventName, String attendeeName, String attendeeEmail) {
        logger.info("[NOTIFICATION] Ticket Issued! Event: '{}', Attendee: '{}' ({}), QR Code: '{}'", 
                eventName, attendeeName, attendeeEmail, qrCode);
    }

    public void notifyTicketValidated(String qrCode, String eventName, String attendeeName) {
        logger.info("[NOTIFICATION] Gate Check-in Successful! Event: '{}', Attendee: '{}', QR Code: '{}'", 
                eventName, attendeeName, qrCode);
    }

    public void notifyCapacityReached(String eventName, int capacity) {
        logger.warn("[ALERT] Event Capacity Limit Reached! Event: '{}', Max Capacity: {}", eventName, capacity);
    }

    public void notifyInvalidCheckInAttempt(String qrCode, String reason) {
        logger.error("[REJECTED CHECK-IN] QR Code: '{}', Reason: {}", qrCode, reason);
    }
}
