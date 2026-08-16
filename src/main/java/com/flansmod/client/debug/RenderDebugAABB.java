package com.flansmod.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import com.flansmod.common.FlansMod;

public class RenderDebugAABB extends EntityRenderer<EntityDebugAABB, RenderDebugAABB.State>
{
	public static class State extends EntityRenderState
	{
		public float red = 1F, green = 1F, blue = 1F;
		public float yaw, pitch, roll;
		public float minX, minY, minZ, maxX, maxY, maxZ;
	}

	public RenderDebugAABB(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(EntityDebugAABB entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		EntityDebugAABB ent = entity;
		state.red = ent.red;
		state.green = ent.green;
		state.blue = ent.blue;
		state.yaw = ent.getYRot();
		state.pitch = ent.getXRot();
		state.roll = ent.rotationRoll;
		state.minX = ent.offset.x;
		state.minY = ent.offset.y;
		state.minZ = ent.offset.z;
		state.maxX = ent.offset.x + ent.vector.x;
		state.maxY = ent.offset.y + ent.vector.y;
		state.maxZ = ent.offset.z + ent.vector.z;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		if(!FlansMod.DEBUG)
			return;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
		pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
		pose.mulPose(Axis.ZP.rotationDegrees(state.roll));
		collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
		{
			renderBox(p, consumer, state.minX, state.minY, state.minZ, state.maxX, state.maxY, state.maxZ, state.red, state.green, state.blue);
		});
		pose.popPose();
	}

	private void renderBox(PoseStack.Pose p, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue)
	{
		line(p, consumer, minX, minY, minZ, maxX, minY, minZ, red, green, blue);
		line(p, consumer, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue);
		line(p, consumer, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue);
		line(p, consumer, minX, minY, maxZ, minX, minY, minZ, red, green, blue);
		line(p, consumer, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue);
		line(p, consumer, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue);
		line(p, consumer, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue);
		line(p, consumer, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue);
		line(p, consumer, minX, minY, minZ, minX, maxY, minZ, red, green, blue);
		line(p, consumer, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue);
		line(p, consumer, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue);
		line(p, consumer, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue);
	}

	private void line(PoseStack.Pose p, VertexConsumer consumer, float x0, float y0, float z0, float x1, float y1, float z1, float red, float green, float blue)
	{
		consumer.addVertex(p, x0, y0, z0).setColor(red, green, blue, 0.5F);
		consumer.addVertex(p, x1, y1, z1).setColor(red, green, blue, 0.5F);
	}
}
