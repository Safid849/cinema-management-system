package com.example.demo.cinema.security;

import com.example.demo.cinema.entity.Userrole;
import java.util.UUID;

/** GET /reservations/{id}: staff read anything, a client only reads their own booking. */
public final class ReservationAccessPolicy {

  private ReservationAccessPolicy() {}

  public static ReservationAccessResult check(UUID reservationOwnerId, CinemaUserPrincipal caller) {
    if (caller == null) {
      return new ReservationAccessResult.Denied("Authentication required");
    }
    if (caller.getRole() == Userrole.EMPLOYEE || caller.getRole() == Userrole.MANAGER) {
      return new ReservationAccessResult.Granted();
    }
    if (reservationOwnerId != null && reservationOwnerId.equals(caller.getId())) {
      return new ReservationAccessResult.Granted();
    }
    return new ReservationAccessResult.Denied(
        "Reservation owned by " + reservationOwnerId + " does not belong to " + caller.getId());
  }
}
