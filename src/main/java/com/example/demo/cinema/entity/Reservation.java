package com.example.demo.cinema.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "projection_id", nullable = false)
  private Projection projection;

  @ManyToMany
  @JoinTable(
      name = "reservation_seats",
      joinColumns = @JoinColumn(name = "reservation_id"),
      inverseJoinColumns = @JoinColumn(name = "seat_id"))
  private Set<Seat> seats;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;
}
