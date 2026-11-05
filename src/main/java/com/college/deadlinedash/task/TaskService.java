package com.college.deadlinedash.task;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.college.deadlinedash.course.Course;
import com.college.deadlinedash.course.CourseRepository;
import com.college.deadlinedash.urgency.UrgencyBand;
import com.college.deadlinedash.urgency.UrgencyCalculator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private final TaskRepository tasks;
	private final CourseRepository courses;
	private final UrgencyCalculator calculator;
	private final Clock clock;

	public TaskService(TaskRepository tasks, CourseRepository courses, UrgencyCalculator calculator, Clock clock) {
		this.tasks = tasks;
		this.courses = courses;
		this.calculator = calculator;
		this.clock = clock;
	}

	public Task create(TaskForm form) {
		Course course = courses.findById(form.getCourseId())
				.orElseThrow(() -> new IllegalArgumentException("Unknown course " + form.getCourseId()));
		Task task = new Task(course, form.getTitle().trim(), form.getDueDate(), form.getWeight(), Instant.now(clock));
		return tasks.save(task);
	}

	@Transactional
	public void markDone(Long id) {
		Task task = tasks.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown task " + id));
		task.setDone(true);
	}

	public void delete(Long id) {
		tasks.deleteById(id);
	}

	/** Overdue first, then higher score first, done tasks last. */
	public List<TaskView> listSorted() {
		return tasks.findAll().stream()
				.map(t -> new TaskView(t, calculator.calculate(t, clock)))
				.sorted(Comparator
						.comparingInt((TaskView v) -> rank(v.urgency().band()))
						.thenComparing(v -> v.urgency().score(), Comparator.reverseOrder())
						.thenComparing(v -> v.task().getDueDate()))
				.toList();
	}

	private static int rank(UrgencyBand band) {
		return switch (band) {
			case OVERDUE -> 0;
			case DONE -> 2;
			default -> 1;
		};
	}
}
