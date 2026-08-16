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
import com.flansmod.common.tools.EntityParachute;

public class RenderParachute extends EntityRenderer<EntityParachute, RenderParachute.State>
{
	public static class State extends EntityRenderState
	{
		public EntityParachute entity;
		public ModelBase model;
		public Identifier texture;
		public float yaw;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderParachute(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityParachute entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.entity = entity;
		state.model = entity.type.model;
		state.texture = FlansModResourceHandler.getTexture(entity.type);
		state.yaw = entity.getYRot();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ModelBase model = state.model;
		if(model == null)
			return;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.render(state.entity, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}
}
