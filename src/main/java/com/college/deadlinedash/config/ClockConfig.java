package com.college.deadlinedash.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	/** One shared Clock, so tests can replace "today" with a fixed date. */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
