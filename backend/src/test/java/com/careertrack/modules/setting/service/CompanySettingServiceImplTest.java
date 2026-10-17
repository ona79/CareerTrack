package com.careertrack.modules.setting.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.setting.dto.CompanySettingDto;
import com.careertrack.modules.setting.entity.CompanySetting;
import com.careertrack.modules.setting.repository.CompanySettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanySettingServiceImplTest {

    @Mock
    private CompanySettingRepository settingRepository;

    private CompanySettingServiceImpl settingService;

    @BeforeEach
    void setUp() {
        settingService = new CompanySettingServiceImpl(settingRepository);
    }

    @Test
    @DisplayName("Lister tous les paramètres de configuration")
    void shouldGetAllSettings() {
        CompanySetting setting = CompanySetting.builder()
                .id(UUID.randomUUID())
                .settingKey("LEAVE_DAYS_PER_MONTH")
                .settingValue("2.0")
                .description("Jours de congés légaux par mois")
                .build();

        when(settingRepository.findAll()).thenReturn(List.of(setting));

        List<CompanySettingDto> result = settingService.getAllSettings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSettingKey()).isEqualTo("LEAVE_DAYS_PER_MONTH");
        assertThat(result.get(0).getSettingValue()).isEqualTo("2.0");
    }

    @Test
    @DisplayName("Obtenir un paramètre par clé ou lever ResourceNotFoundException")
    void shouldGetSettingByKey() {
        CompanySetting setting = CompanySetting.builder()
                .settingKey("CURRENCY")
                .settingValue("XOF")
                .description("Devise officielle")
                .build();

        when(settingRepository.findBySettingKey("CURRENCY")).thenReturn(Optional.of(setting));

        CompanySettingDto result = settingService.getSettingByKey("CURRENCY");

        assertThat(result.getSettingValue()).isEqualTo("XOF");

        when(settingRepository.findBySettingKey("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> settingService.getSettingByKey("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Mettre à jour la valeur d'un paramètre")
    void shouldUpdateSetting() {
        CompanySetting setting = CompanySetting.builder()
                .settingKey("MAX_WEEKLY_HOURS")
                .settingValue("40")
                .description("Durée hebdomadaire légale du travail")
                .build();

        when(settingRepository.findBySettingKey("MAX_WEEKLY_HOURS")).thenReturn(Optional.of(setting));
        when(settingRepository.save(any(CompanySetting.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanySettingDto updated = settingService.updateSetting("MAX_WEEKLY_HOURS", "44");

        assertThat(updated.getSettingValue()).isEqualTo("44");
    }
}
