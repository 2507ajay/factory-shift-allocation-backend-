package com.zenalyst.factory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenalyst.factory.domain.model.ShiftType;
import com.zenalyst.factory.dto.ManualOverrideRequest;
import com.zenalyst.factory.dto.ShiftCoverageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CoverageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/machines returns all 18 machines with priorities")
    void shouldReturnAll18Machines() throws Exception {
        mockMvc.perform(get("/api/machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[0].code", notNullValue()))
                .andExpect(jsonPath("$[0].priorityRank", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/operators/willing returns monitoring report on the Four Willing People")
    void shouldReturnWillingOperatorsReport() throws Exception {
        mockMvc.perform(get("/api/operators/willing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[*].fullName", hasItems("Dave Miller", "Carlos Ortiz", "Ananya Rao", "Elena Rostova")));
    }

    @Test
    @DisplayName("POST /api/coverage/solve solves morning crisis and covers the line")
    void shouldSolveMorningCrisis() throws Exception {
        ShiftCoverageRequest request = new ShiftCoverageRequest(ShiftType.MORNING, LocalDate.now(), true, false);

        mockMvc.perform(post("/api/coverage/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMachines", is(18)))
                .andExpect(jsonPath("$.coveredMachines", greaterThanOrEqualTo(16)))
                .andExpect(jsonPath("$.assignments", not(empty())))
                .andExpect(jsonPath("$.totalShiftLaborCost", greaterThan(0.0)));
    }

    @Test
    @DisplayName("POST /api/coverage/override applies manual supervisor lock and re-solves")
    void shouldApplyManualOverrideAndResolve() throws Exception {
        ManualOverrideRequest override = new ManualOverrideRequest(
                "M01", "OP-M02", ShiftType.MORNING, LocalDate.now(), true, "Supervisor testing pin"
        );

        mockMvc.perform(post("/api/coverage/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(override)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[?(@.machineCode == 'M01')].operatorEmployeeCode", contains("OP-M02")))
                .andExpect(jsonPath("$.assignments[?(@.machineCode == 'M01')].pinned", contains(true)));
    }
}
