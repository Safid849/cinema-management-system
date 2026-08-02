package com.example.demo.cinema.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private int capacity;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Seat> seats;

  @OneToMany(mappedBy = "room")
  private List<Projection> projections;
}
