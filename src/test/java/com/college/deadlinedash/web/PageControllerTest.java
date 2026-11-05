package com.college.deadlinedash.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PageControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void homePageShowsFooter() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("commit")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("pod")));
	}

	@Test
	void apiInfoReturnsShaAndPod() throws Exception {
		mvc.perform(get("/api/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.commitSha").exists())
				.andExpect(jsonPath("$.podName").exists());
	}

	@Test
	void healthProbesAreEnabled() throws Exception {
		mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
		mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
	}
}
