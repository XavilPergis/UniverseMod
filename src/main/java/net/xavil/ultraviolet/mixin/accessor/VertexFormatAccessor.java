package net.xavil.ultraviolet.mixin.accessor;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;

public interface VertexFormatAccessor {

    ImmutableList<VertexFormatElement> ultraviolet_getElements();

    ImmutableMap<String, VertexFormatElement> ultraviolet_getElementMapping();

    static ImmutableList<VertexFormatElement> getElements(VertexFormat format) {
        return ((VertexFormatAccessor) format).ultraviolet_getElements();
    }

    static ImmutableMap<String, VertexFormatElement> getElementMapping(VertexFormat format) {
        return ((VertexFormatAccessor) format).ultraviolet_getElementMapping();
    }

}
