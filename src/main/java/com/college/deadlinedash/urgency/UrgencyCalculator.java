package com.college.deadlinedash.urgency;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.college.deadlinedash.task.Task;

import org.springframework.stereotype.Component;

/**
 * The unique feature. Takes a Clock instead of calling LocalDate.now(),
 * so tests can fix "today".
 *
 * daysLeft      = days from today to the due date (negative if overdue)
 * effectiveDays = max(daysLeft, 0)
 * score         = round(weight * 100 / (effectiveDays + 1))
 */
@Component
public class UrgencyCalculator {

	// Band thresholds: named so they are easy to tune.
	public static final int URGENT_THRESHOLD = 500;
	public static final int SOON_THRESHOLD = 150;

	public UrgencyResult calculate(Task task, Clock clock) {
		LocalDate today = LocalDate.now(clock);
		long daysLeft = ChronoUnit.DAYS.between(today, task.getDueDate());

		if (task.isDone()) {
			return new UrgencyResult(0, UrgencyBand.DONE, daysLeft);
		}

		long effectiveDays = Math.max(daysLeft, 0);
		int score = (int) Math.round(task.getWeight() * 100.0 / (effectiveDays + 1));

		UrgencyBand band;
		if (daysLeft < 0) {
			band = UrgencyBand.OVERDUE;
		} else if (score >= URGENT_THRESHOLD) {
			band = UrgencyBand.URGENT;
		} else if (score >= SOON_THRESHOLD) {
			band = UrgencyBand.SOON;
		} else {
			band = UrgencyBand.LATER;
		}
		return new UrgencyResult(score, band, daysLeft);
	}
}
