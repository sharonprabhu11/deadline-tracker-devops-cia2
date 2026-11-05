package com.college.deadlinedash.task;

import java.time.Instant;
import java.time.LocalDate;

import com.college.deadlinedash.course.Course;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "course_id")
	private Course course;

	private String title;

	private LocalDate dueDate;

	/** Marks weightage, 1 to 100. */
	private int weight;

	private boolean done;

	private Instant createdAt;

	protected Task() {
		// required by JPA
	}

	public Task(Course course, String title, LocalDate dueDate, int weight, Instant createdAt) {
		this.course = course;
		this.title = title;
		this.dueDate = dueDate;
		this.weight = weight;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Course getCourse() {
		return course;
	}

	public String getTitle() {
		return title;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public int getWeight() {
		return weight;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
