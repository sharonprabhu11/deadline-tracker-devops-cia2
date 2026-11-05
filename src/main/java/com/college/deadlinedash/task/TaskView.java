package com.college.deadlinedash.task;

import com.college.deadlinedash.urgency.UrgencyResult;

/** A task plus its computed urgency, ready for the page. */
public record TaskView(Task task, UrgencyResult urgency) {
}
