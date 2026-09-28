package com.festpass.service;

import com.festpass.dto.CreateEventRequest;
import com.festpass.dto.EventHeadcountResponse;
import com.festpass.exception.ResourceNotFoundException;
import com.festpass.model.FestEvent;
import com.festpass.repository.FestEventRepository;
import com.festpass.repository.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FestEventService {

    private final FestEventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public FestEventService(FestEventRepository eventRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public FestEvent createEvent(CreateEventRequest request) {
        FestEvent event = new FestEvent();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        event.setCapacity(request.getCapacity());
        event.setTicketPrice(request.getTicketPrice());
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<FestEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<FestEvent> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public FestEvent getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FestEvent not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public EventHeadcountResponse getHeadcount(Long eventId) {
        FestEvent event = getEventById(eventId);
        long totalIssued = ticketRepository.countByEventId(eventId);
        long checkedInCount = ticketRepository.countByEventIdAndIsUsedTrue(eventId);

        return new EventHeadcountResponse(
                event.getId(),
                event.getName(),
                event.getCapacity(),
                totalIssued,
                checkedInCount
        );
    }
}
