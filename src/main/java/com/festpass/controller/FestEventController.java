package com.festpass.controller;

import com.festpass.dto.ApiResponse;
import com.festpass.dto.CreateEventRequest;
import com.festpass.dto.EventHeadcountResponse;
import com.festpass.model.FestEvent;
import com.festpass.service.FestEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Fest Events", description = "Endpoints for managing fest events, capacity, and real-time headcount")
public class FestEventController {

    private final FestEventService eventService;

    public FestEventController(FestEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Core Feature 1: Create fest events with capacity and ticket price")
    public ResponseEntity<ApiResponse<FestEvent>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        FestEvent createdEvent = eventService.createEvent(request);
        return new ResponseEntity<>(
                ApiResponse.success("Fest event created successfully", createdEvent),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @Operation(summary = "Get all events (with optional pagination and sorting)")
    public ResponseEntity<ApiResponse<?>> getAllEvents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        if (page != null && size != null) {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Page<FestEvent> eventPage = eventService.getAllEvents(PageRequest.of(page, size, sort));
            return ResponseEntity.ok(ApiResponse.success("Events retrieved successfully", eventPage));
        }

        List<FestEvent> events = eventService.getAllEvents();
        return ResponseEntity.ok(ApiResponse.success("Events retrieved successfully", events));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<ApiResponse<FestEvent>> getEventById(@PathVariable Long id) {
        FestEvent event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Event details retrieved", event));
    }

    @GetMapping("/{id}/headcount")
    @Operation(summary = "Core Feature 5: View real-time headcount versus capacity per event")
    public ResponseEntity<ApiResponse<EventHeadcountResponse>> getHeadcount(@PathVariable Long id) {
        EventHeadcountResponse headcount = eventService.getHeadcount(id);
        return ResponseEntity.ok(ApiResponse.success("Real-time event headcount retrieved", headcount));
    }
}
