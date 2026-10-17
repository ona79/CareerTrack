package com.careertrack.modules.leave.dto;

import com.careertrack.modules.leave.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApprovalDto {

    @NotNull(message = "La décision est obligatoire")
    private LeaveStatus status; // APPROVED_N1, APPROVED_DRH, REJECTED

    private String managerComment;
}
