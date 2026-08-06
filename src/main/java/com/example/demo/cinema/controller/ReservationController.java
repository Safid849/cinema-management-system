package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.dto.ReservationInputDTO;
import com.example.demo.cinema.security.CinemaUserPrincipal;
import com.example.demo.cinema.security.ReservationAccessPolicy;
import com.example.demo.cinema.security.ReservationAccessResult;
import com.example.demo.cinema.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  @GetMapping("/reservations")
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  public List<ReservationDTO> listReservations() {
    return reservationService.findAll();
  }

  /** A client's own reservations, so they have something to read without hitting /reservations. */
  @GetMapping("/reservations/me")
  public List<ReservationDTO> myReservations(@AuthenticationPrincipal CinemaUserPrincipal caller) {
    if (caller == null) {
      throw new AccessDeniedException("Authentication required");
    }
    return reservationService.findByUser(caller.getId());
  }

  @GetMapping("/reservations/{id}")
  public ReservationDTO getReservationById(
      @PathVariable UUID id, @AuthenticationPrincipal CinemaUserPrincipal caller) {
    ReservationDTO reservation = reservationService.findById(id);
    if (ReservationAccessPolicy.check(reservation.userId(), caller)
        instanceof ReservationAccessResult.Denied denied) {
      throw new AccessDeniedException(denied.reason());
    }
    return reservation;
  }

  @PutMapping("/reservations")
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  public ReservationDTO upsertReservation(@Valid @RequestBody ReservationInputDTO input) {
    return reservationService.upsert(input);
  }

  @DeleteMapping("/reservations/{id}")
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  public ResponseEntity<Void> deleteReservation(@PathVariable UUID id) {
    reservationService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
