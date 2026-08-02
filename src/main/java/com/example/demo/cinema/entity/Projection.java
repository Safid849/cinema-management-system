package com.example.demo.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projection {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_id", nullable = false)
  private Movie movie;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(nullable = false)
  private Instant datetime;

  @Column(nullable = false)
  private BigDecimal seatPrice;

  @OneToMany(mappedBy = "projection")
  private List<Reservation> reservations;
}
