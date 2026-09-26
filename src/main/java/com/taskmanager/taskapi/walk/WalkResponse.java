package com.taskmanager.taskapi.walk;

import java.time.Instant;

public record WalkResponse(
		Long id,
		Instant startedAt,
		Instant finishedAt,
		long durationSeconds,
		double distanceMeters,
		Long averagePaceSecondsPerKm,
		WalkStatus status) {

	public static WalkResponse from(Walk walk) {
		Long pace = walk.getDistanceMeters() > 0
				? Math.round(walk.getDurationSeconds() * 1000.0 / walk.getDistanceMeters())
				: null;
		return new WalkResponse(
				walk.getId(),
				walk.getStartedAt(),
				walk.getFinishedAt(),
				walk.getDurationSeconds(),
				walk.getDistanceMeters(),
				pace,
				walk.getStatus());
	}
}