package com.festpass.repository;

import com.festpass.model.Ticket;
import com.festpass.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByQrCode(String qrCode);
    long countByEventId(Long eventId);
    long countByEventIdAndIsUsedTrue(Long eventId);
    List<Ticket> findByEventId(Long eventId);
    List<Ticket> findByAttendeeId(Long attendeeId);
    boolean existsByQrCode(String qrCode);
}
