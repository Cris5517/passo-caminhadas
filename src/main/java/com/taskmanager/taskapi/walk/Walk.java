package com.taskmanager.taskapi.walk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "walks")
public class Walk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Instant startedAt;

	private Instant finishedAt;

	@Column(nullable = false)
	private long durationSeconds;

	@Column(nullable = false)
	private double distanceMeters;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WalkStatus status;

	protected Walk() {
	}

	public Walk(WalkStatus status) {
		this.status = status;
	}

	@PrePersist
	void setStartedAt() {
		if (startedAt == null) {
			startedAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public long getDurationSeconds() {
		return durationSeconds;
	}

	public double getDistanceMeters() {
		return distanceMeters;
	}

	public WalkStatus getStatus() {
		return status;
	}

	public void updateProgress(long durationSeconds, double distanceMeters) {
		this.durationSeconds = durationSeconds;
		this.distanceMeters = distanceMeters;
	}

	public void setStatus(WalkStatus status) {
		this.status = status;
	}

	public void finish() {
		finishedAt = Instant.now();
		status = WalkStatus.COMPLETED;
	}
}