package com.flansmod.apocalypse.client.model;

import com.flansmod.apocalypse.common.entity.EntityTeleporter;

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

import com.flansmod.client.model.ModelBase;
import com.flansmod.client.model.ModelRenderer;

public class RenderTeleporter extends EntityRenderer<EntityTeleporter, RenderTeleporter.State>
{
	public static class State extends EntityRenderState
	{
		public int tickCount;
		public float partialTick;
	}
	
	private final ModelTeleporter model = new ModelTeleporter();
	private final PoseStack poseStack = new PoseStack();
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmodapocalypse", "textures/entity/teleporter.png");
	private static final int[] randomiser = new int[]{145, 167, 324, 541};
	
	public RenderTeleporter(EntityRendererProvider.Context context)
	{
		super(context);
	}
	
	@Override
	public State createRenderState()
	{
		return new State();
	}
	
	@Override
	public void extractRenderState(EntityTeleporter entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.tickCount = entity.tickCount;
		state.partialTick = partialTick;
	}
	
	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		pose.pushPose();
		pose.translate(0.0F, 1.0F, 0.0F);
		
		for(int i = 0; i < 4; i++)
		{
			float scaleX = 0.4F * (float)Math.sin((state.tickCount + state.partialTick) * 0.1F + 1.7F * randomiser[i]) + 1.0F;
			float scaleY = 0.4F * (float)Math.cos((state.tickCount + state.partialTick) * 0.114F + 1.145F + 0.35F * randomiser[(i + 1) % 4]) + 1.0F;
			float scaleZ = 0.4F * (float)Math.cos((state.tickCount + state.partialTick) * 0.121F + 0.7545F - 11F * randomiser[i]) + 1.0F;
			float rotation = 100F * (float)Math.cos((state.tickCount + state.partialTick) * 0.000121F * randomiser[(i + 2) % 4]);
			pose.pushPose();
			pose.mulPose(Axis.XP.rotationDegrees(rotation));
			pose.mulPose(Axis.YP.rotationDegrees(100F * (float)Math.sin((state.tickCount + state.partialTick) * 0.000173F * randomiser[(i + 2) % 4])));
			pose.scale(-scaleX, scaleY, scaleZ);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.render(0.0625F);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
			pose.popPose();
		}
		
		pose.popPose();
	}
}
