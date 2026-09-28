package com.festpass.controller;

import com.festpass.dto.ApiResponse;
import com.festpass.dto.PurchaseTicketRequest;
import com.festpass.dto.TicketResponse;
import com.festpass.dto.ValidateTicketRequest;
import com.festpass.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Fest Tickets", description = "Endpoints for ticket issuance, QR validation, and check-in")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/purchase")
    @Operation(summary = "Core Feature 2: Issue a digital ticket with a unique QR code on purchase (enforces capacity rule)")
    public ResponseEntity<ApiResponse<TicketResponse>> purchaseTicket(@Valid @RequestBody PurchaseTicketRequest request) {
        TicketResponse ticketResponse = ticketService.purchaseTicket(request);
        return new ResponseEntity<>(
                ApiResponse.success("Digital ticket issued successfully", ticketResponse),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/validate")
    @Operation(summary = "Core Feature 3 & 4: Validate a QR code at entry and mark used. Rejects if already used or invalid.")
    public ResponseEntity<ApiResponse<TicketResponse>> validateTicket(@Valid @RequestBody ValidateTicketRequest request) {
        TicketResponse ticketResponse = ticketService.validateTicket(request.getQrCode());
        return ResponseEntity.ok(ApiResponse.success("QR Code Validated Successfully! Gate entry approved.", ticketResponse));
    }

    @GetMapping
    @Operation(summary = "Get all tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getAllTickets() {
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by Ticket ID")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse ticketResponse = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket details retrieved", ticketResponse));
    }

    @GetMapping("/qr/{qrCode}")
    @Operation(summary = "Get ticket by QR Code string")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketByQrCode(@PathVariable String qrCode) {
        TicketResponse ticketResponse = ticketService.getTicketByQrCode(qrCode);
        return ResponseEntity.ok(ApiResponse.success("Ticket details retrieved", ticketResponse));
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get all tickets for an event")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByEventId(@PathVariable Long eventId) {
        List<TicketResponse> tickets = ticketService.getTicketsByEventId(eventId);
        return ResponseEntity.ok(ApiResponse.success("Event tickets retrieved", tickets));
    }

    @GetMapping("/attendee/{attendeeId}")
    @Operation(summary = "Get all tickets purchased by an attendee")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByAttendeeId(@PathVariable Long attendeeId) {
        List<TicketResponse> tickets = ticketService.getTicketsByAttendeeId(attendeeId);
        return ResponseEntity.ok(ApiResponse.success("Attendee tickets retrieved", tickets));
    }
}
