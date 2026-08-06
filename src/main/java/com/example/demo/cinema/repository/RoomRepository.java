package com.example.demo.cinema.repository;

import com.example.demo.cinema.entity.Room;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

  Optional<Room> findByNumber(String number);
}
