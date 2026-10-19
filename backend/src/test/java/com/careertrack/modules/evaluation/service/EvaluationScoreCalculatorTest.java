package com.careertrack.modules.evaluation.service;

import com.careertrack.modules.evaluation.entity.Objective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationScoreCalculatorTest {

    private EvaluationScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EvaluationScoreCalculator();
    }

    @Test
    @DisplayName("Devrait retourner 0.00 lorsque la liste d'objectifs est nulle ou vide")
    void shouldReturnZeroWhenEmptyOrNull() {
        assertThat(calculator.calculateWeightedScore(null)).isEqualByComparingTo("0.00");
        assertThat(calculator.calculateWeightedScore(Collections.emptyList())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Devrait calculer correctement le score pondéré sur 5.0")
    void shouldCalculateWeightedScoreProperly() {
        // Obj 1: 40% poids, 100% atteinte
        // Obj 2: 30% poids, 90% atteinte
        // Obj 3: 30% poids, 95% atteinte
        // Somme pondérée = 4000 + 2700 + 2850 = 9550 / 100 = 95.5%
        // Score sur 5 = 95.5 * 5 / 100 = 4.775 -> arrondi à 4.78
        Objective o1 = Objective.builder().weight(new BigDecimal("40")).achievementRate(new BigDecimal("100")).build();
        Objective o2 = Objective.builder().weight(new BigDecimal("30")).achievementRate(new BigDecimal("90")).build();
        Objective o3 = Objective.builder().weight(new BigDecimal("30")).achievementRate(new BigDecimal("95")).build();

        BigDecimal score = calculator.calculateWeightedScore(List.of(o1, o2, o3));
        assertThat(score).isEqualByComparingTo("4.78");
    }

    @Test
    @DisplayName("Devrait normaliser correctement lorsque la somme des poids est différente de 100")
    void shouldNormalizeWhenWeightsDoNotSumToHundred() {
        // Poids: 2 et 2 (total = 4)
        // Taux: 80% et 60% -> moyenne = 70% -> sur 5 = 3.50
        Objective o1 = Objective.builder().weight(new BigDecimal("2")).achievementRate(new BigDecimal("80")).build();
        Objective o2 = Objective.builder().weight(new BigDecimal("2")).achievementRate(new BigDecimal("60")).build();

        BigDecimal score = calculator.calculateWeightedScore(List.of(o1, o2));
        assertThat(score).isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("Devrait utiliser la moyenne arithmétique lorsque tous les poids sont nuls")
    void shouldUseArithmeticMeanWhenAllWeightsAreZero() {
        Objective o1 = Objective.builder().weight(BigDecimal.ZERO).achievementRate(new BigDecimal("80")).build();
        Objective o2 = Objective.builder().weight(BigDecimal.ZERO).achievementRate(new BigDecimal("60")).build();

        BigDecimal score = calculator.calculateWeightedScore(List.of(o1, o2));
        assertThat(score).isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("Devrait plafonner le score à 5.00 en cas de surperformance (> 100%)")
    void shouldCapScoreToFiveWhenOverachieved() {
        Objective o1 = Objective.builder().weight(new BigDecimal("100")).achievementRate(new BigDecimal("150")).build();

        BigDecimal score = calculator.calculateWeightedScore(List.of(o1));
        assertThat(score).isEqualByComparingTo("5.00");
    }
}
