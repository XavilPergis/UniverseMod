package net.xavil.ultraviolet.client;

import net.xavil.hawklib.ColorSpline;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture1d;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;

public final class ClientNodeInfo implements Disposable {
	public final UnaryCelestialNode node;

	public boolean wasUsedThisFrame = false;
	public GlTexture1d gasGiantGradient;

	public ClientNodeInfo(UnaryCelestialNode node) {
		this.node = node;

		if (node instanceof PlanetaryCelestialNode planetNode
				&& planetNode.type == PlanetaryCelestialNode.Type.GAS_GIANT) {
			this.gasGiantGradient = generateGasGiantGradientTexture(planetNode);
		}

	}

	@Override
	public void close() {
		if (this.gasGiantGradient != null)
			this.gasGiantGradient.close();
	}

	private GlTexture1d generateGasGiantGradientTexture(PlanetaryCelestialNode node) {
		final var rng = new SplittableRng(node.seed);

		// final var palette = getColorTable();

		final var colorSpline = new ColorSpline();

		// colorSpline.addControlPoint(0, ColorRgba.BLACK);
		colorSpline.addControlPoint(0, ColorRgba.RED);
		// colorSpline.addControlPoint(0.7f, ColorRgba.RED);
		// colorSpline.addControlPoint(0, ColorRgba.WHITE);
		colorSpline.addControlPoint(1, ColorRgba.WHITE);

		// final var pickingRng = rng.rng("picking");
		// ColorRgba endColor = palette.pick(pickingRng);
		// endColor = endColor == null ? ColorRgba.MAGENTA : endColor;

		// float t = 0;
		// rng.push("spline");
		// while (t < 1) {
		// final var color = palette.pick(pickingRng);
		// if (color == null)
		// break;
		// colorSpline.addControlPoint(t, color);
		// t += rng.weightedDouble("t", 4.0, 0.6, 0.3);
		// }
		// rng.pop();
		// colorSpline.addControlPoint(1f, endColor);

		return ColorSpline.createGradientTextureFromSpline(colorSpline, 512, GlTexture.Format.RGBA8_UINT_NORM);
	}

}