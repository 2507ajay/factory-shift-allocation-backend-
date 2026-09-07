# Zenalyst AI — Backend Assignment: "Who Runs The Machines Today"

> **"A factory runs three shifts across 18 machines. Every morning the supervisor finds out who has not turned up and has to cover every machine before the line starts, using whoever is on site. Only certified operators can run some of the machines, overtime costs real money and is capped, and the same four willing people end up covering everything and are quietly furious about it. Build the backend that does this job. Real life is messier than it sounds — handle as much of that mess as you think matters."**

---

## 1. Quick Start (Runs on Any Clean Machine)

This project is built with **Java 17** and **Spring Boot 3.2.x**, using an embedded H2 database and bundled Maven Wrapper (`mvnw` / `mvnw.cmd`). **No external database or pre-installed Maven is required.**

### Prerequisites
* **Java 17 LTS** or newer (`java -version`)

### Run the Application

```bash
# Clone and navigate into the project
cd project1

# On Linux / macOS:
./mvnw spring-boot:run

# On Windows (PowerShell or CMD):
.\mvnw.cmd spring-boot:run
```

Once started, open your browser:
* **Interactive Command Center Dashboard**: [http://localhost:8080](http://localhost:8080)
* **Interactive Swagger / OpenAPI UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **H2 In-Memory Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (`JDBC URL: jdbc:h2:mem:factorydb`, User: `sa`, Password: *blank*)

### Run the Test Suite

```bash
# On Linux / macOS:
./mvnw test

# On Windows:
.\mvnw.cmd test
```

---

## 2. The Core Problem & Architectural Design

### The 05:45 AM Supervisor Crisis
At 5:45 AM, attendance roll call reveals who didn't show up. The production line starts at 6:00 AM. 
The supervisor has 15 minutes to solve a high-stakes puzzle:
1. **Safety & Compliance**: Certain machines (e.g. 5-Axis CNCs, high-tonnage stamping presses, industrial fiber lasers) present lethal hazard and tight-tolerance failure risks; uncertified operators **cannot** touch them.
2. **Cost Control**: Overtime costs 1.5× to 2.0× base wage and is strictly capped by company policy and labor law (max 16h/week).
3. **The Human Element (The "Quietly Furious Four")**: In naive scheduling, supervisors default to calling the most cooperative, capable workers because they always answer the phone and rarely complain. Over weeks, these four accumulate 14+ OT hours, work 6-7 consecutive days, and burn out in silence.
4. **Real-Life Factory Chaos**: Shortages happen where the factory simply doesn't have enough certified operators present at 5:50 AM. The system cannot crash or throw a 500 error; it must provide a **triage and mitigation plan**.

---

## 3. How the Engine Works: Multi-Objective Solver

The backend employs a **Multi-Pass Priority & Heuristic Constraint Solver** (`AssignmentSolver.java`):

```
                       [ 05:45 AM Roll Call Input ]
                                    │
                                    ▼
                 ┌──────────────────────────────────────┐
                 │ PASS 1: Preserve Supervisor Pins     │
                 └──────────────────┬───────────────────┘
                                    ▼
                 ┌──────────────────────────────────────┐
                 │ PASS 2: Primary Scheduled Roster     │
                 │ (Assign present certified defaults)  │
                 └──────────────────┬───────────────────┘
                                    ▼
                 ┌──────────────────────────────────────┐
                 │ PASS 3: Intelligent Coverage Search  │
                 │ (Sort machines by Priority P1 to P4) │
                 └──────────────────┬───────────────────┘
                                    │
                 ┌──────────────────┴───────────────────┐
                 │   Candidate Scoring Function:        │
                 │   Score = - (W_fairness × Pen_fair)  │
                 │           - (W_cost × Pen_cost)      │
                 │           + (W_exp × Bonus_exp)      │
                 └──────────────────┬───────────────────┘
                                    ▼
                 ┌──────────────────────────────────────┐
                 │ PASS 4: Graceful Shortage Triage     │
                 │ (Cannibalize low-priority buffer     │
                 │  machines to save P1 bottlenecks)    │
                 └──────────────────┬───────────────────┘
                                    ▼
                 [ Output: Coverage Plan + Audit Trail ]
```

### Solving the "Four Willing People" Problem (`FairnessEvaluator.java`)
We introduce a dynamic **Anti-Exploitation Surcharge**:
* Every operator has a calculated **Burnout Index** ($0.0 - 1.0$) combining overtime load ratio, consecutive days worked, and historical emergency coverage count.
* When evaluating candidates for overtime or cross-shift coverage, operators with high willingness ($\ge 0.80$) receive an exponential fairness penalty:
  $$\text{Penalty} = \text{Burnout} \times 100 + \left(\frac{\text{OT}}{\text{Cap}}\right)^2 \times 150 + (\text{Willingness} \times 40 + \text{CoverageCount} \times 20) + \text{ConsecutiveDaysPenalty}$$
* **Result**: The solver actively bypasses the willing operators when other certified candidates or rested floaters exist. They are treated strictly as a **last-resort backstop**, accompanied by an explicit Burnout Warning alert.

### Handling Real-Life Shortages: Triage & Graceful Degradation
When absenteeism is severe (e.g. 5+ operators absent) and no unassigned certified operators remain:
* The solver identifies operators assigned to low-priority machines (e.g. **M17/M18 Carton Packer / Palletizer**, Priority 4) who hold higher certifications (e.g. **CNC 5-Axis**).
* It **reallocates** that operator to the Priority 1 bottleneck machine (`ONSITE_REALLOCATION`).
* It consciously idles the buffer machine and logs a structured `TriageAlert`:
  > *"Machine M17 temporarily idled to protect critical bottleneck M01. Operator Gavin Reed transferred. Downstream buffer will sustain packaging for 3.5 hours."*

---

## 4. Domain & Data Architecture

### The 18 Machines (4 Production Cells)
1. **Precision Machining** (Priority 1 Bottlenecks):
   * `M01`, `M02`: 5-Axis CNC Milling Center Alpha & Beta (`CNC_5AXIS`, Level 3 Critical)
   * `M03`, `M04`: Wire EDM Precision Stations 1 & 2 (`WIRE_EDM`, Level 3 Critical)
2. **Fabrication & Sheet Metal Forming** (Priority 1-3):
   * `M05`, `M06`: 400-Ton & 250-Ton Hydraulic Stamping Presses (`STAMPING_PRESS`, Level 2)
   * `M07`, `M08`: Industrial Fiber Laser Cutters 1 & 2 (`LASER_CUT`, Level 2)
   * `M09`: CNC Sheet Metal Press Brake (`STAMPING_PRESS`, Level 2)
3. **Automated Welding & Assembly** (Priority 2-3):
   * `M10`, `M11`: 6-Axis Robotic Arc Welding Cells 1 & 2 (`ROBOTIC_WELD`, Level 2)
   * `M12`, `M13`, `M14`: Structural Riveter, Torque Stations, Sub-Assembly Transfer (`ASSEMBLY_TECH`, Level 1)
4. **Finishing & Packaging** (Priority 3-4 Buffer):
   * `M15`, `M16`: Surface Coating Line & Optical Quality Scanner (`GENERAL_MACHINING`, Level 1)
   * `M17`, `M18`: High-Speed Carton Case Packer & Robotic Palletizer (`PACKAGING_AUTO`, Level 1)

### Workforce Structure (~55 Operators)
* **Morning Shift**: 18 regular machine operators + 2 on-site floaters.
* **Night Shift**: 14 operators finishing at 06:00 (eligible for 4h holdover overtime).
* **Afternoon Shift**: 15 off-duty operators (eligible for call-in overtime).
* **The "Four Willing People"**:
  1. `Dave Miller` (`OP-WILLING-1`): CNC Master, willingness: 0.95, OT: 14.5h, 6 consecutive days.
  2. `Carlos Ortiz` (`OP-WILLING-2`): Laser/Press Lead, willingness: 0.92, OT: 13.0h, 5 consecutive days.
  3. `Ananya Rao` (`OP-WILLING-3`): Robotic Weld Specialist, willingness: 0.90, OT: 12.0h, 5 consecutive days.
  4. `Elena Rostova` (`OP-WILLING-4`): EDM Precision Lead, willingness: 0.96, OT: 15.0h (1h from cap!), 6 consecutive days.

---

## 5. REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/coverage/solve` | Solves machine coverage for a given shift and date. Supports `simulateMorningCrisis: true` |
| `POST` | `/api/coverage/override` | Applies a manual supervisor assignment lock and automatically re-balances remaining machines |
| `GET` | `/api/coverage/current` | Returns current active machine assignments and triage logs |
| `POST` | `/api/coverage/reset` | Resets all attendance to baseline and clears overrides |
| `GET` | `/api/machines` | Returns all 18 machines with required certification levels and priority |
| `GET` | `/api/operators` | Returns complete operator roster with certifications, attendance, and burnout metrics |
| `GET` | `/api/operators/willing` | Dedicated audit endpoint tracking the "Quietly Furious Four" protection status |
| `PATCH`| `/api/operators/{code}/attendance`| Updates operator attendance (e.g. `ABSENT_UNPLANNED`, `PRESENT`) |

Full interactive documentation is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

---

## 6. Assumptions Made

1. **Statutory & Policy Overtime Caps**:
   * Hard weekly overtime cap is set to **16.0 hours per operator per week**.
   * Maximum continuous shift duration is **12.0 hours** (8h regular + 4h holdover).
   * Minimum mandatory rest period between shifts is **8.0 hours**.
2. **Compensation Rules**:
   * Standard overtime multiplier is **1.5× base wage**.
   * 7th consecutive day / rest-day call-in triggers double-time (**2.0× base wage**).
3. **Machine Staffing**:
   * Each of the 18 machines requires 1 primary qualified operator to run safely at line startup.
4. **Shift Structure**:
   * Morning: 06:00 – 14:00
   * Afternoon: 14:00 – 22:00
   * Night: 22:00 – 06:00
   * Pre-shift attendance roll call occurs at 05:45 AM (15 minutes prior to startup).

---

## 7. What Was Chosen to Leave Out (and Why)

1. **Hardware-Level Biometric / Turnstile Integration**:
   * *Why*: Real factories have proprietary badge readers (HID, Honeywell) or RFID turnstiles. In this assignment, attendance roll call is modeled cleanly through REST API events and simulated crisis triggers (`PATCH /api/operators/{code}/attendance`), allowing easy testing and integration with any external HR/timeclock webhook.
2. **Full ERP / Payroll Ledger Accounting**:
   * *Why*: The system calculates precise dollar costs, overtime hours, and multipliers for every assignment, but does not write GL entries into SAP or NetSuite. That is the domain of back-office payroll batch jobs, whereas this system is laser-focused on the line-startup operational crisis.
3. **Heavy Meta-Heuristic Solvers (e.g., Simulated Annealing / Genetic Algorithms)**:
   * *Why*: At 5:45 AM with the line starting in 15 minutes, a supervisor cannot wait 45 seconds for a non-deterministic stochastic solver to converge. Our multi-pass priority search with penalty heuristics executes deterministically in **under 15 milliseconds**, produces an explainable audit trail, and guarantees that hard safety/certification rules are never violated.

---

## 8. Verification & Test Coverage

Automated tests in `src/test/java` cover all critical operational scenarios:
* **`ConstraintValidatorTest`**: Verifies uncertified workers are strictly disqualified from Level 2/3 machines, verifies 16h weekly OT caps, and enforces mandatory 8h rest intervals.
* **`FairnessAndBurnoutTest`**: Proves that the "Willing Four" receive high fairness penalties and that the engine chooses rested alternatives (e.g. Nathan Drake over Dave Miller).
* **`ShortageTriageTest`**: Verifies graceful triage when shortages occur, cannibalizing low-priority buffer stations to save critical bottlenecks.
* **`CoverageControllerIntegrationTest`**: Full end-to-end HTTP integration tests for `/api/coverage/solve`, `/api/coverage/override`, and `/api/machines`.

Run tests anytime with:
```bash
./mvnw test
```
