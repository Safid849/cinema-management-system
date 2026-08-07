package com.example.demo.cinema.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.security.CinemaUserDetailsService;
import com.example.demo.cinema.security.JwtService;
import com.example.demo.cinema.service.SeatService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SeatController.class)
class SeatControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private SeatService seatService;
  @MockBean private JwtService jwtService;
  @MockBean private CinemaUserDetailsService cinemaUserDetailsService;

  private static final UUID ROOM_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "CLIENT")
  void listSeats_returnsOkWithSeatsForRoom() throws Exception {
    SeatDTO seat1 = new SeatDTO(UUID.randomUUID(), "A1", ROOM_ID);
    SeatDTO seat2 = new SeatDTO(UUID.randomUUID(), "A2", ROOM_ID);
    when(seatService.listByRoom(ROOM_ID)).thenReturn(List.of(seat1, seat2));

    mockMvc
        .perform(get("/rooms/{roomId}/seats", ROOM_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].number").value("A1"))
        .andExpect(jsonPath("$[1].number").value("A2"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void listSeats_returnsEmptyListWhenRoomHasNoSeats() throws Exception {
    when(seatService.listByRoom(ROOM_ID)).thenReturn(List.of());

    mockMvc
        .perform(get("/rooms/{roomId}/seats", ROOM_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
