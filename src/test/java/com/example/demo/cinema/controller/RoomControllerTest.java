package com.example.demo.cinema.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.cinema.dto.RoomDTO;
import com.example.demo.cinema.dto.RoomInputDTO;
import com.example.demo.cinema.security.CinemaUserDetailsService;
import com.example.demo.cinema.security.JwtService;
import com.example.demo.cinema.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
@Import(RoomControllerTest.MethodSecurityConfig.class)
class RoomControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private RoomService roomService;
  @MockBean private JwtService jwtService;
  @MockBean private CinemaUserDetailsService cinemaUserDetailsService;

  private static final UUID ROOM_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "CLIENT")
  void listRooms_returnsOkForAnyAuthenticatedRole() throws Exception {
    RoomDTO room = new RoomDTO(ROOM_ID, "A1", 50);
    when(roomService.listRooms()).thenReturn(List.of(room));

    mockMvc
        .perform(get("/rooms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].number").value("A1"))
        .andExpect(jsonPath("$[0].capacity").value(50));
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void getRoomById_returnsOk() throws Exception {
    RoomDTO room = new RoomDTO(ROOM_ID, "A1", 50);
    when(roomService.getRoomById(ROOM_ID)).thenReturn(room);

    mockMvc
        .perform(get("/rooms/{id}", ROOM_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value("A1"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void upsert_returnsOkForManager() throws Exception {
    RoomInputDTO input = new RoomInputDTO("B2", 80);
    RoomDTO created = new RoomDTO(ROOM_ID, "B2", 80);
    when(roomService.upsert(any(RoomInputDTO.class))).thenReturn(created);

    mockMvc
        .perform(
            put("/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value("B2"));
  }

  @Test
  @WithMockUser(roles = "CLIENT")
  void upsert_returnsForbiddenForClient() throws Exception {
    RoomInputDTO input = new RoomInputDTO("B2", 80);

    mockMvc
        .perform(
            put("/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "EMPLOYEE")
  void upsert_returnsForbiddenForEmployee() throws Exception {
    RoomInputDTO input = new RoomInputDTO("B2", 80);

    mockMvc
        .perform(
            put("/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isForbidden());
  }

  @EnableMethodSecurity
  static class MethodSecurityConfig {}
}
