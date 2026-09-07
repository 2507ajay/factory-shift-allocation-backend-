package com.zenalyst.factory.repository;

import com.zenalyst.factory.domain.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    Optional<Machine> findByCode(String code);
    List<Machine> findAllByOrderByPriorityRankAscCodeAsc();
    List<Machine> findByActiveTrueOrderByPriorityRankAscCodeAsc();
}
