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
import com.flansmod.common.guns.EntityMG;

public class RenderMG extends EntityRenderer<EntityMG, RenderMG.State>
{
	public static class State extends EntityRenderState
	{
		public EntityMG mg;
		public ModelMG model;
		public Identifier texture;
		public float yaw;
		public float prevYaw;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderMG(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityMG mg, State state, float partialTick)
	{
		super.extractRenderState(mg, state, partialTick);
		state.mg = mg;
		state.model = mg.type.deployableModel;
		state.texture = FlansModResourceHandler.getDeployableTexture(mg.type);
		state.yaw = mg.getYRot();
		state.prevYaw = mg.yRotO;
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ModelMG model = state.model;
		if(model == null)
			return;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180F - state.mg.direction * 90F));
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderBipod(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, state.mg);
			poseStack.mulPose(Axis.YP.rotationDegrees(-(state.prevYaw + (state.yaw - state.prevYaw) * state.partialTick)));
			model.renderGun(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, state.partialTick, state.mg);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}
}
