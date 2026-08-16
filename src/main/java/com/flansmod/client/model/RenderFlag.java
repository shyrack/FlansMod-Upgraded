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

import com.flansmod.common.teams.EntityFlag;
import com.flansmod.common.teams.EntityFlagpole;

public class RenderFlag extends EntityRenderer<EntityFlag, RenderFlag.State>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "teamsmod/flagpole.png");

	public static class State extends EntityRenderState
	{
		public EntityFlag flag;
		public float yaw;
		public boolean ridingFlagpole;
	}

	public ModelFlagpole modelFlagpole;
	public static float angle;
	private final PoseStack poseStack = new PoseStack();

	public RenderFlag(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityFlag flag, State state, float partialTick)
	{
		super.extractRenderState(flag, state, partialTick);
		state.flag = flag;
		state.yaw = flag.getYRot();
		state.ridingFlagpole = flag.getVehicle() instanceof EntityFlagpole;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(state.yaw));

		if(!state.ridingFlagpole)
		{
			pose.mulPose(Axis.YP.rotationDegrees(angle));
			pose.translate(0.5F, 0F, 0F);
		}
		else
		{
			pose.translate(0F, 0.5F, 0F);
		}

		pose.scale(-1F, -1F, 1F);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			modelFlagpole.renderFlag(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, state.flag);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}
}
