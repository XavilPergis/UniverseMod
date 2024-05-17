package net.xavil.ultraviolet.client.screen.layer;

import java.util.function.Consumer;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.impl.VectorFloat;
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.collections.interfaces.ImmutableListFloat;
import net.xavil.hawklib.collections.iterator.IteratorInt;

public final class Histogram {
	private final String inputLabel;
	private final VectorInt bins;

	public final AxisMapping mapping;
	
	private final VectorFloat outliersLo = new VectorFloat(), outliersHi = new VectorFloat();
	private int outliersLoCount, outliersHiCount;

	private int total = 0;

	public Histogram(String inputLabel, int binCount, AxisMapping mapping) {
		this.inputLabel = inputLabel;
		this.bins = new VectorInt(binCount);
		this.mapping = mapping;
		reset(binCount);
	}

	public ImmutableListFloat outliersLo() {
		return this.outliersLo;
	}

	public ImmutableListFloat outliersHi() {
		return this.outliersHi;
	}

	public int size() {
		return this.bins.size();
	}

	public int get(int bin) {
		return this.bins.get(bin);
	}

	public void reset(int binCount) {
		this.total = 0;
		this.bins.clear();
		this.bins.extend(IteratorInt.repeat(0, binCount));
		this.outliersLo.clear();
		this.outliersHi.clear();
		this.outliersLoCount = this.outliersHiCount = 0;
	}

	public void reset() {
		reset(this.bins.size());
	}

	public int total() {
		return this.total;
	}

	public void setBins(IteratorInt binIterator) {
		this.outliersLo.clear();
		this.outliersHi.clear();
		this.outliersLoCount = this.outliersHiCount = 0;
		
		final var binCount = this.bins.size();
		this.bins.clear();
		this.bins.extend(binIterator.limit(binCount));
		this.bins.extend(IteratorInt.repeat(0, binCount - this.bins.size()));

		this.total = 0;
		this.bins.forEach(count -> this.total += count);
	}

	public void insert(double value) {
		this.total += 1;
		final var t = this.mapping.remap(value);
		if (t < 0) {
			if (this.outliersLoCount < 20)
				this.outliersLo.push((float) value);
			this.outliersLoCount += 1;
		} else if (t >= 1) {
			if (this.outliersHiCount < 20)
				this.outliersHi.push((float) value);
			this.outliersHiCount += 1;
		} else {
			final var bin = Mth.floor(t * this.bins.size());
			this.bins.set(bin, this.bins.get(bin) + 1);
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
		for (int i = 0; i < this.bins.size(); ++i) {
			final var binPercent = this.bins.get(i) / (double) this.total;
			final var binLo = this.mapping.unmap(i / (double) this.bins.size());
			final var binHi = this.mapping.unmap((i + 1) / (double) this.bins.size());
			sb.setLength(0);
			makeBar(sb, binPercent, barLength);
			printer.accept(
					String.format("|%s| %d : %f%% : %f-%f", sb, this.bins.get(i), 100 * binPercent, binLo, binHi));
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