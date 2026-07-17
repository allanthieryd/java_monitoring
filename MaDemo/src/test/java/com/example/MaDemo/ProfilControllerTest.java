package com.example.MaDemo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.controller.ProfilController;
import com.example.dto.ProfilDto;
import com.example.exception.ResourceNotFoundException;
import com.example.securite.JwtUtil;
import com.example.service.ProfilService;

@WebMvcTest(ProfilController.class)
class ProfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfilService profilService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "USER")
    void listProfils_shouldReturn200() throws Exception {
        when(profilService.listProfils()).thenReturn(List.of(profilDto(1L, "Alice")));

        mockMvc.perform(get("/api/v1/profils"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProfil_shouldReturn200_whenFound() throws Exception {
        when(profilService.getProfil(1L)).thenReturn(profilDto(1L, "Alice"));

        mockMvc.perform(get("/api/v1/profils/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProfil_shouldReturn404_whenNotFound() throws Exception {
        when(profilService.getProfil(99L)).thenThrow(new ResourceNotFoundException("Profil introuvable avec id 99"));

        mockMvc.perform(get("/api/v1/profils/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProfil_shouldReturn201() throws Exception {
        ProfilDto created = profilDto(5L, "Bob");
        when(profilService.createProfil(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/profils")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"email\":\"bob@test.fr\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProfil_shouldReturn400_whenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/profils")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteProfil_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/profils/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteProfil_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Profil introuvable avec id 99"))
                .when(profilService).deleteProfil(eq(99L));

        mockMvc.perform(delete("/api/v1/profils/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listProfils_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/profils"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private ProfilDto profilDto(Long id, String name) {
        ProfilDto dto = new ProfilDto();
        dto.setId(id);
        dto.setName(name);
        dto.setEmail(name.toLowerCase() + "@test.fr");
        return dto;
    }
}
