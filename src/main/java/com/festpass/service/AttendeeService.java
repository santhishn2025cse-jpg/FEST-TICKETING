package com.festpass.service;

import com.festpass.dto.CreateAttendeeRequest;
import com.festpass.exception.ResourceNotFoundException;
import com.festpass.model.Attendee;
import com.festpass.repository.AttendeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttendeeService {

    private final AttendeeRepository attendeeRepository;

    public AttendeeService(AttendeeRepository attendeeRepository) {
        this.attendeeRepository = attendeeRepository;
    }

    @Transactional
    public Attendee createAttendee(CreateAttendeeRequest request) {
        if (attendeeRepository.existsByEmail(request.getEmail())) {
            return attendeeRepository.findByEmail(request.getEmail()).orElseGet(() -> saveNew(request));
        }
        return saveNew(request);
    }

    private Attendee saveNew(CreateAttendeeRequest request) {
        Attendee attendee = new Attendee();
        attendee.setName(request.getName());
        attendee.setEmail(request.getEmail());
        attendee.setPhone(request.getPhone());
        return attendeeRepository.save(attendee);
    }

    @Transactional(readOnly = true)
    public List<Attendee> getAllAttendees() {
        return attendeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Attendee> getAllAttendees(Pageable pageable) {
        return attendeeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Attendee getAttendeeById(Long id) {
        return attendeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendee not found with ID: " + id));
    }

    @Transactional
    public Attendee updateAttendee(Long id, CreateAttendeeRequest request) {
        Attendee attendee = getAttendeeById(id);
        attendee.setName(request.getName());
        attendee.setEmail(request.getEmail());
        attendee.setPhone(request.getPhone());
        return attendeeRepository.save(attendee);
    }

    @Transactional
    public void deleteAttendee(Long id) {
        Attendee attendee = getAttendeeById(id);
        attendeeRepository.delete(attendee);
    }
}
