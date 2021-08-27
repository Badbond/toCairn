package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.model.HierarchicalConfiguration;
import me.soels.thesis.model.SolverConfiguration;


@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class HierarchicalConfigurationDto extends SolverConfigurationDto {
    // TODO: Add configuration fields for Hierarchical clustering algorithm.

    public HierarchicalConfigurationDto(HierarchicalConfiguration dao) {
    }

    @Override
    public SolverConfiguration toDao() {
        return new HierarchicalConfiguration();
    }
}
