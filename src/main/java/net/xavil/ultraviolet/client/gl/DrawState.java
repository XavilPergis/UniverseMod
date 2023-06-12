package net.xavil.ultraviolet.client.gl;

import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;

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
		private GlState.PolygonMode polygonMode = null;

		private Boolean enableCulling = null;
		private GlState.CullFace cullFace = null;
		private GlState.FrontFace frontFace = null;

		private Boolean depthMask = null;
		private Boolean depthTestingEnabled = null;
		private GlState.DepthFunc depthFunc = null;

		private Boolean blendingEnabled = null;
		private GlState.BlendEquation blendEquationRgb = null;
		private GlState.BlendEquation blendEquationAlpha = null;
		private GlState.BlendFactor blendFactorSrcRgb = null;
		private GlState.BlendFactor blendFactorDstRgb = null;
		private GlState.BlendFactor blendFactorSrcAlpha = null;
		private GlState.BlendFactor blendFactorDstAlpha = null;

		public Builder withPolygonMode(GlState.PolygonMode state) {
			this.polygonMode = state;
			return this;
		}

		public Builder enableCulling(boolean enable) {
			this.enableCulling = enable;
			return this;
		}

		public Builder cullFace(GlState.CullFace cullFace) {
			this.cullFace = cullFace;
			return this;
		}

		public Builder frontFace(GlState.FrontFace frontFace) {
			this.frontFace = frontFace;
			return this;
		}

		public Builder enableCulling(GlState.CullFace cullFace, GlState.FrontFace frontFace) {
			this.enableCulling = true;
			this.cullFace = cullFace;
			this.frontFace = frontFace;
			return this;
		}

		public Builder depthMask(boolean depthMask) {
			this.depthMask = depthMask;
			return this;
		}

		public Builder enableDepthTest(boolean enable) {
			this.depthTestingEnabled = enable;
			return this;
		}

		public Builder enableDepthTest(GlState.DepthFunc depthFunc) {
			this.depthTestingEnabled = true;
			this.depthFunc = depthFunc;
			return this;
		}

		public Builder enableBlending(boolean enable) {
			this.blendingEnabled = enable;
			return this;
		}

		public Builder blendFunc(GlState.BlendFactor srcFactor, GlState.BlendFactor dstFactor) {
			this.blendingEnabled = true;
			this.blendFactorSrcRgb = srcFactor;
			this.blendFactorSrcAlpha = srcFactor;
			this.blendFactorDstRgb = dstFactor;
			this.blendFactorDstAlpha = dstFactor;
			return this;
		}

		public Builder blendFunc(GlState.BlendFactor blendFactorSrcRgb, GlState.BlendFactor blendFactorSrcAlpha,
				GlState.BlendFactor blendFactorDstRgb, GlState.BlendFactor blendFactorDstAlpha) {
			this.blendingEnabled = true;
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
			return this;
		}

		public Builder enableBlending(
				GlState.BlendEquation blendEquation,
				GlState.BlendFactor blendFactorSrc,
				GlState.BlendFactor blendFactorDst) {
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
				GlState.BlendEquation blendEquationRgb,
				GlState.BlendEquation blendEquationAlpha,
				GlState.BlendFactor blendFactorSrcRgb,
				GlState.BlendFactor blendFactorDstRgb,
				GlState.BlendFactor blendFactorSrcAlpha,
				GlState.BlendFactor blendFactorDstAlpha) {
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
			if (this.polygonMode         != null) states.push(new PolygonMode  (this.polygonMode));
			if (this.enableCulling       != null) states.push(new Culling      (this.enableCulling));
			if (this.cullFace            != null) states.push(new CullFace     (this.cullFace));
			if (this.frontFace           != null) states.push(new FrontFace    (this.frontFace));
			if (this.depthMask           != null) states.push(new DepthMask    (this.depthMask));
			if (this.depthTestingEnabled != null) states.push(new DepthTesting (this.depthTestingEnabled));
			if (this.depthFunc           != null) states.push(new DepthFunc    (this.depthFunc));
			if (this.blendingEnabled     != null) states.push(new Blending     (this.blendingEnabled));
			if (this.blendEquationRgb    != null) states.push(new BlendEquation(this.blendEquationRgb, this.blendEquationAlpha));
			if (this.blendFactorSrcRgb   != null) states.push(new BlendFunc    (this.blendFactorSrcRgb, this.blendFactorDstRgb, this.blendFactorSrcAlpha, this.blendFactorDstAlpha));
			// @formatter:on

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

	public static final class DepthMask extends DrawState {
		public final boolean depthMask;

		public DepthMask(boolean depthMask) {
			this.depthMask = depthMask;
		}

		@Override
		public void apply(GlState state) {
			state.depthMask(this.depthMask);
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
