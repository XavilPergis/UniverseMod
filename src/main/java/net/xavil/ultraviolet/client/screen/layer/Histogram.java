package net.xavil.ultraviolet.client.screen.layer;

import java.util.function.Consumer;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.Interval;

public final class Histogram {
	private final Interval domain;
	private final String inputLabel;
	private final int[] bins;

	private final Vector<Double> outliersLo = new Vector<>(), outliersHi = new Vector<>();
	private int outliersLoCount, outliersHiCount;

	private int total = 0;

	public Histogram(String inputLabel, Interval domain, int binCount) {
		this.inputLabel = inputLabel;
		this.bins = new int[binCount];
		this.domain = domain;
	}

	public void reset() {
		this.total = 0;
		for (int i = 0; i < this.bins.length; ++i)
			this.bins[i] = 0;
		this.outliersLo.clear();
		this.outliersHi.clear();
		this.outliersLoCount = this.outliersHiCount = 0;
	}

	public void insert(double value) {
		this.total += 1;
		final var t = Mth.inverseLerp(value, this.domain.lower, this.domain.higher);
		if (t < 0) {
			if (this.outliersLoCount < 20)
				this.outliersLo.push(value);
			this.outliersLoCount += 1;
		} else if (t >= 1) {
			if (this.outliersHiCount < 20)
				this.outliersHi.push(value);
			this.outliersHiCount += 1;
		} else {
			final var bin = Mth.floor(t * this.bins.length);
			this.bins[bin] += 1;
		}
	}

	private static final char[] BAR_CHARS = { ' ', '▏', '▎', '▍', '▌', '▋', '▊', '▉', '█' };

	private void makeBar(StringBuilder sb, double barPercent, int barLength) {
		double w = barPercent * barLength, ws = 1.0;
		for (int j = 0; j < barLength; ++j) {
			double p = Mth.clamp(w, 0, 1);
			w -= ws;
			sb.append(BAR_CHARS[Mth.floor(p * (BAR_CHARS.length - 1))]);
		}
	}

	private void makeBar(StringBuilder sb, String label, char ch, int barLength) {
		if (label != null && !label.isEmpty()) {
			for (int j = 0; j < barLength; ++j) {
				if (j < 5)
					sb.append(ch);
				else if (j < 6)
					sb.append(' ');
				else if (j < 6 + label.length())
					sb.append(label.charAt(j - 6));
				else if (j < 7 + label.length())
					sb.append(' ');
				else
					sb.append(ch);
			}
		} else {
			for (int j = 0; j < barLength; ++j) {
				sb.append(ch);
			}
		}
	}

	public void display(Consumer<String> printer, int barLength) {
		final var sb = new StringBuilder();
		sb.setLength(0);
		sb.append("+");
		makeBar(sb, this.inputLabel, '=', barLength);
		sb.append("+");
		printer.accept(sb.toString());
		for (int i = 0; i < this.bins.length; ++i) {
			final var binPercent = this.bins[i] / (double) this.total;
			final var binLo = Mth.lerp(i / (double) this.bins.length, this.domain.lower, this.domain.higher);
			final var binHi = Mth.lerp((i + 1) / (double) this.bins.length, this.domain.lower, this.domain.higher);
			sb.setLength(0);
			makeBar(sb, binPercent, barLength);
			printer.accept(
					String.format("|%s| %d : %f%% : %f-%f", sb, this.bins[i], 100 * binPercent, binLo, binHi));
		}
		sb.setLength(0);
		sb.append("+");
		makeBar(sb, this.inputLabel, '-', barLength);
		sb.append("+");
		printer.accept(sb.toString());
		printer.accept(String.format("Outliers (Lo): %d, %s", this.outliersLoCount, this.outliersLo));
		printer.accept(String.format("Outliers (Hi): %d, %s", this.outliersHiCount, this.outliersHi));
		printer.accept(String.format("Total: %d", this.total));
		sb.setLength(0);
		makeBar(sb, null, '=', barLength + 2);
		printer.accept(sb.toString());
	}
}