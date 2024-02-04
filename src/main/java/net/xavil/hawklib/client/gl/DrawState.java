package net.xavil.hawklib.client.gl;

import javax.annotation.Nullable;

import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;

public abstract class DrawState {

	public static final DrawState EMPTY = new DrawState() {
		@Override
		public void apply(GlState state) {
		}
	};

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private GlState.PolygonMode polygonMode = GlState.PolygonMode.FILL;

		private Boolean enableCulling = Boolean.TRUE;
		private GlState.CullFace cullFace = GlState.CullFace.BACK;
		private GlState.FrontFace frontFace = GlState.FrontFace.CCW;

		private Boolean depthWriteEnabled = Boolean.TRUE;
		private Boolean depthTestingEnabled = Boolean.FALSE;
		private GlState.DepthFunc depthFunc = GlState.DepthFunc.LESS;

		private Boolean blendingEnabled = Boolean.FALSE;
		private GlState.BlendEquation blendEquationRgb = GlState.BlendEquation.ADD;
		private GlState.BlendEquation blendEquationAlpha = GlState.BlendEquation.ADD;
		private GlState.BlendFactor blendFactorSrcRgb = GlState.BlendFactor.SRC_ALPHA;
		private GlState.BlendFactor blendFactorDstRgb = GlState.BlendFactor.ONE_MINUS_SRC_ALPHA;
		private GlState.BlendFactor blendFactorSrcAlpha = GlState.BlendFactor.SRC_ALPHA;
		private GlState.BlendFactor blendFactorDstAlpha = GlState.BlendFactor.ONE_MINUS_SRC_ALPHA;

		public Builder withPolygonMode(@Nullable GlState.PolygonMode state) {
			this.polygonMode = state;
			return this;
		}

		public Builder enableCulling(@Nullable Boolean enable) {
			this.enableCulling = enable;
			return this;
		}

		public Builder cullFace(@Nullable GlState.CullFace cullFace) {
			this.cullFace = cullFace;
			return this;
		}

		public Builder frontFace(@Nullable GlState.FrontFace frontFace) {
			this.frontFace = frontFace;
			return this;
		}

		public Builder enableCulling(@Nullable GlState.CullFace cullFace, @Nullable GlState.FrontFace frontFace) {
			this.enableCulling = true;
			this.cullFace = cullFace;
			this.frontFace = frontFace;
			return this;
		}

		public Builder enableDepthWrite(@Nullable Boolean enable) {
			this.depthWriteEnabled = enable;
			return this;
		}

		public Builder enableDepthTest(@Nullable Boolean enable) {
			this.depthTestingEnabled = enable;
			return this;
		}

		public Builder enableDepthTest(@Nullable GlState.DepthFunc depthFunc) {
			this.depthTestingEnabled = true;
			this.depthFunc = depthFunc;
			return this;
		}

		public Builder enableBlending(@Nullable Boolean enable) {
			this.blendingEnabled = enable;
			return this;
		}

		public Builder blendFunc(@Nullable GlState.BlendFactor srcFactor, @Nullable GlState.BlendFactor dstFactor) {
			this.blendingEnabled = true;
			this.blendFactorSrcRgb = srcFactor;
			this.blendFactorSrcAlpha = srcFactor;
			this.blendFactorDstRgb = dstFactor;
			this.blendFactorDstAlpha = dstFactor;
			return this;
		}

		public Builder blendFunc(@Nullable GlState.BlendFactor blendFactorSrcRgb,
				@Nullable GlState.BlendFactor blendFactorSrcAlpha,
				@Nullable GlState.BlendFactor blendFactorDstRgb,
				@Nullable GlState.BlendFactor blendFactorDstAlpha) {
			this.blendingEnabled = true;
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
			return this;
		}

		public Builder enableBlending(
				@Nullable GlState.BlendEquation blendEquation,
				@Nullable GlState.BlendFactor blendFactorSrc,
				@Nullable GlState.BlendFactor blendFactorDst) {
			this.blendingEnabled = true;
			this.blendEquationRgb = blendEquation;
			this.blendEquationAlpha = blendEquation;
			this.blendFactorSrcRgb = blendFactorSrc;
			this.blendFactorSrcAlpha = blendFactorSrc;
			this.blendFactorDstRgb = blendFactorDst;
			this.blendFactorDstAlpha = blendFactorDst;
			return this;
		}

		public Builder enableBlending(
				@Nullable GlState.BlendEquation blendEquationRgb,
				@Nullable GlState.BlendEquation blendEquationAlpha,
				@Nullable GlState.BlendFactor blendFactorSrcRgb,
				@Nullable GlState.BlendFactor blendFactorDstRgb,
				@Nullable GlState.BlendFactor blendFactorSrcAlpha,
				@Nullable GlState.BlendFactor blendFactorDstAlpha) {
			this.blendingEnabled = true;
			this.blendEquationRgb = blendEquationRgb;
			this.blendEquationAlpha = blendEquationAlpha;
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
			return this;
		}

		public Builder enableAlphaBlending() {
			this.blendingEnabled = true;
			this.blendEquationRgb = GlState.BlendEquation.ADD;
			this.blendEquationAlpha = GlState.BlendEquation.ADD;
			this.blendFactorSrcRgb = GlState.BlendFactor.SRC_ALPHA;
			this.blendFactorSrcAlpha = GlState.BlendFactor.SRC_ALPHA;
			this.blendFactorDstRgb = GlState.BlendFactor.ONE_MINUS_SRC_ALPHA;
			this.blendFactorDstAlpha = GlState.BlendFactor.ONE_MINUS_SRC_ALPHA;
			return this;
		}

		public Builder enableAdditiveBlending() {
			this.blendingEnabled = true;
			this.blendEquationRgb = GlState.BlendEquation.ADD;
			this.blendEquationAlpha = GlState.BlendEquation.ADD;
			this.blendFactorSrcRgb = GlState.BlendFactor.SRC_ALPHA;
			this.blendFactorSrcAlpha = GlState.BlendFactor.SRC_ALPHA;
			this.blendFactorDstRgb = GlState.BlendFactor.ONE;
			this.blendFactorDstAlpha = GlState.BlendFactor.ONE;
			return this;
		}

		public DrawState.Multi build() {
			final var states = new Vector<DrawState>();

			// @formatter:off
			if (this.polygonMode       != null) states.push(new PolygonMode(this.polygonMode));
			if (this.enableCulling     != null) states.push(new Culling    (this.enableCulling));
			if (this.cullFace          != null) states.push(new CullFace   (this.cullFace));
			if (this.frontFace         != null) states.push(new FrontFace  (this.frontFace));
			if (this.depthWriteEnabled != null) states.push(new DepthWrite (this.depthWriteEnabled));
			// @formatter:on

			if (this.depthTestingEnabled != null) {
				states.push(new DepthTesting(this.depthTestingEnabled));
				if (this.depthTestingEnabled && this.depthFunc != null)
					states.push(new DepthFunc(this.depthFunc));
			}
			if (this.blendingEnabled != null) {
				states.push(new Blending(this.blendingEnabled));
				if (this.blendingEnabled && this.blendEquationRgb != null)
					states.push(new BlendEquation(this.blendEquationRgb, this.blendEquationAlpha));
				if (this.blendingEnabled && this.blendFactorSrcRgb != null)
					states.push(new BlendFunc(this.blendFactorSrcRgb, this.blendFactorDstRgb,
							this.blendFactorSrcAlpha, this.blendFactorDstAlpha));
			}

			states.optimize();
			return new DrawState.Multi(states);
		}

	}

	public static class Multi extends DrawState {
		public final ImmutableList<DrawState> children;

		public Multi(ImmutableList<DrawState> children) {
			this.children = children;
		}

		@Override
		public void apply(GlState state) {
			this.children.forEach(child -> child.apply(state));
		}
	}

	public abstract void apply(GlState state);

	public static final class PolygonMode extends DrawState {
		public final GlState.PolygonMode mode;

		public PolygonMode(GlState.PolygonMode mode) {
			this.mode = mode;
		}

		@Override
		public void apply(GlState state) {
			state.polygonMode(this.mode);
		}
	}

	public static final class Culling extends DrawState {
		public final boolean enableCulling;

		public Culling(boolean enableCulling) {
			this.enableCulling = enableCulling;
		}

		@Override
		public void apply(GlState state) {
			state.enableCull(this.enableCulling);
		}
	}

	public static final class CullFace extends DrawState {
		public final GlState.CullFace cullFace;

		public CullFace(GlState.CullFace cullFace) {
			this.cullFace = cullFace;
		}

		@Override
		public void apply(GlState state) {
			state.cullFace(this.cullFace);
		}
	}

	public static final class FrontFace extends DrawState {
		public final GlState.FrontFace frontFace;

		public FrontFace(GlState.FrontFace frontFace) {
			this.frontFace = frontFace;
		}

		@Override
		public void apply(GlState state) {
			state.frontFace(this.frontFace);
		}
	}

	public static final class DepthWrite extends DrawState {
		public final boolean depthWriteEnabled;

		public DepthWrite(boolean depthMask) {
			this.depthWriteEnabled = depthMask;
		}

		@Override
		public void apply(GlState state) {
			state.depthMask(this.depthWriteEnabled);
		}
	}

	public static final class DepthTesting extends DrawState {
		public final boolean depthTestingEnabled;

		public DepthTesting(boolean depthTestingEnabled) {
			this.depthTestingEnabled = depthTestingEnabled;
		}

		@Override
		public void apply(GlState state) {
			state.enableDepthTest(this.depthTestingEnabled);
		}
	}

	public static final class DepthFunc extends DrawState {
		public final GlState.DepthFunc depthFunc;

		public DepthFunc(GlState.DepthFunc depthFunc) {
			this.depthFunc = depthFunc;
		}

		@Override
		public void apply(GlState state) {
			state.depthFunc(this.depthFunc);
		}
	}

	public static final class Blending extends DrawState {
		public final boolean blendingEnabled;

		public Blending(boolean blendingEnabled) {
			this.blendingEnabled = blendingEnabled;
		}

		@Override
		public void apply(GlState state) {
			state.enableBlend(this.blendingEnabled);
		}
	}

	public static final class BlendEquation extends DrawState {
		public final GlState.BlendEquation blendEquationRgb;
		public final GlState.BlendEquation blendEquationAlpha;

		public BlendEquation(GlState.BlendEquation blendEquationRgb, GlState.BlendEquation blendEquationAlpha) {
			this.blendEquationRgb = blendEquationRgb;
			this.blendEquationAlpha = blendEquationAlpha;
		}

		@Override
		public void apply(GlState state) {
			state.blendEquation(this.blendEquationRgb, this.blendEquationAlpha);
		}
	}

	public static final class BlendFunc extends DrawState {
		public final GlState.BlendFactor blendFactorSrcRgb;
		public final GlState.BlendFactor blendFactorDstRgb;
		public final GlState.BlendFactor blendFactorSrcAlpha;
		public final GlState.BlendFactor blendFactorDstAlpha;

		public BlendFunc(GlState.BlendFactor blendFactorSrcRgb, GlState.BlendFactor blendFactorDstRgb,
				GlState.BlendFactor blendFactorSrcAlpha, GlState.BlendFactor blendFactorDstAlpha) {
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
		}

		@Override
		public void apply(GlState state) {
			state.blendFunc(this.blendFactorSrcRgb, this.blendFactorDstRgb,
					this.blendFactorSrcAlpha, this.blendFactorDstAlpha);
		}
	}

	public static final class ColorMask extends DrawState {
		public final boolean colorMaskR;
		public final boolean colorMaskG;
		public final boolean colorMaskB;
		public final boolean colorMaskA;

		public ColorMask(
				boolean colorMaskR,
				boolean colorMaskG,
				boolean colorMaskB,
				boolean colorMaskA) {
			this.colorMaskR = colorMaskR;
			this.colorMaskG = colorMaskG;
			this.colorMaskB = colorMaskB;
			this.colorMaskA = colorMaskA;
		}

		@Override
		public void apply(GlState state) {
			state.colorMask(this.colorMaskR, this.colorMaskG, this.colorMaskB, this.colorMaskA);
		}
	}

}
