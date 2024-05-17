package net.xavil.hawklib;

import java.time.Duration;
import java.time.Instant;

public final class ErrorRatelimiter {

	private final Duration ratelimitDuration;
	private final int errorsPerBucket;

	private Instant lastError = Instant.EPOCH;
	private int errorsInBucket = 0;

	public ErrorRatelimiter(Duration ratelimitDuration, int errorsPerBucket) {
		this.ratelimitDuration = ratelimitDuration;
		this.errorsPerBucket = errorsPerBucket;
	}

	/**
	 * Update the throttle state and check if messages should be ratelimited.
	 * 
	 * @return {@code true} if the error message should be suppressed.
	 */
	public boolean throttle() {
		final var now = Instant.now();
		if (this.errorsInBucket >= this.errorsPerBucket
				&& now.isBefore(this.lastError.plus(this.ratelimitDuration)))
			return true;
		if (now.isAfter(this.lastError.plus(this.ratelimitDuration))) {
			this.lastError = now;
			this.errorsInBucket = 0;
		} else {
			this.errorsInBucket += 1;
		}
		return false;
	}

}