package com.careertrack.modules.setting.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.setting.dto.CompanySettingDto;
import com.careertrack.modules.setting.entity.CompanySetting;
import com.careertrack.modules.setting.repository.CompanySettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CompanySettingServiceImpl implements CompanySettingService {

    private final CompanySettingRepository settingRepository;

    public CompanySettingServiceImpl(CompanySettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public List<CompanySettingDto> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public CompanySettingDto getSettingByKey(String key) {
        CompanySetting setting = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre introuvable : " + key));
        return mapToDto(setting);
    }

    @Override
    @Transactional
    public CompanySettingDto updateSetting(String key, String value) {
        CompanySetting setting = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre introuvable : " + key));
        setting.setSettingValue(value);
        return mapToDto(settingRepository.save(setting));
    }

    private CompanySettingDto mapToDto(CompanySetting s) {
        return CompanySettingDto.builder()
                .id(s.getId())
                .settingKey(s.getSettingKey())
                .settingValue(s.getSettingValue())
                .description(s.getDescription())
                .build();
    }
}
