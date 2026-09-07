package com.zenalyst.factory.controller;

import com.zenalyst.factory.domain.model.Operator;
import com.zenalyst.factory.domain.model.ShiftType;
import com.zenalyst.factory.dto.AttendanceUpdateRequest;
import com.zenalyst.factory.dto.WillingOperatorStatusDto;
import com.zenalyst.factory.service.OperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
@Tag(name = "Workforce & Attendance Roster", description = "Operator profiles, certifications, attendance toggles, and fatigue indices")
@CrossOrigin(origins = "*")
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @GetMapping
    @Operation(summary = "Get All Operators", description = "Returns full workforce roster with certifications, weekly OT, and burnout scores.")
    public ResponseEntity<List<Operator>> getAllOperators(@RequestParam(required = false) ShiftType shift) {
        if (shift != null) {
            return ResponseEntity.ok(operatorService.getOperatorsByShift(shift));
        }
        return ResponseEntity.ok(operatorService.getAllOperators());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get Operator by Code", description = "Fetches a specific operator profile.")
    public ResponseEntity<Operator> getOperatorByCode(@PathVariable String code) {
        return ResponseEntity.ok(operatorService.getOperatorByCode(code));
    }

    @PatchMapping("/{code}/attendance")
    @Operation(summary = "Update Operator Attendance", description = "Updates real-time attendance status (e.g. ABSENT_UNPLANNED or PRESENT).")
    public ResponseEntity<Operator> updateAttendance(@PathVariable String code,
                                                    @Valid @RequestBody AttendanceUpdateRequest request) {
        return ResponseEntity.ok(operatorService.updateAttendance(code, request.status(), request.reason()));
    }

    @GetMapping("/willing")
    @Operation(summary = "Monitor The 'Four Willing People'",
            description = "Provides dedicated audit metrics demonstrating that overworked, cooperative operators are protected from chronic burnout.")
    public ResponseEntity<List<WillingOperatorStatusDto>> getWillingOperators() {
        return ResponseEntity.ok(operatorService.getWillingOperatorsReport());
    }
}
