package com.college.deadlinedash.web;

import com.college.deadlinedash.config.AppInfo;
import com.college.deadlinedash.course.CourseForm;
import com.college.deadlinedash.course.CourseService;
import com.college.deadlinedash.task.TaskForm;
import com.college.deadlinedash.task.TaskService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

	private final AppInfo appInfo;
	private final TaskService taskService;
	private final CourseService courseService;

	public PageController(AppInfo appInfo, TaskService taskService, CourseService courseService) {
		this.appInfo = appInfo;
		this.taskService = taskService;
		this.courseService = courseService;
	}

	@GetMapping("/")
	public String index(Model model) {
		if (!model.containsAttribute("taskForm")) {
			model.addAttribute("taskForm", new TaskForm());
		}
		if (!model.containsAttribute("courseForm")) {
			model.addAttribute("courseForm", new CourseForm());
		}
		return page(model);
	}

	@PostMapping("/tasks")
	public String createTask(@Valid @ModelAttribute("taskForm") TaskForm form, BindingResult errors, Model model) {
		if (errors.hasErrors()) {
			model.addAttribute("courseForm", new CourseForm());
			return page(model); // re-show the page with the error messages, status 200
		}
		taskService.create(form);
		return "redirect:/";
	}

	@PostMapping("/tasks/{id}/done")
	public String done(@PathVariable Long id) {
		taskService.markDone(id);
		return "redirect:/";
	}

	@PostMapping("/tasks/{id}/delete")
	public String delete(@PathVariable Long id) {
		taskService.delete(id);
		return "redirect:/";
	}

	@PostMapping("/courses")
	public String createCourse(@Valid @ModelAttribute("courseForm") CourseForm form, BindingResult errors,
			Model model) {
		if (errors.hasErrors()) {
			model.addAttribute("taskForm", new TaskForm());
			return page(model);
		}
		courseService.create(form);
		return "redirect:/";
	}

	private String page(Model model) {
		model.addAttribute("info", appInfo);
		model.addAttribute("views", taskService.listSorted());
		model.addAttribute("courses", courseService.findAll());
		return "index";
	}
}
