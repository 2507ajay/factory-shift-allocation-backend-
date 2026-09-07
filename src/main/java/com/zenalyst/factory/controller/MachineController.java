package com.zenalyst.factory.controller;

import com.zenalyst.factory.domain.model.Machine;
import com.zenalyst.factory.service.MachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@Tag(name = "Factory Equipment", description = "The 18 factory machines, required skill levels, cells, and priority rankings")
@CrossOrigin(origins = "*")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    @Operation(summary = "Get All 18 Factory Machines", description = "Returns machines ordered by operational priority (P1 bottlenecks to P4 buffers).")
    public ResponseEntity<List<Machine>> getAllMachines() {
        return ResponseEntity.ok(machineService.getAllMachines());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get Machine by Code", description = "Fetches machine specifications and required certification level.")
    public ResponseEntity<Machine> getMachineByCode(@PathVariable String code) {
        return ResponseEntity.ok(machineService.getMachineByCode(code));
    }
}
