package com.flansmod.apocalypse.client.model;

import com.flansmod.apocalypse.common.entity.EntityNukeDrop;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import com.flansmod.client.model.ModelRenderer;

public class RenderNukeDrop extends EntityRenderer<EntityNukeDrop, RenderNukeDrop.State>
{
	public static class State extends EntityRenderState
	{
		public boolean onGround;
		public int timeSinceExplosion;
		public float partialTick;
	}
	
	private final ModelNukeDrop model = new ModelNukeDrop();
	private final PoseStack poseStack = new PoseStack();
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmodapocalypse", "textures/entity/nukedrop.png");
	
	public RenderNukeDrop(EntityRendererProvider.Context context)
	{
		super(context);
	}
	
	@Override
	public State createRenderState()
	{
		return new State();
	}
	
	@Override
	public void extractRenderState(EntityNukeDrop entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.onGround = entity.onGround();
		state.timeSinceExplosion = entity.timeSinceExplosion;
		state.partialTick = partialTick;
	}
	
	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		// TODO APOCALYPSE: 1.12.2 rendered the explosion ball with alpha blending; alpha is not supported here
		pose.pushPose();
		if(state.onGround)
		{
			//Exploded
			float scale = 1F - 1F / ((float)state.timeSinceExplosion / 5F + 1);
			scale *= 100F * scale;
			pose.scale(-scale, scale, scale);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.renderBall(0.0625F);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
		}
		else
		{
			//Falling
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.renderNuke(0.0625F);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
		}
		pose.popPose();
	}
}
