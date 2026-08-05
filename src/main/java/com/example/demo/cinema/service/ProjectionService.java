package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.mapper.ProjectionMapper;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.cinema.repository.ProjectionRepository;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProjectionService {

    private final ProjectionRepository projectionRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;

    public List<ProjectionDTO> findAll() {
        return projectionRepository.findAll().stream().map(ProjectionMapper::toDTO).toList();
    }

    public ProjectionDTO create(ProjectionInputDTO input) {
        Movie movie =
                movieRepository
                        .findById(input.movieId())
                        .orElseThrow(() -> new NotFoundException("Movie " + input.movieId()));
        Room room =
                roomRepository
                        .findById(input.roomId())
                        .orElseThrow(() -> new NotFoundException("Room " + input.roomId()));

        Projection projection =
                Projection.builder()
                        .movie(movie)
                        .room(room)
                        .datetime(input.datetime())
                        .seatPrice(input.seatPrice())
                        .build();
        return ProjectionMapper.toDTO(projectionRepository.save(projection));
    }
}
