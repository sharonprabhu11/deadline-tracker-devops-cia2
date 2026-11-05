package com.college.deadlinedash.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuildInfoConfig {

	@Bean
	public AppInfo appInfo(ObjectProvider<BuildProperties> build,
			@Value("${APP_GIT_SHA:dev}") String sha,
			@Value("${HOSTNAME:local}") String pod) {
		// build-info.properties is written by the Maven plugin; it is missing when run from an IDE.
		BuildProperties props = build.getIfAvailable();
		String version = props != null ? props.getVersion() : "unknown";
		String time = props != null && props.getTime() != null ? props.getTime().toString() : "unknown";
		return new AppInfo(version, sha, pod, time);
	}
}
