package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.entity.Entity;

/**
 * Base class for Flan's Mod entity models. Content pack models implement the
 * classic {@link #render(Entity, float, float, float, float, float, float)}
 * signature; the Flan's Mod renderers set up a {@link ModelRenderer.RenderContext}
 * before invoking it.
 */
public class ModelBase
{
	public float textureWidth = 64.0F;
	public float textureHeight = 32.0F;

	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlay)
	{
		ModelRenderer.beginRender(poseStack, consumer, light, overlay);
		try
		{
			render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
		}
		finally
		{
			ModelRenderer.endRender();
		}
	}

	public void setRotationAngles(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
	}
}
