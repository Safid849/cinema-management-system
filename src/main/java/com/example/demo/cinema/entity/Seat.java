package com.example.demo.cinema.entity;

import jakarta.persistence.*;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private String number;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @ManyToMany(mappedBy = "seats")
  private Set<Reservation> reservations;
}
