package com.careertrack.modules.setting.service;

import com.careertrack.modules.setting.dto.CompanySettingDto;

import java.util.List;

public interface CompanySettingService {
    List<CompanySettingDto> getAllSettings();
    CompanySettingDto getSettingByKey(String key);
    CompanySettingDto updateSetting(String key, String value);
}
