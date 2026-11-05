package com.college.deadlinedash.web;

import com.college.deadlinedash.config.AppInfo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

	private final AppInfo appInfo;

	public PageController(AppInfo appInfo) {
		this.appInfo = appInfo;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("info", appInfo);
		return "index";
	}
}
