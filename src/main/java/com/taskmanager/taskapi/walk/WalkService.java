package com.taskmanager.taskapi.walk;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WalkService {

	private static final List<WalkStatus> OPEN_STATUSES = List.of(WalkStatus.ACTIVE, WalkStatus.PAUSED);

	private final WalkRepository walkRepository;

	public WalkService(WalkRepository walkRepository) {
		this.walkRepository = walkRepository;
	}

	public WalkResponse start() {
		if (walkRepository.findFirstByStatusInOrderByStartedAtDesc(OPEN_STATUSES).isPresent()) {
			throw new WalkStateException("Finalize ou retome a caminhada atual antes de iniciar outra");
		}
		return WalkResponse.from(walkRepository.save(new Walk(WalkStatus.ACTIVE)));
	}

	@Transactional(readOnly = true)
	public Optional<WalkResponse> findActive() {
		return walkRepository.findFirstByStatusInOrderByStartedAtDesc(OPEN_STATUSES).map(WalkResponse::from);
	}

	@Transactional(readOnly = true)
	public List<WalkResponse> findAll() {
		return walkRepository.findAllByOrderByStartedAtDesc().stream().map(WalkResponse::from).toList();
	}

	public WalkResponse updateProgress(Long id, WalkProgressRequest request) {
		Walk walk = getWalk(id);
		ensureOpen(walk);
		walk.updateProgress(request.durationSeconds(), request.distanceMeters());
		return WalkResponse.from(walk);
	}

	public WalkResponse pause(Long id) {
		Walk walk = getWalk(id);
		if (walk.getStatus() != WalkStatus.ACTIVE) {
			throw new WalkStateException("Somente uma caminhada ativa pode ser pausada");
		}
		walk.setStatus(WalkStatus.PAUSED);
		return WalkResponse.from(walk);
	}

	public WalkResponse resume(Long id) {
		Walk walk = getWalk(id);
		if (walk.getStatus() != WalkStatus.PAUSED) {
			throw new WalkStateException("Somente uma caminhada pausada pode ser retomada");
		}
		walk.setStatus(WalkStatus.ACTIVE);
		return WalkResponse.from(walk);
	}

	public WalkResponse finish(Long id, WalkProgressRequest request) {
		Walk walk = getWalk(id);
		ensureOpen(walk);
		walk.updateProgress(request.durationSeconds(), request.distanceMeters());
		walk.finish();
		return WalkResponse.from(walk);
	}

	public void delete(Long id) {
		walkRepository.delete(getWalk(id));
	}

	private Walk getWalk(Long id) {
		return walkRepository.findById(id).orElseThrow(() -> new WalkNotFoundException(id));
	}

	private void ensureOpen(Walk walk) {
		if (walk.getStatus() == WalkStatus.COMPLETED) {
			throw new WalkStateException("A caminhada já foi finalizada");
		}
	}
}