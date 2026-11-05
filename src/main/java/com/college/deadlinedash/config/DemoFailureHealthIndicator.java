package com.college.deadlinedash.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Rollback demo only. When deadlinedash.demo.fail-readiness=true this reports DOWN, so the
 * readiness probe fails and Kubernetes never sends traffic to the new pods. Normally UP.
 */
@Component("demoFailure")
public class DemoFailureHealthIndicator implements HealthIndicator {

	private final boolean failReadiness;

	public DemoFailureHealthIndicator(@Value("${deadlinedash.demo.fail-readiness:false}") boolean failReadiness) {
		this.failReadiness = failReadiness;
	}

	@Override
	public Health health() {
		if (failReadiness) {
			return Health.down().withDetail("reason", "deliberate failure for the rollback demo").build();
		}
		return Health.up().build();
	}
}
