package com.taskmanager.taskapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmanager.taskapi.task.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TaskRepository taskRepository;

	@BeforeEach
	void cleanDatabase() {
		taskRepository.deleteAll();
	}

	@Test
	void taskCanBeCreatedReadUpdatedAndDeleted() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/tasks")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"  Comprar leite  \",\"description\":\"Integral\",\"completed\":false}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("Comprar leite"))
			.andReturn();
		String taskUrl = created.getResponse().getHeader("Location");

		mockMvc.perform(get(taskUrl))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Integral"));
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Comprar leite"));

		mockMvc.perform(put(taskUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Comprar pão\",\"completed\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Comprar pão"))
			.andExpect(jsonPath("$.completed").value(true));

		mockMvc.perform(delete(taskUrl)).andExpect(status().isNoContent());
		mockMvc.perform(get(taskUrl)).andExpect(status().isNotFound());
	}

	@Test
	void rejectsTasksWithoutTitle() throws Exception {
		mockMvc.perform(post("/api/tasks")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\" \",\"completed\":false}"))
			.andExpect(status().isBadRequest());
	}
}
