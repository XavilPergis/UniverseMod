package net.xavil.hawklib.client.gl;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
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
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.collections.iterator.IntoIterator;

public abstract class GlObject implements Disposable {

	protected static final Cleaner CLEANER = Cleaner.create();

	public static final class Deleter {
		public final Consumer<int[]> bulk;
		public final IntConsumer single;

		public Deleter(Consumer<int[]> bulk, IntConsumer single) {
			this.bulk = bulk;
			this.single = single;
		}

		public Deleter(IntConsumer single) {
			this(ids -> {
				for (final int id : ids)
					single.accept(id);
			}, single);
		}
	}

	public static enum ObjectType {
		// why is GL_TEXTURE not in core profile but everything else is ??????
		TEXTURE(GL45.GL_TEXTURE, "Texture",
				new Deleter(GL45C::glDeleteTextures, GL45C::glDeleteTextures)),
		BUFFER(GL45C.GL_BUFFER, "Buffer",
				new Deleter(GL45C::glDeleteBuffers, GL45C::glDeleteBuffers)),
		VERTEX_ARRAY(GL45C.GL_VERTEX_ARRAY, "Vertex Array",
				new Deleter(GL45C::glDeleteVertexArrays, GL45C::glDeleteVertexArrays)),
		FRAMEBUFFER(GL45C.GL_FRAMEBUFFER, "Framebuffer",
				new Deleter(GL45C::glDeleteFramebuffers, GL45C::glDeleteFramebuffers)),
		RENDERBUFFER(GL45C.GL_RENDERBUFFER, "Renderbuffer",
				new Deleter(GL45C::glDeleteRenderbuffers, GL45C::glDeleteRenderbuffers)),
		SHADER(GL45C.GL_SHADER, "Shader Stage",
				new Deleter(GL45C::glDeleteShader)),
		PROGRAM(GL45C.GL_PROGRAM, "Shader",
				new Deleter(GL45C::glDeleteProgram)),
		QUERY(GL45C.GL_QUERY, "Query",
				new Deleter(GL45C::glDeleteQueries, GL45C::glDeleteQueries));

		public final int glId;
		public final String description;
		public final Deleter releaser;

		private ObjectType(int glId, String description, Deleter releaser) {
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

		GlPerf.objectCreated(type);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void close() {
		if (this.released.set(true)) {
			HawkLib.LOGGER.error("Tried to destroy {} more than once!", this);
			return;
		}
		GlPerf.objectDestroyed(this.objectType);
		if (this.owned)
			this.objectType.releaser.single.accept(this.id);
		if (this.debugInfo != null)
			this.debugInfo.cleanable.clean();
	}

	public static <T extends GlObject> void closeBulk(ObjectType objectType, IntoIterator<T> objects) {
		final var iter = objects.iter().filterCast(GlObject.class);
		final var ids = new VectorInt(iter.sizeHint().lowerBound());
		try {
			iter.forEach(object -> {
				if (object.objectType != objectType)
					throw new IllegalArgumentException();
				if (object.owned)
					ids.push(object.id);
				if (object.debugInfo != null)
					object.debugInfo.cleanable.clean();
			});
		} finally {
			objectType.releaser.bulk.accept(ids.backingStorage());
		}
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
