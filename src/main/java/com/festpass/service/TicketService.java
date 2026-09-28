package com.festpass.service;

import com.festpass.dto.PurchaseTicketRequest;
import com.festpass.dto.TicketResponse;
import com.festpass.exception.EventCapacityExceededException;
import com.festpass.exception.InvalidTicketException;
import com.festpass.exception.ResourceNotFoundException;
import com.festpass.exception.TicketAlreadyUsedException;
import com.festpass.model.Attendee;
import com.festpass.model.FestEvent;
import com.festpass.model.Ticket;
import com.festpass.model.TicketStatus;
import com.festpass.repository.AttendeeRepository;
import com.festpass.repository.FestEventRepository;
import com.festpass.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final FestEventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final NotificationService notificationService;

    public TicketService(TicketRepository ticketRepository,
                         FestEventRepository eventRepository,
                         AttendeeRepository attendeeRepository,
                         QrCodeGeneratorService qrCodeGeneratorService,
                         NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.attendeeRepository = attendeeRepository;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.notificationService = notificationService;
    }

    /**
     * Issue a digital ticket with a unique QR code on purchase.
     * Enforces capacity rule: Ticket issuance must stop once event reaches declared capacity.
     */
    @Transactional
    public TicketResponse purchaseTicket(PurchaseTicketRequest request) {
        FestEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("FestEvent not found with ID: " + request.getEventId()));

        Attendee attendee = attendeeRepository.findById(request.getAttendeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendee not found with ID: " + request.getAttendeeId()));

        // Check Business Rule: Capacity enforcement BEFORE saving
        long currentTicketsIssued = ticketRepository.countByEventId(event.getId());
        if (currentTicketsIssued >= event.getCapacity()) {
            notificationService.notifyCapacityReached(event.getName(), event.getCapacity());
            throw new EventCapacityExceededException(
                    "Cannot issue ticket. Event '" + event.getName() + "' has reached its maximum capacity of " + event.getCapacity() + "."
            );
        }

        // Generate unique QR code token
        String qrCodeToken = "FEST-" + event.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Ticket ticket = new Ticket();
        ticket.setQrCode(qrCodeToken);
        ticket.setEvent(event);
        ticket.setAttendee(attendee);
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setUsed(false);
        ticket.setPurchasedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);

        notificationService.notifyTicketIssued(savedTicket.getQrCode(), event.getName(), attendee.getName(), attendee.getEmail());

        return mapToResponse(savedTicket);
    }

    /**
     * Validate a QR code at entry and mark the ticket used.
     * Enforces rule: A ticket's QR code can be successfully validated for entry only once.
     */
    @Transactional
    public TicketResponse validateTicket(String qrCode) {
        if (qrCode == null || qrCode.trim().isEmpty()) {
            throw new InvalidTicketException("QR code cannot be empty");
        }

        String trimmedQr = qrCode.trim();
        Ticket ticket = ticketRepository.findByQrCode(trimmedQr)
                .orElseThrow(() -> {
                    notificationService.notifyInvalidCheckInAttempt(trimmedQr, "QR Code not found");
                    return new InvalidTicketException("Invalid ticket QR code: " + trimmedQr);
                });

        // Enforce single-use validation rule
        if (ticket.isUsed() || ticket.getStatus() == TicketStatus.USED) {
            notificationService.notifyInvalidCheckInAttempt(trimmedQr, "Ticket already used");
            throw new TicketAlreadyUsedException(
                    "Ticket with QR code '" + trimmedQr + "' has ALREADY been used for entry on " + ticket.getValidatedAt() + ". Entry denied!"
            );
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            notificationService.notifyInvalidCheckInAttempt(trimmedQr, "Ticket is cancelled");
            throw new InvalidTicketException("Ticket with QR code '" + trimmedQr + "' has been cancelled.");
        }

        // Mark ticket as used
        ticket.setUsed(true);
        ticket.setStatus(TicketStatus.USED);
        ticket.setValidatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        notificationService.notifyTicketValidated(updatedTicket.getQrCode(), updatedTicket.getEvent().getName(), updatedTicket.getAttendee().getName());

        return mapToResponse(updatedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));
        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketByQrCode(String qrCode) {
        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with QR Code: " + qrCode));
        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByEventId(Long eventId) {
        return ticketRepository.findByEventId(eventId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByAttendeeId(Long attendeeId) {
        return ticketRepository.findByAttendeeId(attendeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setTicketId(ticket.getId());
        response.setQrCode(ticket.getQrCode());
        response.setQrCodeImageBase64(qrCodeGeneratorService.generateQrCodeBase64(ticket.getQrCode(), 250, 250));
        
        if (ticket.getEvent() != null) {
            response.setEventId(ticket.getEvent().getId());
            response.setEventName(ticket.getEvent().getName());
            response.setEventVenue(ticket.getEvent().getVenue());
            response.setEventDate(ticket.getEvent().getEventDate());
            response.setTicketPrice(ticket.getEvent().getTicketPrice());
        }

        if (ticket.getAttendee() != null) {
            response.setAttendeeId(ticket.getAttendee().getId());
            response.setAttendeeName(ticket.getAttendee().getName());
            response.setAttendeeEmail(ticket.getAttendee().getEmail());
        }

        response.setStatus(ticket.getStatus());
        response.setUsed(ticket.isUsed());
        response.setPurchasedAt(ticket.getPurchasedAt());
        response.setValidatedAt(ticket.getValidatedAt());

        return response;
    }
}
