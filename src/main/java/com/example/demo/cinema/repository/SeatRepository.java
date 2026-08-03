package com.example.demo.cinema.repository;

import com.example.demo.cinema.entity.Seat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
  List<Seat> findByRoomId(UUID roomId);
}
