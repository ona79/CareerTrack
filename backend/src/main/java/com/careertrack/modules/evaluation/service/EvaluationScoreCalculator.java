package com.careertrack.modules.evaluation.service;

import com.careertrack.modules.evaluation.entity.Objective;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class EvaluationScoreCalculator {

    /**
     * Calcule le score global pondéré sur 5.0 à partir des objectifs SMART.
     * Formule : (Somme(tauxAtteinte * poids) / Somme(poids)) * (5 / 100)
     * Arrondi à 2 décimales HALF_UP.
     *
     * @param objectives liste des objectifs SMART avec poids et taux d'atteinte
     * @return score sur 5.0, ou BigDecimal.ZERO si aucun objectif
     */
    public BigDecimal calculateWeightedScore(List<Objective> objectives) {
        if (objectives == null || objectives.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalWeightedAchievement = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal sumAchievementWithoutWeight = BigDecimal.ZERO;
        int count = 0;

        for (Objective obj : objectives) {
            BigDecimal rate = obj.getAchievementRate() != null ? obj.getAchievementRate() : BigDecimal.ZERO;
            BigDecimal weight = obj.getWeight() != null ? obj.getWeight() : BigDecimal.ZERO;

            if (weight.compareTo(BigDecimal.ZERO) > 0) {
                totalWeightedAchievement = totalWeightedAchievement.add(rate.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
            sumAchievementWithoutWeight = sumAchievementWithoutWeight.add(rate);
            count++;
        }

        BigDecimal percentage;
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            percentage = totalWeightedAchievement.divide(totalWeight, 4, RoundingMode.HALF_UP);
        } else if (count > 0) {
            percentage = sumAchievementWithoutWeight.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Conversion en note sur 5 : percentage * 5 / 100
        BigDecimal scoreOnFive = percentage
                .multiply(BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Cap à 5.0 au maximum
        if (scoreOnFive.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            scoreOnFive = BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP);
        }

        return scoreOnFive.max(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }
}
