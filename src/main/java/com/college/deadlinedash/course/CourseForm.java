package com.college.deadlinedash.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CourseForm {

	@NotBlank(message = "Course name is required")
	@Size(max = 100)
	private String name;

	@NotBlank(message = "Course code is required")
	@Size(max = 20)
	private String code;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
