package com.example.demo.cinema.repository;

import com.example.demo.cinema.entity.Reservation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

  List<Reservation> findByUserId(UUID userId);

  /** Every seat already sold for a projection, whichever reservation holds it. */
  @Query("select s.id from Reservation r join r.seats s where r.projection.id = :projectionId")
  Set<UUID> findBookedSeatIds(@Param("projectionId") UUID projectionId);
}
