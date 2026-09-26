package com.taskmanager.taskapi.walk;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/walks")
public class WalkController {

	private final WalkService walkService;

	public WalkController(WalkService walkService) {
		this.walkService = walkService;
	}

	@PostMapping
	public ResponseEntity<WalkResponse> start() {
		WalkResponse walk = walkService.start();
		return ResponseEntity.created(URI.create("/api/walks/" + walk.id())).body(walk);
	}

	@GetMapping("/active")
	public ResponseEntity<WalkResponse> findActive() {
		return walkService.findActive().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping
	public List<WalkResponse> findAll() {
		return walkService.findAll();
	}

	@PutMapping("/{id}/progress")
	public WalkResponse updateProgress(@PathVariable Long id, @Valid @RequestBody WalkProgressRequest request) {
		return walkService.updateProgress(id, request);
	}

	@PostMapping("/{id}/pause")
	public WalkResponse pause(@PathVariable Long id) {
		return walkService.pause(id);
	}

	@PostMapping("/{id}/resume")
	public WalkResponse resume(@PathVariable Long id) {
		return walkService.resume(id);
	}

	@PostMapping("/{id}/finish")
	public WalkResponse finish(@PathVariable Long id, @Valid @RequestBody WalkProgressRequest request) {
		return walkService.finish(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		walkService.delete(id);
		return ResponseEntity.noContent().build();
	}
}