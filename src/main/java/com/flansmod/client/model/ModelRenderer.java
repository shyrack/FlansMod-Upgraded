package com.flansmod.client.model;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/**
 * Base class for all Flan's Mod renderer parts. Keeps the classic
 * rotation-point/rotation-angle API used by content pack models and renders
 * through a thread-local {@link RenderContext} that the Flan's Mod renderers
 * set up before calling {@link #render(float)}.
 */
public class ModelRenderer
{
	public float rotationPointX;
	public float rotationPointY;
	public float rotationPointZ;
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;
	public boolean mirror;
	public boolean showModel = true;
	public String boxName;
	public List<ModelRenderer> childModels = new ArrayList<>();

	public static class RenderContext
	{
		public PoseStack poseStack;
		public VertexConsumer consumer;
		public int light;
		public int overlay;
	}

	private static final ThreadLocal<RenderContext> CONTEXT = new ThreadLocal<>();

	public static void beginRender(PoseStack poseStack, VertexConsumer consumer, int light, int overlay)
	{
		RenderContext context = new RenderContext();
		context.poseStack = poseStack;
		context.consumer = consumer;
		context.light = light;
		context.overlay = overlay;
		CONTEXT.set(context);
	}

	public static void endRender()
	{
		CONTEXT.remove();
	}

	public static RenderContext getRenderContext()
	{
		return CONTEXT.get();
	}

	public ModelRenderer()
	{
	}

	public void setRotationPoint(float x, float y, float z)
	{
		rotationPointX = x;
		rotationPointY = y;
		rotationPointZ = z;
	}

	public void render(float scale)
	{
		RenderContext context = CONTEXT.get();
		if(context == null || context.poseStack == null || context.consumer == null)
			return;
		render(context.poseStack, context.consumer, context.light, context.overlay, scale);
	}

	public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float scale)
	{
		if(!showModel)
			return;
		poseStack.pushPose();
		poseStack.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
		if(rotateAngleY != 0.0F)
			poseStack.mulPose(Axis.YP.rotationDegrees(rotateAngleY * 57.29578F));
		if(rotateAngleZ != 0.0F)
			poseStack.mulPose(Axis.ZP.rotationDegrees(rotateAngleZ * 57.29578F));
		if(rotateAngleX != 0.0F)
			poseStack.mulPose(Axis.XP.rotationDegrees(rotateAngleX * 57.29578F));
		renderParts(poseStack, consumer, light, overlay, scale);
		for(ModelRenderer childModel : childModels)
		{
			childModel.render(poseStack, consumer, light, overlay, scale);
		}
		poseStack.popPose();
	}

	protected void renderParts(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float scale)
	{
	}

	public void renderWithRotation(float f)
	{
		RenderContext context = CONTEXT.get();
		if(context == null || context.poseStack == null || context.consumer == null)
			return;
		if(!showModel)
			return;
		context.poseStack.pushPose();
		context.poseStack.translate(rotationPointX * f, rotationPointY * f, rotationPointZ * f);
		if(rotateAngleY != 0.0F)
			context.poseStack.mulPose(Axis.YP.rotationDegrees(rotateAngleY * 57.29578F));
		if(rotateAngleX != 0.0F)
			context.poseStack.mulPose(Axis.XP.rotationDegrees(rotateAngleX * 57.29578F));
		if(rotateAngleZ != 0.0F)
			context.poseStack.mulPose(Axis.ZP.rotationDegrees(rotateAngleZ * 57.29578F));
		renderParts(context.poseStack, context.consumer, context.light, context.overlay, f);
		context.poseStack.popPose();
	}

	public void postRender(float f)
	{
		RenderContext context = CONTEXT.get();
		if(context == null || context.poseStack == null)
			return;
		if(!showModel)
			return;
		context.poseStack.translate(rotationPointX * f, rotationPointY * f, rotationPointZ * f);
		if(rotateAngleZ != 0.0F)
			context.poseStack.mulPose(Axis.ZP.rotationDegrees(rotateAngleZ * 57.29578F));
		if(rotateAngleY != 0.0F)
			context.poseStack.mulPose(Axis.YP.rotationDegrees(rotateAngleY * 57.29578F));
		if(rotateAngleX != 0.0F)
			context.poseStack.mulPose(Axis.XP.rotationDegrees(rotateAngleX * 57.29578F));
	}

	public Quaternionf getRotation()
	{
		return Axis.XP.rotationDegrees(rotateAngleX * 57.29578F)
				.mul(Axis.YP.rotationDegrees(rotateAngleY * 57.29578F))
				.mul(Axis.ZP.rotationDegrees(rotateAngleZ * 57.29578F));
	}
}
