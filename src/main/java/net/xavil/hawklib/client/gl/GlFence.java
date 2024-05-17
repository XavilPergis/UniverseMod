package net.xavil.hawklib.client.gl;

import java.time.Duration;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.Disposable;

// sync objects are weird and arent like other opengl objects, so they dont inherit from GlObject.
public final class GlFence implements Disposable {
	private long id = 0;

	public static enum WaitResult {
		// the GlFence object contains no GL sync object to query the status of.
		UNINITALIZED(false),
		FAILED(false),
		// the wait operation timed out
		TIMED_OUT(false),
		// the sync object became signaled during the wait operation
		SIGNALED(true),
		// the sync object became signaled some time before the wait operation
		ALREADY_SIGNALED(true);

		public final boolean isSignaled;

		private WaitResult(boolean isSignaled) {
			this.isSignaled = isSignaled;
		}

		public static WaitResult from(int id) {
			return switch (id) {
				case GL45C.GL_WAIT_FAILED -> FAILED;
				case GL45C.GL_TIMEOUT_EXPIRED -> TIMED_OUT;
				case GL45C.GL_CONDITION_SATISFIED -> SIGNALED;
				case GL45C.GL_ALREADY_SIGNALED -> ALREADY_SIGNALED;
				default -> null;
			};
		}
	}

	@Override
	public void close() {
		GL45C.glDeleteSync(this.id);
	}

	public long id() {
		return this.id;
	}

	public void signalFence() {
		// lwjgl doesnt let us forgo this check, even though the opengl docs say that
		// passing 0 here has no effect
		if (this.id != 0)
			GL45C.glDeleteSync(this.id);
		// i like how this call takes two parameters, but both parameters HAVE to be
		// these values lol
		this.id = GL45C.glFenceSync(GL45C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
	}

	public void waitSync() {
		GL45C.glWaitSync(this.id, 0, GL45C.GL_TIMEOUT_IGNORED);
	}

	public WaitResult clientWaitSync(Duration timeout, boolean retryUntilSignalled) {
		WaitResult status = WaitResult.UNINITALIZED;
		if (this.id != 0) {
			do {
				status = WaitResult.from(GL45C.glClientWaitSync(this.id, 0, timeout.toNanos()));
			} while (retryUntilSignalled && !status.isSignaled);
		}
		return status;
	}

	public WaitResult clientWaitSync(Duration timeout) {
		return clientWaitSync(timeout, false);
	}

	public WaitResult clientWaitSync() {
		return clientWaitSync(Duration.ofMillis(100), true);
	}

}