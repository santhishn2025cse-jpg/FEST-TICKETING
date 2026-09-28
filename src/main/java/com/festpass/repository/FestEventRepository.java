package com.festpass.repository;

import com.festpass.model.FestEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FestEventRepository extends JpaRepository<FestEvent, Long> {
}
