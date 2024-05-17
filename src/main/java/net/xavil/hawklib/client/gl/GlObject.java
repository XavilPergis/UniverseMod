package net.xavil.hawklib.client.gl;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.IntConsumer;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Box;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.ErrorRatelimiter;
import net.xavil.hawklib.HawkLib;

public abstract class GlObject implements Disposable {

	protected static final Cleaner CLEANER = Cleaner.create();

	public static enum ObjectType {
		// why is GL_TEXTURE not in core profile but everything else is ??????
		TEXTURE(GL45.GL_TEXTURE, "Texture", GL45C::glDeleteTextures),
		BUFFER(GL45C.GL_BUFFER, "Buffer", GL45C::glDeleteBuffers),
		VERTEX_ARRAY(GL45C.GL_VERTEX_ARRAY, "Vertex Array", GL45C::glDeleteVertexArrays),
		FRAMEBUFFER(GL45C.GL_FRAMEBUFFER, "Framebuffer", GL45C::glDeleteFramebuffers),
		RENDERBUFFER(GL45C.GL_RENDERBUFFER, "Renderbuffer", GL45C::glDeleteRenderbuffers),
		SHADER(GL45C.GL_SHADER, "Shader Stage", GL45C::glDeleteShader),
		PROGRAM(GL45C.GL_PROGRAM, "Shader", GL45C::glDeleteProgram);

		public final int glId;
		public final String description;
		public final IntConsumer releaser;

		private ObjectType(int glId, String description, IntConsumer releaser) {
			this.glId = glId;
			this.description = description;
			this.releaser = releaser;
		}
	}

	private static final class DebugInfo {
		private final Cleaner.Cleanable cleanable;
		private final Box<String> cachedDebugDescription = new Box<>();
		private final Box<StackTraceElement[]> stackTrace = Assert.EXPENSIVE_DEBUG_MODE ? new Box<>() : null;

		private static ErrorRatelimiter THROTTLER = new ErrorRatelimiter(Duration.ofMillis(2000L), 6);

		public DebugInfo(Box<Boolean> releasedPointer) {
			this.cleanable = CLEANER.register(this, handleCleanup(
					releasedPointer,
					this.cachedDebugDescription,
					this.stackTrace));
			if (this.stackTrace != null)
				this.stackTrace.set(Thread.currentThread().getStackTrace());
		}

		private static Runnable handleCleanup(Box<Boolean> releasedFlag,
				Box<String> debugDescription, Box<StackTraceElement[]> stackTrace) {
			return () -> {
				if (releasedFlag.get())
					return;

				// ratelimit if we had an oopsie and started leaking tons of stuff cuz it could
				// get really really spammy
				if (THROTTLER.throttle())
					return;

				HawkLib.LOGGER.error("{} was never realeased! This is a resource leak!", debugDescription.get());
				if (stackTrace != null) {
					HawkLib.LOGGER.error("Object instantiated at:");
					for (final var elem : stackTrace.get())
						HawkLib.LOGGER.error("\t- " + elem);
				}
			};
		}

	}

	public final int id;
	private final ObjectType objectType;

	// does this object have ownership of the associated GL resource? ie, is it
	// responsible for deleting the resource when done?
	protected final boolean owned;
	protected final Box<Boolean> released = new Box<>(false);

	private String debugName;
	private final DebugInfo debugInfo;

	public GlObject(ObjectType type, int id, boolean owned) {
		this.id = id;
		this.objectType = type;
		this.owned = owned;

		this.debugInfo = owned && Assert.DEBUG_MODE ? new DebugInfo(this.released) : null;

		if (this.debugInfo != null)
			this.debugInfo.cachedDebugDescription.set(debugDescription());
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void close() {
		if (this.released.set(true)) {
			HawkLib.LOGGER.error("Tried to destroy {} more than once!", this);
			return;
		}
		if (this.owned)
			this.objectType.releaser.accept(this.id);
		if (this.debugInfo != null)
			this.debugInfo.cleanable.clean();
	}

	public String getDebugName() {
		return this.debugName;
	}

	public void setDebugName(@Nullable String debugName) {
		if (Objects.equals(this.debugName, debugName))
			return;
		this.debugName = debugName;
		GL45C.glObjectLabel(this.objectType.glId, this.id, debugName);
		if (this.debugInfo.cachedDebugDescription != null)
			this.debugInfo.cachedDebugDescription.set(debugDescription());
	}

	public String debugDescription() {
		var desc = this.objectType.description + " " + this.id;
		if (this.debugName != null)
			desc += " (\"" + this.debugName + "\")";
		return desc;
	}

	@Override
	public String toString() {
		return "[" + debugDescription() + "]@" + Integer.toHexString(hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof GlObject other
				&& this.objectType == other.objectType
				&& this.id == other.id;
	}

	public final boolean isDestroyed() {
		return this.released.get();
	}

}
