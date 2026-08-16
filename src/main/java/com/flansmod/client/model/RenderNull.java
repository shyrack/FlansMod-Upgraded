package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

import com.flansmod.common.FlansMod;

public class RenderNull<E extends Entity> extends EntityRenderer<E, RenderNull.State>
{
	public static class State extends EntityRenderState
	{
	}

	public RenderNull(EntityRendererProvider.Context context)
	{
		super(context);
		shadowRadius = 0.5F;
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		if(FlansMod.DEBUG)
		{
			collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
			{
				consumer.addVertex(p, -1F, -1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, -1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, -1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, -1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, -1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, -1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, -1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, -1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, 1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, 1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, 1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, 1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, 1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, 1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, 1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, 1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, -1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, 1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, -1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, 1F, -1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, -1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, 1F, 1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, -1F, 1F).setColor(0F, 0F, 1F, 0.3F);
				consumer.addVertex(p, -1F, 1F, 1F).setColor(0F, 0F, 1F, 0.3F);
			});
		}
	}
}
