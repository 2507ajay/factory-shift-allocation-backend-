package com.zenalyst.factory.repository;

import com.zenalyst.factory.domain.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    Optional<Certification> findBySkillCode(String skillCode);
    List<Certification> findBySkillCodeOrderByLevelDesc(String skillCode);
}
