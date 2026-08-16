package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.ItemHolderType;
import com.flansmod.common.TileEntityItemHolder;
import com.flansmod.common.guns.ItemGun;

public class RenderItemHolder implements BlockEntityRenderer<TileEntityItemHolder, RenderItemHolder.State>
{
	public static class State extends BlockEntityRenderState
	{
		public ModelItemHolder model;
		public Identifier texture;
		public ItemStack stack = ItemStack.EMPTY;
		public Direction facing = Direction.NORTH;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderItemHolder()
	{
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(TileEntityItemHolder te, State state, float partialTick, net.minecraft.world.phys.Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay)
	{
		TileEntityItemHolder holder = te;
		if(holder == null || holder.type == null)
			return;
		state.model = holder.type.model;
		state.texture = getTexture(holder.type);
		state.stack = holder.getItem(0);
		state.facing = holder.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ModelItemHolder model = state.model;

		if(model != null)
		{
			pose.pushPose();
			pose.mulPose(Axis.ZP.rotationDegrees(180F));

			switch(state.facing)
			{
				case NORTH:
					pose.translate(-1F, 0F, 0F);
					break;
				case EAST:
					pose.translate(-1F, 0F, 1F);
					pose.mulPose(Axis.YP.rotationDegrees(90F));
					break;
				case SOUTH:
					pose.translate(0F, 0F, 1F);
					pose.mulPose(Axis.YP.rotationDegrees(180F));
					break;
				case WEST:
					pose.mulPose(Axis.YP.rotationDegrees(270F));
					break;
				default:
					break;
			}

			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.render();
				ModelRenderer.endRender();
				poseStack.popPose();
			});

			ItemStack stack = state.stack;
			if(stack != null && !stack.isEmpty())
			{
				pose.pushPose();
				pose.mulPose(Axis.ZP.rotationDegrees(180F));
				pose.translate(-0.5F, 0.5F, 0.5F);

				pose.translate(model.itemOffset.x, model.itemOffset.y, model.itemOffset.z);
				pose.mulPose(Axis.XP.rotationDegrees(model.itemRotation.x));
				pose.mulPose(Axis.ZP.rotationDegrees(model.itemRotation.z));
				pose.mulPose(Axis.YP.rotationDegrees(model.itemRotation.y));

				if(stack.getItem() instanceof ItemGun && ((ItemGun)stack.getItem()).GetType() != null && ((ItemGun)stack.getItem()).GetType().model != null)
				{
					RenderGun.renderGunItem(collector, pose, stack, state.lightCoords, 0F, 0F);
				}
				// TODO: [26.1.2] non-gun items in holders are rendered through the modern item model system, to be redone
				pose.popPose();
			}

			pose.popPose();
		}
	}

	protected Identifier getTexture(ItemHolderType type)
	{
		return FlansModResourceHandler.getTexture(type);
	}
}
