package com.zenalyst.factory.service;

import com.zenalyst.factory.domain.model.AttendanceStatus;
import com.zenalyst.factory.domain.model.Operator;
import com.zenalyst.factory.domain.model.ShiftType;
import com.zenalyst.factory.dto.WillingOperatorStatusDto;
import com.zenalyst.factory.repository.OperatorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OperatorService {

    private final OperatorRepository operatorRepository;

    public OperatorService(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    @Transactional(readOnly = true)
    public List<Operator> getAllOperators() {
        return operatorRepository.findAllWithCertifications();
    }

    @Transactional(readOnly = true)
    public List<Operator> getOperatorsByShift(ShiftType shift) {
        return operatorRepository.findAllWithCertificationsByShift(shift);
    }

    @Transactional(readOnly = true)
    public Operator getOperatorByCode(String code) {
        return operatorRepository.findByEmployeeCode(code)
                .orElseThrow(() -> new NoSuchElementException("Operator not found with code: " + code));
    }

    public Operator updateAttendance(String code, AttendanceStatus status, String notes) {
        Operator op = getOperatorByCode(code);
        op.setAttendanceStatus(status);
        if (notes != null && !notes.isBlank()) {
            op.setNotes(notes);
        }
        return operatorRepository.save(op);
    }

    @Transactional(readOnly = true)
    public List<WillingOperatorStatusDto> getWillingOperatorsReport() {
        List<Operator> operators = operatorRepository.findAllWithCertifications();
        List<WillingOperatorStatusDto> report = new ArrayList<>();

        for (Operator op : operators) {
            if (op.getWillingnessScore() >= 0.85) {
                double burnout = op.calculateBurnoutIndex();
                String status;
                String msg;

                if (burnout >= 0.70) {
                    status = "PROTECTED_FROM_OVERLOAD";
                    msg = String.format("High historical willingness (%.2f) and elevated burnout (%.2f). System algorithmically protects operator from further overtime.",
                            op.getWillingnessScore(), burnout);
                } else if (op.getAttendanceStatus() == AttendanceStatus.PRESENT) {
                    status = "WORKING_REGULAR";
                    msg = "Working standard assigned shift. No overtime abuse detected.";
                } else {
                    status = "RESTING_OFF_DUTY";
                    msg = "Off-duty on scheduled rest interval. Preserving recovery time.";
                }

                report.add(new WillingOperatorStatusDto(
                        op.getEmployeeCode(),
                        op.getFullName(),
                        op.getWillingnessScore(),
                        op.getWeeklyOvertimeHoursWorked(),
                        burnout,
                        status,
                        msg
                ));
            }
        }
        return report;
    }
}
