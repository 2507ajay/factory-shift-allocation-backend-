package com.zenalyst.factory.controller;

import com.zenalyst.factory.domain.model.MachineAssignment;
import com.zenalyst.factory.domain.model.ShiftType;
import com.zenalyst.factory.dto.ManualOverrideRequest;
import com.zenalyst.factory.dto.ShiftCoverageRequest;
import com.zenalyst.factory.dto.ShiftCoverageResponse;
import com.zenalyst.factory.service.CoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/coverage")
@Tag(name = "Shift Coverage Optimization", description = "Solves the 5:45 AM line startup crisis balancing certifications, overtime caps, and fatigue")
@CrossOrigin(origins = "*")
public class CoverageController {

    private final CoverageService coverageService;

    public CoverageController(CoverageService coverageService) {
        this.coverageService = coverageService;
    }

    @PostMapping("/solve")
    @Operation(summary = "Execute Multi-Objective Shift Coverage Solver",
            description = "Runs the optimization engine to staff all 18 machines before the line starts, respecting certifications and protecting burned-out workers.")
    public ResponseEntity<ShiftCoverageResponse> solveCoverage(@RequestBody(required = false) ShiftCoverageRequest request) {
        if (request == null) {
            request = new ShiftCoverageRequest(ShiftType.MORNING, LocalDate.now(), false, false);
        }
        return ResponseEntity.ok(coverageService.solveShiftCoverage(request));
    }

    @PostMapping("/override")
    @Operation(summary = "Supervisor Manual Assignment Override",
            description = "Allows the supervisor to manually pin an operator to a machine. Automatically re-optimizes remaining machines.")
    public ResponseEntity<ShiftCoverageResponse> applyOverride(@Valid @RequestBody ManualOverrideRequest request) {
        return ResponseEntity.ok(coverageService.applyManualOverride(request));
    }

    @GetMapping("/current")
    @Operation(summary = "Get Current Shift Assignments",
            description = "Returns all active machine assignments for the given shift and date.")
    public ResponseEntity<List<MachineAssignment>> getCurrentAssignments(
            @RequestParam(defaultValue = "MORNING") ShiftType shift,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return ResponseEntity.ok(coverageService.getCurrentAssignments(shift, date));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset Absences to Baseline",
            description = "Restores all operators to present and re-solves baseline coverage.")
    public ResponseEntity<ShiftCoverageResponse> resetToBaseline(
            @RequestParam(defaultValue = "MORNING") ShiftType shift) {
        coverageService.resetAllAttendance();
        return ResponseEntity.ok(coverageService.solveShiftCoverage(
                new ShiftCoverageRequest(shift, LocalDate.now(), false, true)
        ));
    }
}
