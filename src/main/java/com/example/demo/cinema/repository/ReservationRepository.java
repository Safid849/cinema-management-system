package com.example.demo.cinema.repository;

import com.example.demo.cinema.entity.Reservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {}
