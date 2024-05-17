package net.xavil.hawklib.client.gl;

public final class DrawState {

	public static final DrawState DEFAULT = new DrawState(new Builder());

	public final GlState.PolygonMode polygonMode;

	public final boolean enableCulling;
	public final GlState.CullFace cullFace;
	public final GlState.FrontFace frontFace;

	public final boolean depthWriteEnabled;
	public final boolean depthTestingEnabled;
	public final GlState.DepthFunc depthFunc;

	public final boolean blendingEnabled;
	public final GlState.BlendEquation blendEquationRgb;
	public final GlState.BlendEquation blendEquationAlpha;
	public final GlState.BlendFactor blendFactorSrcRgb;
	public final GlState.BlendFactor blendFactorDstRgb;
	public final GlState.BlendFactor blendFactorSrcAlpha;
	public final GlState.BlendFactor blendFactorDstAlpha;

	public final boolean colorMaskR;
	public final boolean colorMaskG;
	public final boolean colorMaskB;
	public final boolean colorMaskA;

	public DrawState(Builder builder) {
		this.polygonMode = builder.polygonMode;
		this.enableCulling = builder.enableCulling;
		this.cullFace = builder.cullFace;
		this.frontFace = builder.frontFace;
		this.depthWriteEnabled = builder.depthWriteEnabled;
		this.depthTestingEnabled = builder.depthTestingEnabled;
		this.depthFunc = builder.depthFunc;
		this.blendingEnabled = builder.blendingEnabled;
		this.blendEquationRgb = builder.blendEquationRgb;
		this.blendEquationAlpha = builder.blendEquationAlpha;
		this.blendFactorSrcRgb = builder.blendFactorSrcRgb;
		this.blendFactorDstRgb = builder.blendFactorDstRgb;
		this.blendFactorSrcAlpha = builder.blendFactorSrcAlpha;
		this.blendFactorDstAlpha = builder.blendFactorDstAlpha;
		this.colorMaskR = builder.colorMaskR;
		this.colorMaskG = builder.colorMaskG;
		this.colorMaskB = builder.colorMaskB;
		this.colorMaskA = builder.colorMaskA;
	}

	public Builder builder() {
		return new Builder(this);
	}

	public static final class Builder {
		private GlState.PolygonMode polygonMode = GlState.PolygonMode.FILL;

		private boolean enableCulling = false;
		private GlState.CullFace cullFace = GlState.CullFace.BACK;
		private GlState.FrontFace frontFace = GlState.FrontFace.CCW;

		private boolean depthWriteEnabled = true;
		private boolean depthTestingEnabled = false;
		private GlState.DepthFunc depthFunc = GlState.DepthFunc.LESS;

		private boolean blendingEnabled = false;
		private GlState.BlendEquation blendEquationRgb = GlState.BlendEquation.ADD;
		private GlState.BlendEquation blendEquationAlpha = GlState.BlendEquation.ADD;
		private GlState.BlendFactor blendFactorSrcRgb = GlState.BlendFactor.SRC_ALPHA;
		private GlState.BlendFactor blendFactorDstRgb = GlState.BlendFactor.ONE_MINUS_SRC_ALPHA;
		private GlState.BlendFactor blendFactorSrcAlpha = GlState.BlendFactor.SRC_ALPHA;
		private GlState.BlendFactor blendFactorDstAlpha = GlState.BlendFactor.ONE_MINUS_SRC_ALPHA;

		public boolean colorMaskR = true, colorMaskG = true, colorMaskB = true, colorMaskA = true;

		public Builder() {}

		public Builder(DrawState state) {
			this.polygonMode = state.polygonMode;
			this.enableCulling = state.enableCulling;
			this.cullFace = state.cullFace;
			this.frontFace = state.frontFace;
			this.depthWriteEnabled = state.depthWriteEnabled;
			this.depthTestingEnabled = state.depthTestingEnabled;
			this.depthFunc = state.depthFunc;
			this.blendingEnabled = state.blendingEnabled;
			this.blendEquationRgb = state.blendEquationRgb;
			this.blendEquationAlpha = state.blendEquationAlpha;
			this.blendFactorSrcRgb = state.blendFactorSrcRgb;
			this.blendFactorDstRgb = state.blendFactorDstRgb;
			this.blendFactorSrcAlpha = state.blendFactorSrcAlpha;
			this.blendFactorDstAlpha = state.blendFactorDstAlpha;
			this.colorMaskR = state.colorMaskR;
			this.colorMaskG = state.colorMaskG;
			this.colorMaskB = state.colorMaskB;
			this.colorMaskA = state.colorMaskA;
		}

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
			return enableCulling(true).cullFace(cullFace).frontFace(frontFace);
		}

		public Builder enableDepthWrite(boolean enable) {
			this.depthWriteEnabled = enable;
			return this;
		}

		public Builder enableDepthTest(boolean enable) {
			this.depthTestingEnabled = enable;
			return this;
		}

		public Builder depthFunc(GlState.DepthFunc depthFunc) {
			this.depthFunc = depthFunc;
			return this;
		}

		public Builder enableDepthTest(GlState.DepthFunc depthFunc) {
			return enableDepthTest(true).depthFunc(depthFunc);
		}
		
		public Builder enableDepthTest() {
			return enableDepthTest(GlState.DepthFunc.LESS);
		}
		
		public Builder enableBlending(boolean enable) {
			this.blendingEnabled = enable;
			return this;
		}

		public Builder blendFunc(GlState.BlendFactor blendFactorSrcRgb,
				GlState.BlendFactor blendFactorSrcAlpha,
				GlState.BlendFactor blendFactorDstRgb,
				GlState.BlendFactor blendFactorDstAlpha) {
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
			return this;
		}

		public Builder blendEquation(GlState.BlendEquation blendEquationRgb,
				GlState.BlendEquation blendEquationAlpha) {
			this.blendEquationRgb = blendEquationRgb;
			this.blendEquationAlpha = blendEquationAlpha;
			return this;
		}

		public Builder blendFunc(GlState.BlendFactor srcFactor, GlState.BlendFactor dstFactor) {
			return blendFunc(srcFactor, srcFactor, dstFactor, dstFactor);
		}

		public Builder blendEquation(GlState.BlendEquation blendEquation) {
			return blendEquation(blendEquation, blendEquation);
		}

		public Builder enableBlending(
				GlState.BlendEquation blendEquation,
				GlState.BlendFactor blendFactorSrc,
				GlState.BlendFactor blendFactorDst) {
			return enableBlending(true)
					.blendEquation(blendEquation)
					.blendFunc(blendFactorSrc, blendFactorDst);
		}

		public Builder enableBlending(
				GlState.BlendEquation blendEquationRgb,
				GlState.BlendEquation blendEquationAlpha,
				GlState.BlendFactor blendFactorSrcRgb,
				GlState.BlendFactor blendFactorDstRgb,
				GlState.BlendFactor blendFactorSrcAlpha,
				GlState.BlendFactor blendFactorDstAlpha) {
			return enableBlending(true)
					.blendEquation(blendEquationRgb, blendEquationAlpha)
					.blendFunc(blendFactorSrcRgb, blendFactorDstRgb, blendFactorSrcAlpha, blendFactorDstAlpha);
		}

		public Builder enableAlphaBlending() {
			return enableBlending(GlState.BlendEquation.ADD,
					GlState.BlendFactor.SRC_ALPHA,
					GlState.BlendFactor.ONE_MINUS_SRC_ALPHA);
		}

		public Builder enableAdditiveBlending() {
			return enableBlending(GlState.BlendEquation.ADD,
					GlState.BlendFactor.SRC_ALPHA,
					GlState.BlendFactor.ONE);
		}

		public Builder colorMask(boolean r, boolean g, boolean b, boolean a) {
			this.colorMaskR = r;
			this.colorMaskG = g;
			this.colorMaskB = b;
			this.colorMaskA = a;
			return this;
		}

		public DrawState build() {
			return new DrawState(this);
		}

	}

	public void apply() {
		GlManager.polygonMode(this.polygonMode);
		GlManager.enableCull(this.enableCulling);
		GlManager.cullFace(this.cullFace);
		GlManager.frontFace(this.frontFace);
		GlManager.depthMask(this.depthWriteEnabled);
		GlManager.enableDepthTest(this.depthTestingEnabled);
		GlManager.depthFunc(this.depthFunc);
		GlManager.enableBlend(this.blendingEnabled);
		GlManager.blendEquation(this.blendEquationRgb, this.blendEquationAlpha);
		GlManager.blendFunc(this.blendFactorSrcRgb, this.blendFactorDstRgb,
				this.blendFactorSrcAlpha, this.blendFactorDstAlpha);
		GlManager.colorMask(this.colorMaskR, this.colorMaskG, this.colorMaskB, this.colorMaskA);

	}

}
