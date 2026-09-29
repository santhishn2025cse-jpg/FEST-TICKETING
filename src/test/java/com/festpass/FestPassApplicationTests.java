package com.festpass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festpass.dto.CreateAttendeeRequest;
import com.festpass.dto.CreateEventRequest;
import com.festpass.dto.PurchaseTicketRequest;
import com.festpass.dto.ValidateTicketRequest;
import com.festpass.model.FestEvent;
import com.festpass.model.Ticket;
import com.festpass.model.TicketStatus;
import com.festpass.repository.AttendeeRepository;
import com.festpass.repository.FestEventRepository;
import com.festpass.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FestPassApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FestEventRepository eventRepository;

    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        attendeeRepository.deleteAll();
    }

    @Test
    @DisplayName("Feature 1: Create fest event with capacity and ticket price")
    void testCreateFestEvent() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "TechFest Hackathon 2026",
                "Annual coding and innovation festival",
                "Auditorium Main Hall",
                LocalDateTime.now().plusDays(10),
                50,
                new BigDecimal("499.00")
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("TechFest Hackathon 2026"))
                .andExpect(jsonPath("$.data.capacity").value(50))
                .andExpect(jsonPath("$.data.ticketPrice").value(499.00));
    }

    @Test
    @DisplayName("Feature 2: Issue a digital ticket with unique QR code on purchase")
    void testPurchaseTicketSuccess() throws Exception {
        // Create Event with capacity 2
        FestEvent event = eventRepository.save(new FestEvent(null, "Music Fest", "Live Concert", "Grounds", LocalDateTime.now().plusDays(5), 2, new BigDecimal("250.00")));
        
        // Create Attendee
        CreateAttendeeRequest attendeeReq = new CreateAttendeeRequest("Alex Rivera", "alex@example.com", "+1234567890");
        String attendeeRespStr = mockMvc.perform(post("/api/attendees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendeeReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long attendeeId = objectMapper.readTree(attendeeRespStr).get("data").get("id").asLong();

        // Purchase Ticket
        PurchaseTicketRequest purchaseReq = new PurchaseTicketRequest(event.getId(), attendeeId);

        mockMvc.perform(post("/api/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qrCode").value(startsWith("FEST-")))
                .andExpect(jsonPath("$.data.qrCodeImageBase64").value(startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.used").value(false));
    }

    @Test
    @DisplayName("Business Rule: Ticket issuance must stop once an event reaches declared capacity")
    void testCapacityEnforcementRule() throws Exception {
        // Create Event with capacity = 1
        FestEvent event = eventRepository.save(new FestEvent(null, "VIP Gala", "Exclusive Party", "Lounge", LocalDateTime.now().plusDays(2), 1, new BigDecimal("1000.00")));

        // Create 2 Attendees
        String att1Str = mockMvc.perform(post("/api/attendees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAttendeeRequest("Guest 1", "guest1@example.com", "111"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long att1Id = objectMapper.readTree(att1Str).get("data").get("id").asLong();

        String att2Str = mockMvc.perform(post("/api/attendees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAttendeeRequest("Guest 2", "guest2@example.com", "222"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long att2Id = objectMapper.readTree(att2Str).get("data").get("id").asLong();

        // 1st Ticket Purchase -> Should succeed
        mockMvc.perform(post("/api/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PurchaseTicketRequest(event.getId(), att1Id))))
                .andExpect(status().isCreated());

        // 2nd Ticket Purchase -> Must be REJECTED immediately due to capacity limit reached!
        mockMvc.perform(post("/api/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PurchaseTicketRequest(event.getId(), att2Id))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Capacity Exceeded"))
                .andExpect(jsonPath("$.message").value(containsString("reached its maximum capacity of 1")));
    }

    @Test
    @DisplayName("Feature 3 & 4 & Business Rule: Validate QR code once at entry, reject duplicate check-in")
    void testValidateQrCodeOnceAndRejectDuplicate() throws Exception {
        // Create Event & Attendee
        FestEvent event = eventRepository.save(new FestEvent(null, "RoboWars 2026", "Bot fights", "Arena", LocalDateTime.now().plusDays(3), 10, new BigDecimal("150.00")));
        String attStr = mockMvc.perform(post("/api/attendees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAttendeeRequest("Sarah Connor", "sarah@example.com", "999"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long attId = objectMapper.readTree(attStr).get("data").get("id").asLong();

        // Issue Ticket
        String ticketRespStr = mockMvc.perform(post("/api/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PurchaseTicketRequest(event.getId(), attId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String qrCode = objectMapper.readTree(ticketRespStr).get("data").get("qrCode").asText();

        // 1st Entry Validation -> Must Succeed!
        mockMvc.perform(post("/api/tickets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidateTicketRequest(qrCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("USED"))
                .andExpect(jsonPath("$.data.used").value(true))
                .andExpect(jsonPath("$.data.validatedAt").isNotEmpty());

        // 2nd Entry Validation using SAME QR Code -> Must be REJECTED!
        mockMvc.perform(post("/api/tickets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidateTicketRequest(qrCode))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ticket Already Used"))
                .andExpect(jsonPath("$.message").value(containsString("ALREADY been used for entry")));
    }

    @Test
    @DisplayName("Edge Case: Validate invalid/non-existent QR code")
    void testValidateInvalidQrCode() throws Exception {
        mockMvc.perform(post("/api/tickets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidateTicketRequest("INVALID-QR-CODE-123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Ticket"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid ticket QR code")));
    }

    @Test
    @DisplayName("Feature 5: Real-time headcount vs capacity per event")
    void testRealTimeHeadcount() throws Exception {
        // Create Event with capacity 5
        FestEvent event = eventRepository.save(new FestEvent(null, "E-Sports League", "Gaming Tournament", "Lab 1", LocalDateTime.now().plusDays(4), 5, new BigDecimal("100.00")));

        // Create 2 Attendees & Tickets
        String att1Str = mockMvc.perform(post("/api/attendees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAttendeeRequest("Player 1", "p1@example.com", "111"))))
                .andReturn().getResponse().getContentAsString();
        Long att1Id = objectMapper.readTree(att1Str).get("data").get("id").asLong();

        String att2Str = mockMvc.perform(post("/api/attendees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAttendeeRequest("Player 2", "p2@example.com", "222"))))
                .andReturn().getResponse().getContentAsString();
        Long att2Id = objectMapper.readTree(att2Str).get("data").get("id").asLong();

        // Purchase 2 tickets
        String t1Str = mockMvc.perform(post("/api/tickets/purchase").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PurchaseTicketRequest(event.getId(), att1Id))))
                .andReturn().getResponse().getContentAsString();
        String qr1 = objectMapper.readTree(t1Str).get("data").get("qrCode").asText();

        mockMvc.perform(post("/api/tickets/purchase").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PurchaseTicketRequest(event.getId(), att2Id))));

        // Validate 1 ticket for check-in
        mockMvc.perform(post("/api/tickets/validate").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ValidateTicketRequest(qr1))));

        // Check Headcount API
        mockMvc.perform(get("/api/events/" + event.getId() + "/headcount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(event.getId()))
                .andExpect(jsonPath("$.data.capacity").value(5))
                .andExpect(jsonPath("$.data.totalTicketsIssued").value(2))
                .andExpect(jsonPath("$.data.currentHeadcount").value(1))
                .andExpect(jsonPath("$.data.remainingCapacity").value(3))
                .andExpect(jsonPath("$.data.occupancyPercentage").value(20.0));
    }

    @Test
    @DisplayName("PUT /api/events/{id}: Update Fest Event details")
    void testUpdateEvent() throws Exception {
        FestEvent event = eventRepository.save(new FestEvent(null, "Old Title", "Old Desc", "Room 1", LocalDateTime.now().plusDays(1), 10, new BigDecimal("100.00")));

        CreateEventRequest updateReq = new CreateEventRequest("New Title", "New Desc", "Main Hall", LocalDateTime.now().plusDays(2), 20, new BigDecimal("200.00"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/events/" + event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Title"))
                .andExpect(jsonPath("$.data.capacity").value(20));
    }

    @Test
    @DisplayName("DELETE /api/events/{id}: Delete Fest Event")
    void testDeleteEvent() throws Exception {
        FestEvent event = eventRepository.save(new FestEvent(null, "Event To Delete", "Desc", "Room 2", LocalDateTime.now().plusDays(1), 5, new BigDecimal("50.00")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(eventRepository.existsById(event.getId()));
    }

    @Test
    @DisplayName("PUT & DELETE /api/attendees/{id}: Update and Delete Attendee")
    void testUpdateAndDeleteAttendee() throws Exception {
        com.festpass.model.Attendee attendee = attendeeRepository.save(new com.festpass.model.Attendee(null, "John Doe", "john@example.com", "12345"));

        CreateAttendeeRequest updateReq = new CreateAttendeeRequest("John Updated", "john.updated@example.com", "99999");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/attendees/" + attendee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("John Updated"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/attendees/" + attendee.getId()))
                .andExpect(status().isOk());

        assertFalse(attendeeRepository.existsById(attendee.getId()));
    }

    @Test
    @DisplayName("PUT /api/tickets/{id}/cancel & DELETE /api/tickets/{id}")
    void testCancelAndDeleteTicket() throws Exception {
        FestEvent event = eventRepository.save(new FestEvent(null, "Concert", "Music", "Hall", LocalDateTime.now().plusDays(2), 10, new BigDecimal("50.00")));
        com.festpass.model.Attendee attendee = attendeeRepository.save(new com.festpass.model.Attendee(null, "Jane", "jane@example.com", "555"));

        String tktStr = mockMvc.perform(post("/api/tickets/purchase").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PurchaseTicketRequest(event.getId(), attendee.getId()))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(tktStr).get("data").get("ticketId").asLong();

        // Cancel ticket
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tickets/" + ticketId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Delete ticket
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/tickets/" + ticketId))
                .andExpect(status().isOk());

        assertFalse(ticketRepository.existsById(ticketId));
    }
}
