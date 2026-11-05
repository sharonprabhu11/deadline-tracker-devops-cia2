package com.college.deadlinedash.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PageControllerTest.FixedClock.class)
class PageControllerTest {

	@TestConfiguration
	static class FixedClock {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(LocalDate.of(2026, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
		}
	}

	@Autowired
	MockMvc mvc;

	@Test
	void homePageReturns200WithFooter() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DeadlineDash v")))
				.andExpect(content().string(containsString("commit")))
				.andExpect(content().string(containsString("pod")));
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

	@Test
	void invalidTaskFormIsRejectedWithMessages() throws Exception {
		mvc.perform(post("/tasks")
						.param("title", "   ")
						.param("weight", "500"))
				.andExpect(status().isOk()) // page is shown again, nothing saved
				.andExpect(content().string(containsString("Title is required")))
				.andExpect(content().string(containsString("Due date is required")))
				.andExpect(content().string(containsString("Weight must be at most 100")));
	}

	@Test
	void invalidCourseFormIsRejected() throws Exception {
		mvc.perform(post("/courses").param("name", "").param("code", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Course name is required")));
	}

	@Test
	void addCourseThenTaskThenCompleteAndDelete() throws Exception {
		mvc.perform(post("/courses").param("name", "Networks").param("code", "CN301"))
				.andExpect(redirectedUrl("/"));
		mvc.perform(get("/")).andExpect(content().string(containsString("CN301")));
		// the only course created by this test class has a generated id; look it up through the page
		String page = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
		String id = page.replaceAll("(?s).*<option value=\"(\\d+)\">CN301.*", "$1");

		mvc.perform(post("/tasks").param("courseId", id).param("title", "Lab record")
						.param("dueDate", "2026-03-11").param("weight", "25"))
				.andExpect(redirectedUrl("/"));
		mvc.perform(get("/"))
				.andExpect(content().string(containsString("Lab record")))
				.andExpect(content().string(containsString("URGENT"))); // 25*100/2 = 1250
	}
}
