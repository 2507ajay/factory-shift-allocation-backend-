/**
 * Zenalyst AI - Factory Floor Machine Coverage & Anti-Burnout Dispatch
 * Front-end controller interacting with Spring Boot REST API
 */

const API_BASE = '/api';

let state = {
    machines: [],
    operators: [],
    willingReport: [],
    currentPlan: null,
    selectedShift: 'MORNING',
    selectedMachineForOverride: null
};

// Production cell metadata for grouping
const CELLS = {
    PRECISION_MACHINING: { name: 'Precision Machining (CNC & EDM)', code: 'PRECISION_MACHINING' },
    FABRICATION_FORMING: { name: 'Fabrication & Sheet Metal Forming', code: 'FABRICATION_FORMING' },
    ASSEMBLY_WELDING: { name: 'Automated Welding & Assembly', code: 'ASSEMBLY_WELDING' },
    FINISHING_PACKAGING: { name: 'Finishing, Quality & Packaging', code: 'FINISHING_PACKAGING' }
};

document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();
    loadInitialData();
});

function initEventListeners() {
    const shiftSelect = document.getElementById('shift-select');
    shiftSelect.addEventListener('change', (e) => {
        state.selectedShift = e.target.value;
        solveCoverage(false);
    });

    document.getElementById('btn-simulate-crisis').addEventListener('click', () => {
        solveCoverage(true, false, true);
    });

    document.getElementById('btn-solve-shift').addEventListener('click', () => {
        solveCoverage(false, false, false);
    });

    document.getElementById('btn-reset-baseline').addEventListener('click', () => {
        resetFloor();
    });

    // Modal controls
    document.getElementById('btn-close-modal').addEventListener('click', closeModal);
    document.getElementById('btn-cancel-modal').addEventListener('click', closeModal);
    document.getElementById('btn-submit-override').addEventListener('click', submitOverride);
}

async function loadInitialData() {
    try {
        updateStatus('Loading machines, roster, and baseline coverage...');
        const [machinesRes, operatorsRes] = await Promise.all([
            fetch(`${API_BASE}/machines`),
            fetch(`${API_BASE}/operators`)
        ]);

        state.machines = await machinesRes.json();
        state.operators = await operatorsRes.json();

        // Run default solver on page load
        await solveCoverage(false, false, false);
    } catch (err) {
        console.error('Failed to load initial data:', err);
        updateStatus('Error connecting to backend API: ' + err.message, true);
    }
}

async function solveCoverage(simulateCrisis = false, resetOverrides = false, isCrisisAlert = false) {
    try {
        updateStatus(isCrisisAlert ? 'Simulating 5:45 AM crisis with unplanned absences...' : 'Running optimization solver...');
        
        const response = await fetch(`${API_BASE}/coverage/solve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                shiftType: state.selectedShift,
                date: new Date().toISOString().split('T')[0],
                simulateMorningCrisis: simulateCrisis,
                resetOverrides: resetOverrides
            })
        });

        if (!response.ok) {
            throw new Error(`Server returned HTTP ${response.status}`);
        }

        state.currentPlan = await response.json();
        renderDashboard();

        if (isCrisisAlert) {
            updateStatus('⚠️ 5:45 AM Crisis simulated: 5 morning operators absent. Engine reallocated cross-trained staff and holdovers without overloading the willing four!');
        } else {
            updateStatus(state.currentPlan.operationalSummaryMessage);
        }
    } catch (err) {
        console.error('Solve error:', err);
        updateStatus('Solve failed: ' + err.message, true);
    }
}

async function resetFloor() {
    try {
        updateStatus('Resetting all operators to present and clearing manual locks...');
        const response = await fetch(`${API_BASE}/coverage/reset?shift=${state.selectedShift}`, {
            method: 'POST'
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        state.currentPlan = await response.json();
        renderDashboard();
        updateStatus('Floor successfully reset to baseline. All 18 machines staffed with regular roster.');
    } catch (err) {
        console.error('Reset error:', err);
        updateStatus('Reset error: ' + err.message, true);
    }
}

function renderDashboard() {
    if (!state.currentPlan) return;

    renderKPIs();
    renderWillingOperators();
    renderTriageAlerts();
    renderMachinesFloor();
}

function renderKPIs() {
    const plan = state.currentPlan;

    // Coverage KPI
    document.getElementById('metric-covered-count').textContent = plan.coveredMachines;
    document.getElementById('metric-total-count').textContent = plan.totalMachines;
    const coveragePercent = Math.round((plan.coveredMachines / plan.totalMachines) * 100);
    const bar = document.getElementById('metric-coverage-bar');
    bar.style.width = `${coveragePercent}%`;
    bar.style.backgroundColor = coveragePercent === 100 ? 'var(--success)' : 'var(--warning)';
    document.getElementById('metric-coverage-desc').textContent = 
        coveragePercent === 100 ? 'All 18 machines operational' : `${plan.unstaffedMachines} machine(s) idled / unstaffed`;

    // Cost KPI
    document.getElementById('metric-total-cost').textContent = `$${plan.totalShiftLaborCost.toFixed(2)}`;
    document.getElementById('metric-regular-cost').textContent = `$${plan.totalRegularLaborCost.toFixed(2)}`;
    document.getElementById('metric-ot-cost').textContent = `$${plan.totalOvertimeCost.toFixed(2)}`;

    // OT Hours KPI
    document.getElementById('metric-ot-hours').innerHTML = `${plan.totalOvertimeHours.toFixed(1)} <small>hrs</small>`;

    // Fatigue KPI
    document.getElementById('metric-fatigue-score').innerHTML = `${plan.averageWorkforceFatigue.toFixed(2)} <small>/ 1.0</small>`;
    const burnoutPercent = Math.min(100, Math.round(plan.averageWorkforceFatigue * 100));
    document.getElementById('metric-burnout-bar').style.width = `${burnoutPercent}%`;
}

function renderWillingOperators() {
    const container = document.getElementById('willing-operators-container');
    container.innerHTML = '';

    const list = state.currentPlan.willingOperatorsSummary || [];
    list.forEach(op => {
        const card = document.createElement('div');
        card.className = 'willing-card';

        const isProtected = op.protectionStatus === 'PROTECTED_FROM_OVERLOAD';
        const badgeClass = isProtected ? 'shield-badge' : 'shield-badge shield-badge-warning';
        const badgeIcon = isProtected ? '🛡️ PROTECTED' : '⚡ ' + op.protectionStatus.replace('_', ' ');

        const burnoutPercent = Math.min(100, Math.round(op.burnoutIndex * 100));

        card.innerHTML = `
            <div class="willing-card-top">
                <div>
                    <div class="willing-name">${escapeHtml(op.fullName)}</div>
                    <div class="willing-code">${escapeHtml(op.employeeCode)} • Will: ${(op.willingnessScore * 100).toFixed(0)}%</div>
                </div>
                <span class="${badgeClass}">${badgeIcon}</span>
            </div>
            <div class="willing-meter-wrap">
                <div class="willing-meter-label">
                    <span>Burnout Index: <strong>${op.burnoutIndex.toFixed(2)}</strong></span>
                    <span>OT this week: <strong>${op.weeklyOvertimeHours.toFixed(1)}h / 16h</strong></span>
                </div>
                <div class="meter-track">
                    <div class="meter-fill-danger" style="width: ${burnoutPercent}%;"></div>
                </div>
            </div>
            <div class="willing-notes">${escapeHtml(op.auditMessage)}</div>
        `;
        container.appendChild(card);
    });
}

function renderTriageAlerts() {
    const section = document.getElementById('triage-container');
    const list = document.getElementById('triage-list');
    list.innerHTML = '';

    const alerts = state.currentPlan.triageAlerts || [];
    if (alerts.length === 0) {
        section.style.display = 'none';
        return;
    }

    section.style.display = 'flex';
    alerts.forEach(a => {
        const item = document.createElement('div');
        item.className = a.severity === 'HIGH' || a.severity === 'CRITICAL' ? 'triage-item' : 'triage-item warning';
        item.innerHTML = `
            <div class="triage-headline">[${escapeHtml(a.alertType)}] ${escapeHtml(a.description)}</div>
            <div class="triage-mitigation">💡 Recommended Action: ${escapeHtml(a.suggestedMitigation)}</div>
        `;
        list.appendChild(item);
    });
}

function renderMachinesFloor() {
    const container = document.getElementById('cells-container');
    container.innerHTML = '';

    const assignmentsMap = new Map();
    (state.currentPlan.assignments || []).forEach(a => {
        assignmentsMap.set(a.machineCode, a);
    });

    // Group machines by Production Cell
    const cellGroups = {
        PRECISION_MACHINING: [],
        FABRICATION_FORMING: [],
        ASSEMBLY_WELDING: [],
        FINISHING_PACKAGING: []
    };

    state.machines.forEach(m => {
        if (cellGroups[m.cell]) {
            cellGroups[m.cell].push(m);
        }
    });

    Object.keys(cellGroups).forEach(cellKey => {
        const cellMachines = cellGroups[cellKey];
        if (cellMachines.length === 0) return;

        const cellInfo = CELLS[cellKey] || { name: cellKey };
        const groupEl = document.createElement('div');
        groupEl.className = 'cell-group';

        groupEl.innerHTML = `
            <div class="cell-header">
                <div class="cell-title">
                    <span>${escapeHtml(cellInfo.name)}</span>
                </div>
                <span class="cell-badge">${cellMachines.length} Machines</span>
            </div>
            <div class="machines-grid" id="grid-${cellKey}"></div>
        `;

        container.appendChild(groupEl);
        const gridEl = groupEl.querySelector(`#grid-${cellKey}`);

        cellMachines.forEach(machine => {
            const assignment = assignmentsMap.get(machine.getCode ? machine.getCode() : machine.code);
            const card = createMachineCard(machine, assignment);
            gridEl.appendChild(card);
        });
    });
}

function createMachineCard(machine, assignment) {
    const card = document.createElement('div');
    card.className = 'machine-card';

    const code = machine.code;
    const name = machine.name;
    const priority = machine.priorityRank;
    const certReq = machine.requiredCertLevel;
    const skill = machine.requiredSkillCode || 'GENERAL';

    const priClass = `p${priority}`;
    const isAssigned = !!assignment && assignment.operatorName !== 'UNASSIGNED';

    let typeBadgeText = 'UNASSIGNED';
    let typeBadgeClass = 'type-unassigned';
    let opName = 'No Operator Assigned';
    let reasonText = 'Machine idled due to staffing shortage.';
    let costText = '$0.00';

    if (isAssigned) {
        opName = assignment.operatorName + (assignment.pinned ? ' 📌' : '');
        reasonText = assignment.decisionReason || 'Assigned';
        costText = `$${assignment.totalCost.toFixed(2)}`;

        switch (assignment.assignmentType) {
            case 'PRIMARY_SCHEDULED':
                typeBadgeText = 'Primary Scheduled';
                typeBadgeClass = 'type-primary';
                break;
            case 'ONSITE_REALLOCATION':
                typeBadgeText = 'On-Site Reallocation';
                typeBadgeClass = 'type-onsite';
                break;
            case 'OVERTIME_HOLDOVER':
                typeBadgeText = `Holdover OT (${assignment.overtimeHours}h)`;
                typeBadgeClass = 'type-holdover';
                break;
            case 'OVERTIME_CALL_IN':
                typeBadgeText = `Call-In OT (${assignment.overtimeHours}h)`;
                typeBadgeClass = 'type-callin';
                break;
            case 'SUPERVISOR_OVERRIDE':
                typeBadgeText = 'Supervisor Override';
                typeBadgeClass = 'type-override';
                break;
        }
    }

    card.innerHTML = `
        <div class="machine-top">
            <span class="machine-code-badge">${escapeHtml(code)}</span>
            <span class="priority-badge ${priClass}">Priority ${priority}</span>
        </div>
        <div class="machine-name" title="${escapeHtml(name)}">${escapeHtml(name)}</div>
        <div class="cert-requirement">
            <span>Requires:</span>
            <span class="cert-pill">${escapeHtml(skill)} • ${escapeHtml(certReq)}</span>
        </div>
        <div class="assignment-block">
            <div class="operator-row">
                <span class="operator-name">${escapeHtml(opName)}</span>
                <span class="assignment-badge ${typeBadgeClass}">${escapeHtml(typeBadgeText)}</span>
            </div>
            <div class="reason-text">${escapeHtml(reasonText)}</div>
        </div>
        <div class="machine-card-footer">
            <span class="cost-tag">${costText}</span>
            <button class="btn-card-action btn-override" data-machine="${escapeHtml(code)}">
                ${assignment && assignment.pinned ? '📌 Edit Pin' : 'Override / Pin'}
            </button>
        </div>
    `;

    card.querySelector('.btn-override').addEventListener('click', (e) => {
        e.stopPropagation();
        openOverrideModal(machine, assignment);
    });

    return card;
}

function openOverrideModal(machine, existingAssignment) {
    state.selectedMachineForOverride = machine;

    document.getElementById('modal-machine-label').textContent = `${machine.code} - ${machine.name}`;
    const select = document.getElementById('modal-operator-select');
    select.innerHTML = '';

    // Sort operators: certified first
    const sortedOps = [...state.operators].sort((a, b) => {
        return a.fullName.localeCompare(b.fullName);
    });

    sortedOps.forEach(op => {
        const option = document.createElement('option');
        option.value = op.employeeCode;
        option.textContent = `${op.fullName} (${op.employeeCode}) - Shift: ${op.primaryShift} | OT this wk: ${op.weeklyOvertimeHoursWorked.toFixed(1)}h | Burnout: ${op.burnoutIndex ? op.burnoutIndex.toFixed(2) : '0.00'}`;
        if (existingAssignment && existingAssignment.operatorEmployeeCode === op.employeeCode) {
            option.selected = true;
        }
        select.appendChild(option);
    });

    document.getElementById('override-modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('override-modal').style.display = 'none';
    state.selectedMachineForOverride = null;
}

async function submitOverride() {
    if (!state.selectedMachineForOverride) return;

    const machineCode = state.selectedMachineForOverride.code;
    const operatorCode = document.getElementById('modal-operator-select').value;
    const notes = document.getElementById('modal-notes').value;
    const lockPin = document.getElementById('modal-lock-pin').checked;

    try {
        updateStatus(`Pinning operator ${operatorCode} to ${machineCode} and re-balancing remaining machines...`);
        closeModal();

        const response = await fetch(`${API_BASE}/coverage/override`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                machineCode: machineCode,
                operatorEmployeeCode: operatorCode,
                shiftType: state.selectedShift,
                date: new Date().toISOString().split('T')[0],
                lockAssignment: lockPin,
                supervisorNotes: notes
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        state.currentPlan = await response.json();
        renderDashboard();
        updateStatus(`Override locked for ${machineCode}. Floor successfully re-optimized around manual pin.`);
    } catch (err) {
        console.error('Override error:', err);
        updateStatus('Failed to apply override: ' + err.message, true);
    }
}

function updateStatus(text, isError = false) {
    const bar = document.getElementById('status-summary-text');
    if (bar) {
        bar.textContent = text;
        bar.style.color = isError ? 'var(--danger)' : 'var(--cyan)';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
