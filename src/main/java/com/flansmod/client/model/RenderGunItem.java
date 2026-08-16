package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;

import com.flansmod.common.EntityItemCustomRender;
import com.flansmod.common.guns.ItemGun;

public class RenderGunItem extends EntityRenderer<EntityItemCustomRender, RenderGunItem.State>
{
	public static class State extends EntityRenderState
	{
		public ItemStack stack = ItemStack.EMPTY;
		public float yaw;
		public float partialTick;
	}

	public RenderGunItem(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(EntityItemCustomRender entity, State state, float partialTick)
	{
		super.extractRenderState(entity, state, partialTick);
		state.stack = entity.getItem();
		state.yaw = entity.tickCount;
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ItemStack stack = state.stack;

		if(stack.getItem() instanceof ItemGun && ((ItemGun)stack.getItem()).GetType() != null && ((ItemGun)stack.getItem()).GetType().model != null)
		{
			RenderGun.renderGunItem(collector, pose, stack, state.lightCoords, state.yaw, state.partialTick);
		}
	}
}
