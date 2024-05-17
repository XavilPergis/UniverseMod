package net.xavil.ultraviolet.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL45C;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlFence;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.collections.iterator.IteratorInt;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.ultraviolet.client.screen.layer.AxisMapping;
import net.xavil.ultraviolet.client.screen.layer.Histogram;

public final class AverageLuminanceComputer implements Disposable {

	public static final AverageLuminanceComputer INSTANCE = new AverageLuminanceComputer(3);

	public static final int HISTOGRAM_SIZE = 256;
	public static final int HISTOGRAM_SIZE_BYTES = 4 * HISTOGRAM_SIZE;
	private static final double TEMPORAL_FACTOR = 0.05;

	private final GlBuffer histogramBuffer;
	private final GlBuffer readbackBuffer;
	private final ByteBuffer readbackPointer;
	private GlTexture2d luminanceImage;

	private AxisMapping axisMapping = new AxisMapping.Log(2, 1e-2, 1e4);

	private final GlFence[] fences;
	private final IntBuffer[] readbackPointers;
	private final Histogram[] histograms;
	private int currentFence = 0;

	private double currentAverageBrightness = 1.0;
	// private float minLogLuminance = -10, maxLogLuminance = 2;
	private double minLuminance = 1e-2, maxLuminance = 1e4;

	public AverageLuminanceComputer(int maxInFlight) {
		this.fences = new GlFence[maxInFlight];
		this.histogramBuffer = new GlBuffer();
		this.readbackBuffer = new GlBuffer();
		this.readbackPointers = new IntBuffer[maxInFlight];
		this.histograms = new Histogram[maxInFlight];

		// no flags, just a simple GPU-only buffer :3
		this.histogramBuffer.allocateImmutableStorage(HISTOGRAM_SIZE_BYTES, 0);
		// client-accessible buffer to copy the histogram into
		this.readbackBuffer.allocateImmutableStorage(maxInFlight * HISTOGRAM_SIZE_BYTES,
				GL45C.GL_MAP_READ_BIT | GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_CLIENT_STORAGE_BIT);
		this.readbackPointer = this.readbackBuffer.map(GL45C.GL_MAP_READ_BIT | GL45C.GL_MAP_PERSISTENT_BIT);

		for (int i = 0; i < maxInFlight; ++i) {
			this.fences[i] = new GlFence();
			this.histograms[i] = new Histogram("Log Luminance", HISTOGRAM_SIZE, this.axisMapping);
			this.readbackPointer.clear();
			this.readbackPointers[i] = this.readbackPointer
					.slice(HISTOGRAM_SIZE_BYTES * i, HISTOGRAM_SIZE_BYTES)
					.asIntBuffer();
		}
	}

	@Override
	public void close() {
		this.histogramBuffer.close();
		this.readbackBuffer.close();
		if (this.luminanceImage != null)
			this.luminanceImage.close();
		for (final var fence : this.fences)
			fence.close();
	}

	private static Vec2i getWindowSize() {
		final var window = Minecraft.getInstance().getWindow();
		return new Vec2i(window.getWidth(), window.getHeight());
	}

	private void dispatch(GlTexture2d sceneTexture) {
		final var windowSize = getWindowSize();
		// final var desiredSize = windowSize.div(2);
		final var desiredSize = windowSize;
		if (this.luminanceImage == null || !this.luminanceImage.size().d2().equals(desiredSize)) {
			if (this.luminanceImage != null)
				this.luminanceImage.close();
			this.luminanceImage = new GlTexture2d(false);
			this.luminanceImage.createStorage(GlTexture.Format.R16_FLOAT, desiredSize.x, desiredSize.y);
		}

		// // image unit 1 is shared between both compute passes, we dont need to rebind
		// GL45C.glBindImageTexture(1, this.luminanceImage.id, 0, false, 0,
		// GL45C.GL_READ_ONLY, GL45C.GL_R16F);

		final var preprocessShader = UltravioletShaders.SHADER_COMPUTE_LUMINANCE_PREPROCESS.get();
		preprocessShader.setUniformSampler("uSceneTexture", sceneTexture);
		preprocessShader.setUniformImage("oLuminanceTexture", this.luminanceImage, GlTexture.ImageAccess.WRITE);
		preprocessShader.bind();
		GL45C.glDispatchCompute(windowSize.x / 16, windowSize.y / 16, 1);
		GL45C.glMemoryBarrier(GL45C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		this.histogramBuffer.slice().clear(GL45C.GL_R8UI);
		GL45C.glMemoryBarrier(GL45C.GL_SHADER_STORAGE_BARRIER_BIT);

		final var histogramShader = UltravioletShaders.SHADER_COMPUTE_LUMINANCE_HISTOGRAM.get();
		histogramShader.setUniformf("uMinLogLuminance", Math.log(this.minLuminance) / Math.log(2.0));
		histogramShader.setUniformf("uMaxLogLuminance", Math.log(this.maxLuminance) / Math.log(2.0));
		histogramShader.setStorageBuffer("bHistogram", this.histogramBuffer.slice());
		histogramShader.setUniformImage("uSceneLuminance", this.luminanceImage, GlTexture.ImageAccess.READ);
		histogramShader.bind();
		GL45C.glDispatchCompute(windowSize.x / 16, windowSize.y / 16, 1);
		// GL45C.glDispatchCompute(1, 1, 1);

		// copy to persistently mapped readback buffer (device -> host buffer hopefully)
		final var readbackSlice = this.readbackBuffer.slice(HISTOGRAM_SIZE_BYTES * this.currentFence,
				HISTOGRAM_SIZE_BYTES);
		this.histogramBuffer.slice().copyTo(readbackSlice, HISTOGRAM_SIZE_BYTES);
		// TODO: do i even need a barrier here??? i think i do??????
		GL45C.glMemoryBarrier(GL45C.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
		this.fences[this.currentFence].signalFence();
	}

	private Histogram getHistogram() {
		// final var buf =
		// this.readbackPointers[this.currentFence].order(ByteOrder.nativeOrder());
		final var byteBuf = this.readbackPointer.slice(HISTOGRAM_SIZE_BYTES * this.currentFence, HISTOGRAM_SIZE_BYTES)
				.order(ByteOrder.nativeOrder());
		final var buf = byteBuf.asIntBuffer();
		buf.clear();
		final var histogram = this.histograms[this.currentFence];
		histogram.setBins(IteratorInt.fromBuffer(buf).limit(HISTOGRAM_SIZE));
		return histogram;
	}

	// assumes the current fence is already signalled and we wont run into sync
	// issues
	// TODO: i have no idea how this works
	private double computeAverageBrightness() {
		final var histogram = getHistogram();

		// compute a convolved histogram. the average of the convolved histogram should
		// be weighted more greatly towards more diffuse luminance peaks, hopefully
		// reducing the contribution of very spikey bins.
		final int convolutionRadius = 1;
		final var convolvedBins = new int[histogram.size()];
		for (int i = 0; i < histogram.size(); ++i) {
			final var boundLo = Math.max(0, i - convolutionRadius);
			final var boundHi = Math.min(histogram.size(), i + convolutionRadius + 1);
			final var sampleCount = boundHi - boundLo;
			for (int j = boundLo; j < boundHi; ++j)
				convolvedBins[i] += histogram.get(j);
			convolvedBins[i] /= sampleCount;
		}

		final double brightnessThreshold = 0.025;
		double total = 0.0;
		double maxValue = 0.0;
		int pixelsAboveThreshold = 0;
		for (int i = 0; i < histogram.size(); ++i) {
			// treat middle of bin as luminance for entire bin
			final var binBrightness = histogram.mapping.unmap((i + 0.5) / histogram.size());
			if (binBrightness < brightnessThreshold)
				continue;
			// // convolve bins
			// pixelsAboveThreshold += histogram.get(i);
			// total += histogram.get(i) * binBrightness;
			pixelsAboveThreshold += convolvedBins[i];
			total += convolvedBins[i] * binBrightness;
			if (convolvedBins[i] * binBrightness > maxValue) {
				maxValue = convolvedBins[i] * binBrightness;
			}
		}

		// final var thresholdRatio = Math.pow(pixelsAboveThreshold / (double) histogram.total(), 0.5);
		final var thresholdRatio = Math.pow(pixelsAboveThreshold / (double) histogram.total(), 1.0);
		// return Mth.lerp(thresholdRatio, 1.0, total / Math.max(1, pixelsAboveThreshold));
		// return Mth.lerp(thresholdRatio, 0.0001, total / Math.max(1, pixelsAboveThreshold));
		// return total / Math.max(1, pixelsAboveThreshold);
		return maxValue;

		// what percentage of pixels are above the threshold?
		// final var thresholdRatio = pixelsAboveThreshold / histogram.total();
		// final var maxLuminance = histogram.mapping.domain.max;
		// return Mth.lerp(thresholdRatio, 1.0, );

		// final var blackPixelCount = histogram.get(0);

		// // TODO: is this right? what if the window changed sizes between when the
		// // histogram was taken and now?
		// final var windowSize = getWindowSize();
		// final var totalPixelCount = windowSize.x * windowSize.y;
		// final var weightedLogAverage = weightedCount / Math.max(totalPixelCount -
		// blackPixelCount, 1.0) - 1.0;
		// // final var weightedLogAverage = weightedCount / totalPixelCount;
		// final var weightedAvgLum = Math.pow(2.0, Mth.lerp(weightedLogAverage / 254.0,
		// Math.log(this.minLuminance) / Math.log(2.0),
		// Math.log(this.maxLuminance) / Math.log(2.0)));

		// return weightedAvgLum;
	}

	public void compute(GlTexture2d sceneTexture) {
		final var dt = Minecraft.getInstance().getDeltaFrameTime();

		if (this.fences[this.currentFence].clientWaitSync().isSignaled) {
			// read data from histogram buffer
			final var averageBrightness = computeAverageBrightness();
			final double blendT;
			if (averageBrightness > this.currentAverageBrightness) {
				blendT = 1 - Math.exp(-dt * 0.005);
			} else {
				blendT = 1 - Math.exp(-dt * 0.1);
			}
			// final var blendT = 1 - Math.exp(-dt);
			// this.currentAverageBrightness = Mth.lerp(blendT, this.currentAverageBrightness, averageBrightness);
			this.currentAverageBrightness = averageBrightness;
		}

		// done using data - we can clobber the buffer now
		dispatch(sceneTexture);
		this.currentFence = (this.currentFence + 1) % this.fences.length;
	}

	public double currentAverageBrightness() {
		return this.currentAverageBrightness;
	}

}
