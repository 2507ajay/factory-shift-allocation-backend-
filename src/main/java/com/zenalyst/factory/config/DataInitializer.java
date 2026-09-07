package com.zenalyst.factory.config;

import com.zenalyst.factory.domain.model.*;
import com.zenalyst.factory.repository.CertificationRepository;
import com.zenalyst.factory.repository.MachineRepository;
import com.zenalyst.factory.repository.OperatorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Initializes realistic baseline data for the factory:
 * - 18 machines across 4 cells with distinct safety/certification tiers.
 * - Standard technical certifications.
 * - 55-person workforce across Morning, Afternoon, and Night shifts.
 * - Seeds "The Four Willing People" with elevated burnout and overtime to demonstrate protection.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final MachineRepository machineRepository;
    private final CertificationRepository certificationRepository;
    private final OperatorRepository operatorRepository;

    public DataInitializer(MachineRepository machineRepository,
                           CertificationRepository certificationRepository,
                           OperatorRepository operatorRepository) {
        this.machineRepository = machineRepository;
        this.certificationRepository = certificationRepository;
        this.operatorRepository = operatorRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (machineRepository.count() > 0) {
            return; // Already initialized
        }

        LocalDate futureYear = LocalDate.now().plusYears(2);

        // 1. Technical Certifications
        Certification certCnc = certificationRepository.save(new Certification("CNC_5AXIS", "5-Axis Precision CNC Machining", CertificationLevel.LEVEL_3_CRITICAL, futureYear));
        Certification certEdm = certificationRepository.save(new Certification("WIRE_EDM", "Precision Wire Electrical Discharge Machining", CertificationLevel.LEVEL_3_CRITICAL, futureYear));
        Certification certPress = certificationRepository.save(new Certification("STAMPING_PRESS", "Heavy Hydraulic Stamping & Forming Press", CertificationLevel.LEVEL_2_STANDARD, futureYear));
        Certification certLaser = certificationRepository.save(new Certification("LASER_CUT", "Industrial Fiber Laser Cutting & Safety", CertificationLevel.LEVEL_2_STANDARD, futureYear));
        Certification certWeld = certificationRepository.save(new Certification("ROBOTIC_WELD", "6-Axis Robotic Arc & Laser Welding", CertificationLevel.LEVEL_2_STANDARD, futureYear));
        Certification certAssembly = certificationRepository.save(new Certification("ASSEMBLY_TECH", "Automated Fastening & Torque Calibration", CertificationLevel.LEVEL_1_BASIC, futureYear));
        Certification certPkg = certificationRepository.save(new Certification("PACKAGING_AUTO", "Packaging Line & Robotic Palletizer Operation", CertificationLevel.LEVEL_1_BASIC, futureYear));
        Certification certGen = certificationRepository.save(new Certification("GENERAL_MACHINING", "Standard Industrial Equipment & Lathes", CertificationLevel.LEVEL_1_BASIC, futureYear));

        // 2. The 18 Factory Machines across 4 Production Cells
        machineRepository.saveAll(List.of(
                // Precision Machining (P1 Bottlenecks)
                new Machine("M01", "5-Axis CNC Milling Center Alpha", ProductionCell.PRECISION_MACHINING, "CNC_5AXIS", CertificationLevel.LEVEL_3_CRITICAL, 1, 950.0),
                new Machine("M02", "5-Axis CNC Milling Center Beta", ProductionCell.PRECISION_MACHINING, "CNC_5AXIS", CertificationLevel.LEVEL_3_CRITICAL, 1, 950.0),
                new Machine("M03", "Wire EDM Precision Station 1", ProductionCell.PRECISION_MACHINING, "WIRE_EDM", CertificationLevel.LEVEL_3_CRITICAL, 1, 800.0),
                new Machine("M04", "Wire EDM Precision Station 2", ProductionCell.PRECISION_MACHINING, "WIRE_EDM", CertificationLevel.LEVEL_3_CRITICAL, 2, 750.0),

                // Fabrication & Forming
                new Machine("M05", "400-Ton Hydraulic Stamping Press A", ProductionCell.FABRICATION_FORMING, "STAMPING_PRESS", CertificationLevel.LEVEL_2_STANDARD, 1, 650.0),
                new Machine("M06", "250-Ton Hydraulic Stamping Press B", ProductionCell.FABRICATION_FORMING, "STAMPING_PRESS", CertificationLevel.LEVEL_2_STANDARD, 2, 550.0),
                new Machine("M07", "Fiber Laser Cutting System 1", ProductionCell.FABRICATION_FORMING, "LASER_CUT", CertificationLevel.LEVEL_2_STANDARD, 1, 700.0),
                new Machine("M08", "Fiber Laser Cutting System 2", ProductionCell.FABRICATION_FORMING, "LASER_CUT", CertificationLevel.LEVEL_2_STANDARD, 2, 650.0),
                new Machine("M09", "CNC Sheet Metal Press Brake", ProductionCell.FABRICATION_FORMING, "STAMPING_PRESS", CertificationLevel.LEVEL_2_STANDARD, 3, 450.0),

                // Assembly & Welding
                new Machine("M10", "Robotic Arc Welding Cell 1", ProductionCell.ASSEMBLY_WELDING, "ROBOTIC_WELD", CertificationLevel.LEVEL_2_STANDARD, 2, 500.0),
                new Machine("M11", "Robotic Arc Welding Cell 2", ProductionCell.ASSEMBLY_WELDING, "ROBOTIC_WELD", CertificationLevel.LEVEL_2_STANDARD, 2, 500.0),
                new Machine("M12", "Automated Structural Riveter", ProductionCell.ASSEMBLY_WELDING, "ASSEMBLY_TECH", CertificationLevel.LEVEL_1_BASIC, 3, 380.0),
                new Machine("M13", "Precision Torque Fastening Cell", ProductionCell.ASSEMBLY_WELDING, "ASSEMBLY_TECH", CertificationLevel.LEVEL_1_BASIC, 3, 350.0),
                new Machine("M14", "Sub-Assembly Transfer Station", ProductionCell.ASSEMBLY_WELDING, "ASSEMBLY_TECH", CertificationLevel.LEVEL_1_BASIC, 3, 350.0),

                // Finishing & Packaging
                new Machine("M15", "Automated Surface Coating Line", ProductionCell.FINISHING_PACKAGING, "GENERAL_MACHINING", CertificationLevel.LEVEL_1_BASIC, 3, 300.0),
                new Machine("M16", "Optical Quality Inspection Scanner", ProductionCell.FINISHING_PACKAGING, "GENERAL_MACHINING", CertificationLevel.LEVEL_1_BASIC, 3, 320.0),
                new Machine("M17", "High-Speed Carton Case Packer", ProductionCell.FINISHING_PACKAGING, "PACKAGING_AUTO", CertificationLevel.LEVEL_1_BASIC, 4, 200.0),
                new Machine("M18", "Robotic Palletizing Station", ProductionCell.FINISHING_PACKAGING, "PACKAGING_AUTO", CertificationLevel.LEVEL_1_BASIC, 4, 180.0)
        ));

        // 3. Seed Workforce: Morning Shift Regular Rostered Operators (M01 to M18)
        seedMorningRoster(certCnc, certEdm, certPress, certLaser, certWeld, certAssembly, certPkg, certGen);

        // 4. Seed "The Four Willing People" (The core prompt scenario)
        seedTheFourWillingPeople(certCnc, certEdm, certPress, certLaser, certWeld);

        // 5. Seed Preceding Night Shift Operators (Available for Holdover Overtime)
        seedNightShiftOperators(certCnc, certEdm, certPress, certLaser, certWeld, certAssembly, certPkg);

        // 6. Seed Afternoon Shift Operators (Eligible for Call-In Overtime)
        seedAfternoonShiftOperators(certCnc, certEdm, certPress, certLaser, certWeld, certAssembly, certPkg);
    }

    private void seedMorningRoster(Certification cnc, Certification edm, Certification press,
                                   Certification laser, Certification weld, Certification assem,
                                   Certification pkg, Certification gen) {

        createOp("OP-M01", "Marcus Vance", ShiftType.MORNING, "M01", 38.0, 0.45, 0.0, 0, cnc);
        createOp("OP-M02", "Sarah Jenkins", ShiftType.MORNING, "M02", 37.5, 0.50, 0.0, 0, cnc);
        createOp("OP-M03", "Leon Chen", ShiftType.MORNING, "M03", 36.0, 0.40, 2.0, 1, edm);
        createOp("OP-M04", "Rachel Adams", ShiftType.MORNING, "M04", 35.0, 0.45, 0.0, 0, edm);
        createOp("OP-M05", "Dmitri Volkov", ShiftType.MORNING, "M05", 33.0, 0.35, 1.5, 0, press);
        createOp("OP-M06", "Priya Sharma", ShiftType.MORNING, "M06", 32.5, 0.50, 0.0, 0, press);
        createOp("OP-M07", "Travis Scott", ShiftType.MORNING, "M07", 34.0, 0.40, 0.0, 0, laser);
        createOp("OP-M08", "Hannah Abbott", ShiftType.MORNING, "M08", 33.5, 0.45, 0.0, 0, laser);
        createOp("OP-M09", "Mateo Silva", ShiftType.MORNING, "M09", 30.0, 0.50, 0.0, 0, press, gen);
        createOp("OP-M10", "Kenji Sato", ShiftType.MORNING, "M10", 35.0, 0.40, 0.0, 0, weld);
        createOp("OP-M11", "Amira Hassan", ShiftType.MORNING, "M11", 34.5, 0.45, 0.0, 0, weld);
        createOp("OP-M12", "Chloe Dupont", ShiftType.MORNING, "M12", 29.0, 0.50, 0.0, 0, assem);
        createOp("OP-M13", "Aaron Brooks", ShiftType.MORNING, "M13", 28.5, 0.40, 0.0, 0, assem);
        createOp("OP-M14", "Zoe Taylor", ShiftType.MORNING, "M14", 28.5, 0.50, 0.0, 0, assem);
        createOp("OP-M15", "Felix Weber", ShiftType.MORNING, "M15", 27.5, 0.50, 0.0, 0, gen);
        createOp("OP-M16", "Maya Lin", ShiftType.MORNING, "M16", 28.0, 0.40, 0.0, 0, gen);
        // Note: OP-M17 and OP-M18 are cross-certified operators placed on packaging buffer stations
        createOp("OP-M17", "Gavin Reed", ShiftType.MORNING, "M17", 32.0, 0.60, 0.0, 0, pkg, cnc, laser);
        createOp("OP-M18", "Lydia Becker", ShiftType.MORNING, "M18", 31.0, 0.55, 0.0, 0, pkg, press, weld);

        // Morning Shift Floaters on site
        createOp("OP-FL-01", "Samira Khan", ShiftType.MORNING, null, 31.5, 0.60, 0.0, 0, assem, gen, pkg);
        createOp("OP-FL-02", "Tyler Ward", ShiftType.MORNING, null, 33.0, 0.55, 0.0, 0, press, laser);
    }

    /**
     * Seeds the iconic "Four Willing People" described in the prompt:
     * Highly versatile, cooperative (willingness >= 0.90), but already over-utilized
     * with 12-15 hours of OT this week and feeling burned out.
     */
    private void seedTheFourWillingPeople(Certification cnc, Certification edm,
                                         Certification press, Certification laser, Certification weld) {

        Operator dave = new Operator("OP-WILLING-1", "Dave Miller", ShiftType.NIGHT, null, 40.0, 0.95, 14.5, 5);
        dave.setConsecutiveDaysWorked(6);
        dave.setLastShiftEndTime(LocalDateTime.now().minusHours(8));
        dave.addCertification(cnc);
        dave.addCertification(laser);
        dave.setNotes("Key veteran. Historically says yes to every call-in. Currently near exhaustion.");
        operatorRepository.save(dave);

        Operator carlos = new Operator("OP-WILLING-2", "Carlos Ortiz", ShiftType.AFTERNOON, null, 38.0, 0.92, 13.0, 4);
        carlos.setConsecutiveDaysWorked(5);
        carlos.setLastShiftEndTime(LocalDateTime.now().minusHours(10));
        carlos.addCertification(press);
        carlos.addCertification(laser);
        carlos.setNotes("Always reliable. Overloaded with 13h OT this week. Needs protection.");
        operatorRepository.save(carlos);

        Operator ananya = new Operator("OP-WILLING-3", "Ananya Rao", ShiftType.AFTERNOON, null, 37.0, 0.90, 12.0, 4);
        ananya.setConsecutiveDaysWorked(5);
        ananya.setLastShiftEndTime(LocalDateTime.now().minusHours(10));
        ananya.addCertification(weld);
        ananya.addCertification(press);
        ananya.setNotes("Robotic welding expert. Quietly frustrated by constant emergency call-ins.");
        operatorRepository.save(ananya);

        Operator elena = new Operator("OP-WILLING-4", "Elena Rostova", ShiftType.NIGHT, null, 42.0, 0.96, 15.0, 6);
        elena.setConsecutiveDaysWorked(6);
        elena.setLastShiftEndTime(LocalDateTime.now().minusHours(8));
        elena.addCertification(edm);
        elena.addCertification(cnc);
        elena.setNotes("Master wire EDM operator. 15h OT accumulated out of 16h cap. Severely burned out.");
        operatorRepository.save(elena);
    }

    private void seedNightShiftOperators(Certification cnc, Certification edm, Certification press,
                                         Certification laser, Certification weld, Certification assem, Certification pkg) {

        // Fresh night shift operators who just finished at 6:00 AM, with LOW overtime hours (0 to 3h),
        // making them ideal, rested candidates for holdover overtime!
        createNightOp("OP-N-01", "Nathan Drake", 38.0, 0.65, 1.0, 0, cnc);
        createNightOp("OP-N-02", "Brooke Davis", 36.0, 0.60, 2.0, 0, edm);
        createNightOp("OP-N-03", "Julian Alcantara", 34.0, 0.55, 0.0, 0, press);
        createNightOp("OP-N-04", "Kavita Nair", 35.0, 0.60, 0.0, 0, laser);
        createNightOp("OP-N-05", "Brian O'Connor", 36.0, 0.65, 1.5, 0, weld);
        createNightOp("OP-N-06", "Siddharth Roy", 29.0, 0.50, 0.0, 0, assem);
        createNightOp("OP-N-07", "Nina Petrenko", 30.0, 0.55, 0.0, 0, pkg);
    }

    private void seedAfternoonShiftOperators(Certification cnc, Certification edm, Certification press,
                                             Certification laser, Certification weld, Certification assem, Certification pkg) {

        // Afternoon shift operators with low overtime, available for call-in
        createAfternoonOp("OP-A-01", "Oscar Wilde", 38.0, 0.60, 2.0, 0, cnc);
        createAfternoonOp("OP-A-02", "Deepak Gupta", 36.0, 0.55, 0.0, 0, edm);
        createAfternoonOp("OP-A-03", "Megan Foster", 34.0, 0.50, 0.0, 0, press);
        createAfternoonOp("OP-A-04", "Alan Turing", 35.0, 0.60, 0.0, 0, laser);
        createAfternoonOp("OP-A-05", "Clara Oswald", 36.0, 0.55, 1.0, 0, weld);
    }

    private void createOp(String code, String name, ShiftType shift, String defaultMachine,
                          double wage, double willingness, double ot, int histCoverage, Certification... certs) {
        Operator op = new Operator(code, name, shift, defaultMachine, wage, willingness, ot, histCoverage);
        op.setAttendanceStatus(AttendanceStatus.PRESENT);
        op.setConsecutiveDaysWorked(2);
        for (Certification c : certs) {
            op.addCertification(c);
        }
        operatorRepository.save(op);
    }

    private void createNightOp(String code, String name, double wage, double willingness,
                               double ot, int histCoverage, Certification... certs) {
        Operator op = new Operator(code, name, ShiftType.NIGHT, null, wage, willingness, ot, histCoverage);
        op.setAttendanceStatus(AttendanceStatus.PRESENT); // Finished shift, present at 06:00
        op.setConsecutiveDaysWorked(2);
        op.setLastShiftEndTime(LocalDateTime.now().minusHours(8));
        for (Certification c : certs) {
            op.addCertification(c);
        }
        operatorRepository.save(op);
    }

    private void createAfternoonOp(String code, String name, double wage, double willingness,
                                   double ot, int histCoverage, Certification... certs) {
        Operator op = new Operator(code, name, ShiftType.AFTERNOON, null, wage, willingness, ot, histCoverage);
        op.setAttendanceStatus(AttendanceStatus.SCHEDULED);
        op.setConsecutiveDaysWorked(2);
        op.setLastShiftEndTime(LocalDateTime.now().minusHours(14)); // Well-rested (14h rest)
        for (Certification c : certs) {
            op.addCertification(c);
        }
        operatorRepository.save(op);
    }
}
