package com.zenalyst.factory.service;

import com.zenalyst.factory.domain.model.Machine;
import com.zenalyst.factory.repository.MachineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class MachineService {

    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<Machine> getAllMachines() {
        return machineRepository.findAllByOrderByPriorityRankAscCodeAsc();
    }

    public List<Machine> getActiveMachines() {
        return machineRepository.findByActiveTrueOrderByPriorityRankAscCodeAsc();
    }

    public Machine getMachineByCode(String code) {
        return machineRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Machine not found with code: " + code));
    }
}
