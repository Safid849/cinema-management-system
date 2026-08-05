package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.dto.ReservationInputDTO;
import com.example.demo.cinema.security.CinemaUserPrincipal;
import com.example.demo.cinema.security.ReservationAccessPolicy;
import com.example.demo.cinema.security.ReservationAccessResult;
import com.example.demo.cinema.service.ReservationService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  public List<ReservationDTO> listReservations() {
    return reservationService.findAll();
  }

  @GetMapping("/reservations/{id}")
  public ReservationDTO getReservationById(
      @PathVariable UUID id, @AuthenticationPrincipal CinemaUserPrincipal caller) {
    ReservationDTO reservation = reservationService.findById(id);
    ReservationAccessResult access = ReservationAccessPolicy.check(reservation.userId(), caller);
    if (access instanceof ReservationAccessResult.Denied denied) {
      throw new AccessDeniedException(denied.reason());
    }
    return reservation;
  }

  @PutMapping("/reservations")
  public ResponseEntity<ReservationDTO> upsertReservation(@RequestBody ReservationInputDTO input) {
    return new ResponseEntity<>(reservationService.create(input), HttpStatus.OK);
  }
}
