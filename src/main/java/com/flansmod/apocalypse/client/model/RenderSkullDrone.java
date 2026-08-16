package com.flansmod.apocalypse.client.model;

import com.flansmod.apocalypse.common.entity.EntitySkullDrone;

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

public class RenderSkullDrone extends EntityRenderer<EntitySkullDrone, RenderSkullDrone.State>
{
	public static class State extends EntityRenderState
	{
		public float yaw;
		public float tickCount;
		public float partialTick;
	}
	
	private final ModelSkullDrone model = new ModelSkullDrone();
	private final PoseStack poseStack = new PoseStack();
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmodapocalypse", "textures/entity/skulldrone.png");
	
	public RenderSkullDrone(EntityRendererProvider.Context context)
	{
		super(context);
	}
	
	@Override
	public State createRenderState()
	{
		return new State();
	}
	
	@Override
	public void extractRenderState(EntitySkullDrone entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.yaw = entity.getYRot();
		state.tickCount = entity.tickCount;
		state.partialTick = partialTick;
	}
	
	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
		
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderBase(1F / 16F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		
		for(int i = 0; i < model.numPropellers; i++)
		{
			pose.pushPose();
			pose.translate(model.propellerOrigins[i].x, model.propellerOrigins[i].y, model.propellerOrigins[i].z);
			pose.mulPose(Axis.YP.rotationDegrees((state.tickCount + state.partialTick) * (i % 2 == 0 ? -80f : 80f)));
			pose.scale(2f, 2f, 2f);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.renderPropeller(1f / 16f);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
			pose.popPose();
		}
		
		// TODO APOCALYPSE: 1.12.2 rendered the held gun model via ClientProxy.gunRenderer;
		// gun model rendering not ported
		
		pose.popPose();
	}
}
