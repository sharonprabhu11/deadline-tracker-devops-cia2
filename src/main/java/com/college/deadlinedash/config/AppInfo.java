package com.college.deadlinedash.config;

/** What the footer and /api/info show. */
public record AppInfo(String version, String commitSha, String podName, String buildTime) {
}
