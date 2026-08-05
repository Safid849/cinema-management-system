package com.example.demo.cinema.repository;

import com.example.demo.cinema.entity.Movie;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, UUID> {}
