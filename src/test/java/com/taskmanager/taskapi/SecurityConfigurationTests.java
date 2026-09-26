package com.taskmanager.taskapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"app.security.enabled=true",
		"app.security.username=walk-security-test",
		"app.security.password=local-test-password-1234",
		"spring.datasource.url=jdbc:h2:mem:walk-security;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
class SecurityConfigurationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void walkingApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/walks")).andExpect(status().isUnauthorized());
	}

	@Test
	void walkingApiAcceptsConfiguredCredentials() throws Exception {
		mockMvc.perform(get("/api/walks").with(httpBasic("walk-security-test", "local-test-password-1234")))
				.andExpect(status().isOk());
	}

	@Test
	void webInterfaceRemainsPublic() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
	}
}