package com.example.demo.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

  @Mock private SeatRepository seatRepository;
  @Mock private RoomRepository roomRepository;

  @Test
  void listByRoom_returnsMappedSeatsForGivenRoom() {
    SeatService seatService = new SeatService(seatRepository, roomRepository);
    UUID roomId = UUID.randomUUID();
    Room room = Room.builder().id(roomId).number("A").capacity(50).build();
    Seat seat1 = Seat.builder().id(UUID.randomUUID()).number("A1").room(room).build();
    Seat seat2 = Seat.builder().id(UUID.randomUUID()).number("A2").room(room).build();
    when(roomRepository.existsById(roomId)).thenReturn(true);
    when(seatRepository.findByRoomId(roomId)).thenReturn(List.of(seat1, seat2));

    List<SeatDTO> result = seatService.listByRoom(roomId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).number()).isEqualTo("A1");
    assertThat(result.get(0).roomId()).isEqualTo(roomId);
    assertThat(result.get(1).number()).isEqualTo("A2");
  }

  @Test
  void listByRoom_returnsEmptyList_whenRoomHasNoSeats() {
    SeatService seatService = new SeatService(seatRepository, roomRepository);
    UUID roomId = UUID.randomUUID();
    when(roomRepository.existsById(roomId)).thenReturn(true);
    when(seatRepository.findByRoomId(roomId)).thenReturn(List.of());

    List<SeatDTO> result = seatService.listByRoom(roomId);

    assertThat(result).isEmpty();
  }

  @Test
  void listByRoom_throwsNotFound_whenRoomDoesNotExist() {
    SeatService seatService = new SeatService(seatRepository, roomRepository);
    UUID roomId = UUID.randomUUID();
    when(roomRepository.existsById(roomId)).thenReturn(false);

    assertThatThrownBy(() -> seatService.listByRoom(roomId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(roomId.toString());
  }
}
