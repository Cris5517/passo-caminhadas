package com.taskmanager.taskapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmanager.taskapi.walk.WalkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WalkApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WalkRepository walkRepository;

	@BeforeEach
	void cleanWalks() {
		walkRepository.deleteAll();
	}

	@Test
	void walkCanBePausedResumedAndFinished() throws Exception {
		MvcResult started = mockMvc.perform(post("/api/walks"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn();
		String walkUrl = started.getResponse().getHeader("Location");

		mockMvc.perform(put(walkUrl + "/progress")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"durationSeconds\":600,\"distanceMeters\":850}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.distanceMeters").value(850));
		mockMvc.perform(post(walkUrl + "/pause")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAUSED"));
		mockMvc.perform(post(walkUrl + "/resume")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
		mockMvc.perform(post(walkUrl + "/finish")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"durationSeconds\":1200,\"distanceMeters\":1700}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.averagePaceSecondsPerKm").value(706));

		mockMvc.perform(get("/api/walks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].durationSeconds").value(1200));
	}
}