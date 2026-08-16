package com.flansmod.client.model;

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

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.guns.EntityAAGun;

public class RenderAAGun extends EntityRenderer<EntityAAGun, RenderAAGun.State>
{
	public static class State extends EntityRenderState
	{
		public EntityAAGun aa;
		public ModelAAGun model;
		public Identifier texture;
		public float gunYaw;
		public float prevGunYaw;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderAAGun(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityAAGun aa, State state, float partialTick)
	{
		super.extractRenderState(aa, state, partialTick);
		state.aa = aa;
		state.model = aa.type.model;
		state.texture = FlansModResourceHandler.getTexture(aa.type);
		state.gunYaw = aa.gunYaw;
		state.prevGunYaw = aa.prevGunYaw;
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ModelAAGun modelAAGun = state.model;
		if(modelAAGun == null)
			return;

		float dYaw = state.gunYaw - state.prevGunYaw;
		for(; dYaw > 180F; dYaw -= 360F)
		{
		}
		for(; dYaw <= -180F; dYaw += 360F)
		{
		}
		final float gunYaw = state.prevGunYaw + dYaw * state.partialTick;

		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			modelAAGun.renderBase(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, state.aa);
			poseStack.mulPose(Axis.YP.rotationDegrees(180F - gunYaw));
			modelAAGun.renderGun(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, state.aa);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
	}
}
