package com.taskmanager.taskapi.walk;

public class WalkNotFoundException extends RuntimeException {
	public WalkNotFoundException(Long id) {
		super("Caminhada não encontrada: " + id);
	}
}