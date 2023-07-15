package net.xavil.hawklib.client.gl;

import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.hawklib.collections.interfaces.MutableMap;

public final class GlFragmentWrites {

	public static final String COLOR = "fColor";
	public static final String POSITION = "fPosition";
	public static final String NORMAL = "fNormal";

	public static final GlFragmentWrites COLOR_ONLY = new GlFragmentWrites(new ResourceLocation("hawk", "color"), COLOR);
	public static final GlFragmentWrites VANILLA = new GlFragmentWrites(new ResourceLocation("default"), "fragColor");

	public final ResourceLocation name;
	private final String[] outputNames;
	private final ImmutableMap<String, Integer> fragmentWriteIndices;

	private GlFragmentWrites(ResourceLocation name, String... outputNames) {
		this.name = name;
		this.outputNames = outputNames;
		final var indices = MutableMap.<String, Integer>hashMap();
		for (int i = 0; i < outputNames.length; ++i) {
			if (indices.insert(outputNames[i], i).isSome()) {
				throw new IllegalArgumentException(String.format(
						"For fragment write set '%s', fragment write '%s' was specified more than once!",
						name, outputNames[i]));
			}
		}
		this.fragmentWriteIndices = indices;
	}

	public int getFragmentWriteId(String fragmentWriteName) {
		return this.fragmentWriteIndices.get(fragmentWriteName).unwrapOr(-1);
	}

	public String getFragmentWriteName(int fragmentWriteId) {
		if (fragmentWriteId >= this.outputNames.length)
			return null;
		return this.outputNames[fragmentWriteId];
	}

	public int getFragmentWriteCount() {
		return this.outputNames.length;
	}

}
