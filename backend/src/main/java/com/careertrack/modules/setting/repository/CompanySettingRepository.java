package com.careertrack.modules.setting.repository;

import com.careertrack.modules.setting.entity.CompanySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanySettingRepository extends JpaRepository<CompanySetting, UUID> {
    Optional<CompanySetting> findBySettingKey(String settingKey);
}
