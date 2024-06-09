package net.xavil.ultraviolet.mixin.impl.render;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.ultraviolet.mixin.accessor.VertexFormatAccessor;

@Mixin(VertexFormat.class)
public abstract class VertexFormatMixin implements VertexFormatAccessor {

    @Shadow
    @Final
    private com.google.common.collect.ImmutableList<VertexFormatElement> elements;
    @Shadow
    @Final
    private com.google.common.collect.ImmutableMap<String, VertexFormatElement> elementMapping;

    @Override
    public ImmutableMap<String, VertexFormatElement> ultraviolet_getElementMapping() {
        return ImmutableMap.proxy(this.elementMapping);
    }

    @Override
    public ImmutableList<VertexFormatElement> ultraviolet_getElements() {
        return ImmutableList.proxy(this.elements);
    }

}
