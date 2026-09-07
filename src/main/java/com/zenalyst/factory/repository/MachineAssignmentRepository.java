package com.zenalyst.factory.repository;

import com.zenalyst.factory.domain.model.MachineAssignment;
import com.zenalyst.factory.domain.model.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineAssignmentRepository extends JpaRepository<MachineAssignment, Long> {
    List<MachineAssignment> findByShiftDateAndShiftTypeOrderByMachinePriorityAscMachineCodeAsc(LocalDate shiftDate, ShiftType shiftType);
    Optional<MachineAssignment> findByShiftDateAndShiftTypeAndMachineCode(LocalDate shiftDate, ShiftType shiftType, String machineCode);
    void deleteByShiftDateAndShiftTypeAndPinnedFalse(LocalDate shiftDate, ShiftType shiftType);
    void deleteByShiftDateAndShiftType(LocalDate shiftDate, ShiftType shiftType);
}
