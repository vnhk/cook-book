package com.bervan.cookbook;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class MacroBreakdownDto implements BaseDTO<UUID> {
    private UUID id;
    private double avgConsumedProtein;
    private double avgConsumedFat;
    private double avgConsumedCarbs;
    private double avgTargetProtein;
    private double avgTargetFat;
    private double avgTargetCarbs;
    private boolean hasData;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return null; // This is a data transfer object without corresponding entity
    }
}
