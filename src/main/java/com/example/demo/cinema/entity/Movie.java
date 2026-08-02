package com.example.demo.cinema.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private String title;

  @ElementCollection(targetClass = Genre.class)
  @CollectionTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "genre")
  private Set<Genre> genre;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false)
  private Duration duration;

  @OneToMany(mappedBy = "movie")
  private List<Projection> projections;
}
