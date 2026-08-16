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
import com.flansmod.common.guns.EntityBullet;

public class RenderBullet extends EntityRenderer<EntityBullet, RenderBullet.State>
{
	public static class State extends EntityRenderState
	{
		public EntityBullet bullet;
		public ModelBase model;
		public Identifier texture;
		public float yaw;
		public float pitch;
		public float prevPitch;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderBullet(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityBullet bullet, State state, float partialTick)
	{
		super.extractRenderState(bullet, state, partialTick);
		state.bullet = bullet;
		state.model = bullet.getFiredShot().getBulletType().model;
		state.texture = FlansModResourceHandler.getTexture(bullet.getFiredShot().getBulletType());
		state.yaw = bullet.getYRot();
		state.pitch = bullet.getXRot();
		state.prevPitch = bullet.xRotO;
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ModelBase model = state.model;
		if(model == null)
			return;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
		pose.mulPose(Axis.XP.rotationDegrees(90F - (state.prevPitch + (state.pitch - state.prevPitch) * state.partialTick)));
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.render(state.bullet, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}
}
