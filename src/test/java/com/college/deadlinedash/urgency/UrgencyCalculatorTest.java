package com.college.deadlinedash.urgency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.college.deadlinedash.course.Course;
import com.college.deadlinedash.task.Task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UrgencyCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
	private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

	private final UrgencyCalculator calculator = new UrgencyCalculator();

	private Task task(int weight, int daysFromToday, boolean done) {
		Task t = new Task(new Course("Maths", "MA1"), "t", TODAY.plusDays(daysFromToday), weight, Instant.EPOCH);
		t.setDone(done);
		return t;
	}

	@ParameterizedTest(name = "weight {0}, due in {1} days -> {2} {3}")
	@CsvSource({
			"20,  0, 2000, URGENT",   // due today
			"20,  3,  500, URGENT",   // exactly on the URGENT boundary
			"30,  6,  429, SOON",     // 3000/7 = 428.57 rounds to 429
			"10,  9,  100, LATER",
			"20, -1, 2000, OVERDUE",  // yesterday
			"1,  -5,  100, OVERDUE",  // far overdue still scores from effectiveDays = 0
			"21,  3,  525, URGENT",
			"19,  3,  475, SOON",     // just under URGENT
			"15,  9,  150, SOON",     // exactly on the SOON boundary
			"14,  9,  140, LATER",    // just under SOON
	})
	void scoreAndBand(int weight, int days, int expectedScore, UrgencyBand expectedBand) {
		UrgencyResult r = calculator.calculate(task(weight, days, false), CLOCK);

		assertThat(r.score()).isEqualTo(expectedScore);
		assertThat(r.band()).isEqualTo(expectedBand);
	}

	@Test
	void doneTaskScoresZeroAndIsDone() {
		UrgencyResult r = calculator.calculate(task(20, 0, true), CLOCK);

		assertThat(r.score()).isZero();
		assertThat(r.band()).isEqualTo(UrgencyBand.DONE);
	}

	@Test
	void overdueTaskThatIsDoneIsDoneNotOverdue() {
		assertThat(calculator.calculate(task(20, -3, true), CLOCK).band()).isEqualTo(UrgencyBand.DONE);
	}

	@Test
	void daysLeftIsNegativeWhenOverdue() {
		assertThat(calculator.calculate(task(20, -2, false), CLOCK).daysLeft()).isEqualTo(-2);
	}
}
