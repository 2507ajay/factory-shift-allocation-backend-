package com.zenalyst.factory.service;

import com.zenalyst.factory.domain.model.*;
import com.zenalyst.factory.dto.*;
import com.zenalyst.factory.engine.AssignmentSolver;
import com.zenalyst.factory.engine.CostCalculator;
import com.zenalyst.factory.repository.MachineAssignmentRepository;
import com.zenalyst.factory.repository.MachineRepository;
import com.zenalyst.factory.repository.OperatorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CoverageService {

    private final MachineRepository machineRepository;
    private final OperatorRepository operatorRepository;
    private final MachineAssignmentRepository assignmentRepository;
    private final AssignmentSolver assignmentSolver;
    private final CostCalculator costCalculator;
    private final OperatorService operatorService;

    public CoverageService(MachineRepository machineRepository,
                           OperatorRepository operatorRepository,
                           MachineAssignmentRepository assignmentRepository,
                           AssignmentSolver assignmentSolver,
                           CostCalculator costCalculator,
                           OperatorService operatorService) {
        this.machineRepository = machineRepository;
        this.operatorRepository = operatorRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSolver = assignmentSolver;
        this.costCalculator = costCalculator;
        this.operatorService = operatorService;
    }

    public ShiftCoverageResponse solveShiftCoverage(ShiftCoverageRequest request) {
        ShiftType shift = request.shiftType();
        LocalDate date = request.date();

        if (request.simulateMorningCrisis()) {
            applyMorningAbsenceScenario(shift);
        }

        if (request.resetOverrides()) {
            assignmentRepository.deleteByShiftDateAndShiftType(date, shift);
        } else {
            // Delete non-pinned assignments to allow re-solving around manual pins
            assignmentRepository.deleteByShiftDateAndShiftTypeAndPinnedFalse(date, shift);
        }

        List<Machine> machines = machineRepository.findByActiveTrueOrderByPriorityRankAscCodeAsc();
        List<Operator> operators = operatorRepository.findAllWithCertifications();
        List<MachineAssignment> pinnedAssignments = assignmentRepository
                .findByShiftDateAndShiftTypeOrderByMachinePriorityAscMachineCodeAsc(date, shift)
                .stream().filter(MachineAssignment::isPinned).toList();

        // Execute optimization solver
        AssignmentSolver.SolverResult result = assignmentSolver.solve(
                shift, date, machines, operators, pinnedAssignments
        );

        // Save newly generated non-pinned assignments
        for (MachineAssignment ma : result.assignments()) {
            if (!ma.isPinned()) {
                assignmentRepository.save(ma);
            }
        }

        List<WillingOperatorStatusDto> willingStatus = operatorService.getWillingOperatorsReport();

        String summary = String.format(
                "Coverage solved for %s on %s. %d of %d machines operational. Labor Cost: $%.2f (Reg: $%.2f, OT: $%.2f). Avg Fatigue: %.2f.",
                shift.getDisplayName(), date, result.assignments().size(), machines.size(),
                result.totalCost(), result.totalRegularCost(), result.totalOvertimeCost(),
                result.averageFatigueIndex()
        );

        return new ShiftCoverageResponse(
                UUID.randomUUID().toString(),
                shift,
                date,
                LocalDateTime.now(),
                machines.size(),
                result.assignments().size(),
                result.unassignedMachines().size(),
                result.assignments(),
                result.unassignedMachines(),
                result.triageAlerts(),
                willingStatus,
                result.totalRegularCost(),
                result.totalOvertimeCost(),
                result.totalCost(),
                result.totalOvertimeHours(),
                result.averageFatigueIndex(),
                summary
        );
    }

    public ShiftCoverageResponse applyManualOverride(ManualOverrideRequest request) {
        Machine machine = machineRepository.findByCode(request.machineCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid machine code: " + request.machineCode()));
        Operator operator = operatorRepository.findByEmployeeCode(request.operatorEmployeeCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid operator code: " + request.operatorEmployeeCode()));

        ShiftType shift = request.shiftType();
        LocalDate date = request.date();

        // Calculate cost for override
        double regHours = (operator.getPrimaryShift() == shift) ? 8.0 : 0.0;
        double otHours = (operator.getPrimaryShift() == shift) ? 0.0 : 8.0;
        var cost = costCalculator.calculateCost(operator, AssignmentType.SUPERVISOR_OVERRIDE, regHours, otHours);

        // Remove any existing assignment for this machine on this shift
        assignmentRepository.findByShiftDateAndShiftTypeAndMachineCode(date, shift, machine.getCode())
                .ifPresent(assignmentRepository::delete);

        MachineAssignment overrideAssignment = new MachineAssignment(
                machine, operator, shift, date,
                AssignmentType.SUPERVISOR_OVERRIDE,
                regHours, otHours, cost.totalCost(),
                "Supervisor manual override: " + (request.supervisorNotes() != null ? request.supervisorNotes() : "Direct assignment"),
                request.lockAssignment()
        );
        assignmentRepository.save(overrideAssignment);

        // Re-solve the remaining machines around this pinned override
        return solveShiftCoverage(new ShiftCoverageRequest(shift, date, false, false));
    }

    @Transactional(readOnly = true)
    public List<MachineAssignment> getCurrentAssignments(ShiftType shift, LocalDate date) {
        return assignmentRepository.findByShiftDateAndShiftTypeOrderByMachinePriorityAscMachineCodeAsc(date, shift);
    }

    public void applyMorningAbsenceScenario(ShiftType shift) {
        // Realistic 5:45 AM factory chaos: 5 morning operators no-show/call-in sick
        // Specifically targeting key machines (M01 CNC, M05 Stamping, M10 Robotic Weld)
        // to test the solver's skill matching and anti-burnout algorithms.
        List<String> noShowCodes = List.of("OP-M01", "OP-M05", "OP-M10", "OP-M13", "OP-M16");
        for (String code : noShowCodes) {
            operatorRepository.findByEmployeeCode(code).ifPresent(op -> {
                op.setAttendanceStatus(AttendanceStatus.ABSENT_UNPLANNED);
                op.setNotes("Morning call-in: Unplanned medical/personal absence.");
                operatorRepository.save(op);
            });
        }
    }

    public void resetAllAttendance() {
        List<Operator> all = operatorRepository.findAll();
        for (Operator op : all) {
            op.setAttendanceStatus(AttendanceStatus.PRESENT);
            op.setNotes(null);
            operatorRepository.save(op);
        }
    }
}
