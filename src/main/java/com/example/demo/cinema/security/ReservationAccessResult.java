package com.example.demo.cinema.security;

public sealed interface ReservationAccessResult {

    record Granted() implements ReservationAccessResult {}

    record Denied(String reason) implements ReservationAccessResult {}
}

