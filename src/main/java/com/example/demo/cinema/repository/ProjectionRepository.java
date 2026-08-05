package com.example.demo.cinema.repository;

import com.example.demo.cinema.entity.Projection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectionRepository extends JpaRepository<Projection, UUID> {}
