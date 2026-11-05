package com.college.deadlinedash.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.college.deadlinedash.course.Course;
import com.college.deadlinedash.course.CourseRepository;
import com.college.deadlinedash.urgency.UrgencyBand;
import com.college.deadlinedash.urgency.UrgencyCalculator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
	private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

	@Mock
	TaskRepository tasks;
	@Mock
	CourseRepository courses;

	private TaskService service() {
		return new TaskService(tasks, courses, new UrgencyCalculator(), CLOCK);
	}

	private Task task(String title, int weight, int daysFromToday, boolean done) {
		Task t = new Task(new Course("Maths", "MA1"), title, TODAY.plusDays(daysFromToday), weight, Instant.EPOCH);
		t.setDone(done);
		return t;
	}

	@Test
	void createSavesTaskWithTrimmedTitleAndClockTime() {
		Course course = new Course("Maths", "MA1");
		when(courses.findById(1L)).thenReturn(Optional.of(course));
		when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
		TaskForm form = new TaskForm();
		form.setCourseId(1L);
		form.setTitle("  Lab record ");
		form.setDueDate(TODAY.plusDays(2));
		form.setWeight(15);

		service().create(form);

		ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
		verify(tasks).save(saved.capture());
		assertThat(saved.getValue().getTitle()).isEqualTo("Lab record");
		assertThat(saved.getValue().getWeight()).isEqualTo(15);
		assertThat(saved.getValue().isDone()).isFalse();
		assertThat(saved.getValue().getCreatedAt()).isEqualTo(Instant.now(CLOCK));
	}

	@Test
	void createWithUnknownCourseFails() {
		when(courses.findById(9L)).thenReturn(Optional.empty());
		TaskForm form = new TaskForm();
		form.setCourseId(9L);
		form.setTitle("x");

		assertThatThrownBy(() -> service().create(form)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void markDoneSetsDoneFlag() {
		Task t = task("a", 10, 1, false);
		when(tasks.findById(5L)).thenReturn(Optional.of(t));

		service().markDone(5L);

		assertThat(t.isDone()).isTrue();
	}

	@Test
	void deleteCallsRepository() {
		service().delete(7L);

		verify(tasks).deleteById(7L);
	}

	@Test
	void listIsOverdueFirstThenScoreDescendingThenDoneLast() {
		when(tasks.findAll()).thenReturn(List.of(
				task("done", 90, 0, true),
				task("later", 10, 9, false),
				task("urgent", 20, 0, false),
				task("overdue", 5, -3, false),
				task("soon", 30, 6, false)));

		List<TaskView> sorted = service().listSorted();

		assertThat(sorted).extracting(v -> v.task().getTitle())
				.containsExactly("overdue", "urgent", "soon", "later", "done");
		assertThat(sorted.get(0).urgency().band()).isEqualTo(UrgencyBand.OVERDUE);
		assertThat(sorted.get(4).urgency().band()).isEqualTo(UrgencyBand.DONE);
	}
}
