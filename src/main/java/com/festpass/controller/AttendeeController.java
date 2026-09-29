package com.festpass.controller;

import com.festpass.dto.ApiResponse;
import com.festpass.dto.CreateAttendeeRequest;
import com.festpass.model.Attendee;
import com.festpass.service.AttendeeService;
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
@RequestMapping("/api/attendees")
@Tag(name = "Attendees", description = "Endpoints for managing fest attendees")
public class AttendeeController {

    private final AttendeeService attendeeService;

    public AttendeeController(AttendeeService attendeeService) {
        this.attendeeService = attendeeService;
    }

    @PostMapping
    @Operation(summary = "Register a new attendee")
    public ResponseEntity<ApiResponse<Attendee>> createAttendee(@Valid @RequestBody CreateAttendeeRequest request) {
        Attendee attendee = attendeeService.createAttendee(request);
        return new ResponseEntity<>(
                ApiResponse.success("Attendee registered successfully", attendee),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @Operation(summary = "Get all attendees (with optional pagination)")
    public ResponseEntity<ApiResponse<?>> getAllAttendees(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        if (page != null && size != null) {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Page<Attendee> attendeePage = attendeeService.getAllAttendees(PageRequest.of(page, size, sort));
            return ResponseEntity.ok(ApiResponse.success("Attendees retrieved successfully", attendeePage));
        }

        List<Attendee> attendees = attendeeService.getAllAttendees();
        return ResponseEntity.ok(ApiResponse.success("Attendees retrieved successfully", attendees));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attendee by ID")
    public ResponseEntity<ApiResponse<Attendee>> getAttendeeById(@PathVariable Long id) {
        Attendee attendee = attendeeService.getAttendeeById(id);
        return ResponseEntity.ok(ApiResponse.success("Attendee details retrieved", attendee));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update attendee details")
    public ResponseEntity<ApiResponse<Attendee>> updateAttendee(@PathVariable Long id, @Valid @RequestBody CreateAttendeeRequest request) {
        Attendee updatedAttendee = attendeeService.updateAttendee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Attendee updated successfully", updatedAttendee));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an attendee by ID")
    public ResponseEntity<ApiResponse<Void>> deleteAttendee(@PathVariable Long id) {
        attendeeService.deleteAttendee(id);
        return ResponseEntity.ok(ApiResponse.success("Attendee deleted successfully", null));
    }
}
