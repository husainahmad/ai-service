package com.harmoni.pos.ai.domain.model;

import java.util.List;

public record CustomizationGroupSelection(Long customizationId,
                                          List<CustomizationOptionSelection> options) {
}
