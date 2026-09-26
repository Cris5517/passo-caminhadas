package com.taskmanager.taskapi.walk;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

public record WalkProgressRequest(
		@PositiveOrZero(message = "A duração não pode ser negativa") long durationSeconds,
		@DecimalMin(value = "0.0", message = "A distância não pode ser negativa") double distanceMeters) {
}