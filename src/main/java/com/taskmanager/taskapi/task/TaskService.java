package com.taskmanager.taskapi.task;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

	private final TaskRepository taskRepository;

	public TaskService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	public TaskResponse create(TaskRequest request) {
		Task task = new Task(request.title().trim(), request.description(), request.completed());
		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> findAll() {
		return taskRepository.findAll().stream().map(TaskResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse findById(Long id) {
		return TaskResponse.from(getTask(id));
	}

	public TaskResponse update(Long id, TaskRequest request) {
		Task task = getTask(id);
		task.setTitle(request.title().trim());
		task.setDescription(request.description());
		task.setCompleted(request.completed());
		return TaskResponse.from(taskRepository.save(task));
	}

	public void delete(Long id) {
		taskRepository.delete(getTask(id));
	}

	private Task getTask(Long id) {
		return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}
}