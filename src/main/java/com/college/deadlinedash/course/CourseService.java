package com.college.deadlinedash.course;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CourseService {

	private final CourseRepository courses;

	public CourseService(CourseRepository courses) {
		this.courses = courses;
	}

	public Course create(CourseForm form) {
		return courses.save(new Course(form.getName().trim(), form.getCode().trim()));
	}

	public List<Course> findAll() {
		return courses.findAllByOrderByCodeAsc();
	}
}
