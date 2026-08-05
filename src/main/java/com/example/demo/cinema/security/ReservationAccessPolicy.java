package com.example.demo.cinema.security;

import com.example.demo.cinema.entity.Userrole;
import java.util.UUID;

public final class ReservationAccessPolicy {

  private ReservationAccessPolicy() {}

  public static ReservationAccessResult check(UUID reservationOwnerId, CinemaUserPrincipal caller) {
    if (caller.getRole() == Userrole.EMPLOYEE || caller.getRole() == Userrole.MANAGER) {
      return new ReservationAccessResult.Granted();
    }
    if (reservationOwnerId.equals(caller.getId())) {
      return new ReservationAccessResult.Granted();
    }
    return new ReservationAccessResult.Denied(
        "Reservation owned by " + reservationOwnerId + " does not belong to " + caller.getId());
  }
}
