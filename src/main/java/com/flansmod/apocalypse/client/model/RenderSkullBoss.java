package com.flansmod.apocalypse.client.model;

import com.flansmod.apocalypse.common.entity.EntitySkullBoss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import com.flansmod.client.model.ModelRenderer;

public class RenderSkullBoss extends EntityRenderer<EntitySkullBoss, RenderSkullBoss.State>
{
	public static class State extends EntityRenderState
	{
		public float yaw;
		public float pitch;
		public float spawnSpin;
		public float laughFactor;
		public float partialTick;
	}
	
	private final ModelSkullBoss model = new ModelSkullBoss();
	private final PoseStack poseStack = new PoseStack();
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmodapocalypse", "textures/entity/skullboss.png");
	
	public RenderSkullBoss(EntityRendererProvider.Context context)
	{
		super(context);
	}
	
	@Override
	public State createRenderState()
	{
		return new State();
	}
	
	@Override
	public void extractRenderState(EntitySkullBoss entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.yaw = entity.getYRot();
		state.pitch = entity.getXRot();
		state.spawnSpin = entity.GetSpawnSpin(partialTick);
		state.laughFactor = entity.GetLaughFactor(partialTick);
		state.partialTick = partialTick;
	}
	
	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		pose.pushPose();
		
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw + state.spawnSpin));
		pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));
		pose.scale(32f, 32f, 32f);
		
		float laughFactor = state.laughFactor;
		
		pose.pushPose();
		pose.mulPose(Axis.ZP.rotationDegrees(laughFactor * 15.0f));
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderHead(1F / 16F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
		
		pose.pushPose();
		pose.mulPose(Axis.ZP.rotationDegrees(-laughFactor * 15.0f));
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderJaw(1F / 16F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
		
		pose.popPose();
	}
}
