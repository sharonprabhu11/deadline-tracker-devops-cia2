package com.college.deadlinedash.web;

import com.college.deadlinedash.config.AppInfo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

	private final AppInfo appInfo;

	public InfoController(AppInfo appInfo) {
		this.appInfo = appInfo;
	}

	/** The pipeline calls this after a deploy to check the new commit SHA is live. */
	@GetMapping("/api/info")
	public AppInfo info() {
		return appInfo;
	}
}
