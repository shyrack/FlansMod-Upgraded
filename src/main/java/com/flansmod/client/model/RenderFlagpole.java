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

import com.flansmod.common.teams.EntityFlagpole;

public class RenderFlagpole extends EntityRenderer<EntityFlagpole, RenderFlagpole.State>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "teamsmod/flagpole.png");

	public static class State extends EntityRenderState
	{
		public EntityFlagpole flagpole;
		public float yaw;
	}

	public ModelFlagpole modelFlagpole;
	private final PoseStack poseStack = new PoseStack();

	public RenderFlagpole(EntityRendererProvider.Context context)
	{
		super(context);
		modelFlagpole = new ModelFlagpole();
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(EntityFlagpole flagpole, State state, float partialTick)
	{
		super.extractRenderState(flagpole, state, partialTick);
		state.flagpole = flagpole;
		state.yaw = flagpole.getYRot();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
		pose.scale(-1F, -1F, 1F);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			modelFlagpole.renderPole(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, state.flagpole);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}
}
