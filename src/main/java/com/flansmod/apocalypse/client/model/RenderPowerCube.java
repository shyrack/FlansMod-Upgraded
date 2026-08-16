package com.flansmod.apocalypse.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import com.flansmod.apocalypse.common.blocks.TileEntityPowerCube;
import com.flansmod.client.model.ModelRenderer;

public class RenderPowerCube implements BlockEntityRenderer<TileEntityPowerCube, RenderPowerCube.State>
{
	public static class State extends BlockEntityRenderState
	{
		public float age;
		public float partialTick;
	}
	
	private final Identifier TEXTURE = Identifier.fromNamespaceAndPath("flansmodapocalypse", "textures/blocks/powercube.png");
	private final ModelPowerCube model = new ModelPowerCube();
	private final PoseStack poseStack = new PoseStack();
	
	public RenderPowerCube(BlockEntityRendererProvider.Context context)
	{
	}
	
	@Override
	public State createRenderState()
	{
		return new State();
	}
	
	@Override
	public void extractRenderState(TileEntityPowerCube holder, State state, float partialTick, net.minecraft.world.phys.Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay)
	{
		state.age = holder.age;
		state.partialTick = partialTick;
	}
	
	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.mulPose(Axis.XP.rotationDegrees(180F));
		
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.render();
			ModelRenderer.endRender();
			poseStack.popPose();
		});

		float angle = (state.age + state.partialTick) * 10F;
		float scale = (float)Math.sin(angle * 0.01F);

		pose.pushPose();
		pose.mulPose(Axis.XP.rotationDegrees(angle * 1.345F));
		pose.mulPose(Axis.YP.rotationDegrees(angle * 0.8925F));
		pose.mulPose(Axis.ZP.rotationDegrees(angle * 0.245F));
		pose.scale(scale, scale, scale);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderPower();
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();

		scale = (float)Math.cos(angle * 0.0134F);

		pose.pushPose();
		pose.mulPose(Axis.XP.rotationDegrees(angle * 1.783F));
		pose.mulPose(Axis.YP.rotationDegrees(angle * 1.145F));
		pose.mulPose(Axis.ZP.rotationDegrees(angle * 0.3567F));
		pose.scale(scale, scale, scale);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderPower();
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();

		scale = (float)Math.sin(angle * 0.0254F);

		pose.pushPose();
		pose.mulPose(Axis.XP.rotationDegrees(angle * 1.9993F));
		pose.mulPose(Axis.YP.rotationDegrees(angle * 1.111F));
		pose.mulPose(Axis.ZP.rotationDegrees(angle * 0.578F));
		pose.scale(scale, scale, scale);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.renderPower();
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();

		pose.popPose();
	}
}
