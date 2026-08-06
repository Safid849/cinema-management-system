package com.example.demo.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.cinema.dto.RoomDTO;
import com.example.demo.cinema.dto.RoomInputDTO;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  @Mock private RoomRepository roomRepository;

  @Test
  void listRooms_returnsMappedDTOs() {
    RoomService roomService = new RoomService(roomRepository);
    Room room = Room.builder().id(UUID.randomUUID()).number("A1").capacity(50).build();
    when(roomRepository.findAll()).thenReturn(List.of(room));

    List<RoomDTO> result = roomService.listRooms();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).number()).isEqualTo("A1");
    assertThat(result.get(0).capacity()).isEqualTo(50);
  }

  @Test
  void getRoomById_returnsDTO_whenRoomExists() {
    RoomService roomService = new RoomService(roomRepository);
    UUID id = UUID.randomUUID();
    Room room = Room.builder().id(id).number("B2").capacity(80).build();
    when(roomRepository.findById(id)).thenReturn(Optional.of(room));

    RoomDTO result = roomService.getRoomById(id);

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.number()).isEqualTo("B2");
  }

  @Test
  void getRoomById_throwsNotFound_whenRoomDoesNotExist() {
    RoomService roomService = new RoomService(roomRepository);
    UUID id = UUID.randomUUID();
    when(roomRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.getRoomById(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void upsert_savesAndReturnsMappedDTO() {
    RoomService roomService = new RoomService(roomRepository);
    RoomInputDTO input = new RoomInputDTO("C3", 100);
    Room saved = Room.builder().id(UUID.randomUUID()).number("C3").capacity(100).build();
    when(roomRepository.save(any(Room.class))).thenReturn(saved);

    RoomDTO result = roomService.upsert(input);

    assertThat(result.number()).isEqualTo("C3");
    assertThat(result.capacity()).isEqualTo(100);
    verify(roomRepository).save(any(Room.class));
  }
}
