package com.zenalyst.factory.repository;

import com.zenalyst.factory.domain.model.AttendanceStatus;
import com.zenalyst.factory.domain.model.Operator;
import com.zenalyst.factory.domain.model.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    Optional<Operator> findByEmployeeCode(String employeeCode);
    List<Operator> findByPrimaryShift(ShiftType primaryShift);
    List<Operator> findByAttendanceStatus(AttendanceStatus status);

    @Query("SELECT DISTINCT o FROM Operator o LEFT JOIN FETCH o.certifications ORDER BY o.fullName ASC")
    List<Operator> findAllWithCertifications();

    @Query("SELECT DISTINCT o FROM Operator o LEFT JOIN FETCH o.certifications WHERE o.primaryShift = :shift")
    List<Operator> findAllWithCertificationsByShift(ShiftType shift);
}
