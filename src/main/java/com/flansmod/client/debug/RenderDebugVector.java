package com.flansmod.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import com.flansmod.common.FlansMod;

public class RenderDebugVector extends EntityRenderer<EntityDebugVector, RenderDebugVector.State>
{
	public static class State extends EntityRenderState
	{
		public float red = 1F, green = 1F, blue = 1F;
		public float pointingX, pointingY, pointingZ;
	}

	public RenderDebugVector(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(EntityDebugVector entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.red = entity.getColorRed();
		state.green = entity.getColorGreen();
		state.blue = entity.getColorBlue();
		state.pointingX = entity.getPointingX();
		state.pointingY = entity.getPointingY();
		state.pointingZ = entity.getPointingZ();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		if(!FlansMod.DEBUG)
			return;

		collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
		{
			consumer.addVertex(p, 0F, 0F, 0F).setColor(state.red, state.green, state.blue, 1F);
			consumer.addVertex(p, state.pointingX, state.pointingY, state.pointingZ).setColor(state.red, state.green, state.blue, 1F);
		});
	}
}
