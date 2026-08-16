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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.guns.ItemGrenade;

public class RenderGrenade extends EntityRenderer<EntityGrenade, RenderGrenade.State> implements CustomItemRenderer
{
	public static class State extends EntityRenderState
	{
		public EntityGrenade grenade;
		public ModelBase model;
		public Identifier texture;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderGrenade(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityGrenade grenade, State state, float partialTick)
	{
		super.extractRenderState(grenade, state, partialTick);
		state.grenade = grenade;
		state.model = grenade.type.model;
		Identifier texture = FlansModResourceHandler.getTexture(grenade.type);
		if(texture == null)
			texture = FlansModResourceHandler.getIcon(grenade.type);
		state.texture = texture;
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		ModelBase model = state.model;
		if(model == null)
			return;
		EntityGrenade grenade = state.grenade;

		pose.pushPose();
		if(grenade.stuck)
		{
			pose.mulPose(Axis.YP.rotationDegrees(180F - grenade.axes.getYaw()));
			pose.mulPose(Axis.ZP.rotationDegrees(grenade.axes.getPitch()));
			pose.mulPose(Axis.XP.rotationDegrees(grenade.axes.getRoll()));
		}
		else
		{
			float dYaw = (grenade.axes.getYaw() - grenade.yRotO);
			for(; dYaw > 180F; dYaw -= 360F)
			{
			}
			for(; dYaw <= -180F; dYaw += 360F)
			{
			}
			float dPitch = (grenade.axes.getPitch() - grenade.xRotO);
			for(; dPitch > 180F; dPitch -= 360F)
			{
			}
			for(; dPitch <= -180F; dPitch += 360F)
			{
			}
			float dRoll = (grenade.axes.getRoll() - grenade.prevRotationRoll);
			for(; dRoll > 180F; dRoll -= 360F)
			{
			}
			for(; dRoll <= -180F; dRoll += 360F)
			{
			}
			pose.mulPose(Axis.YP.rotationDegrees(180F - grenade.yRotO - dYaw * state.partialTick));
			pose.mulPose(Axis.ZP.rotationDegrees(grenade.xRotO + dPitch * state.partialTick));
			pose.mulPose(Axis.XP.rotationDegrees(grenade.prevRotationRoll + dRoll * state.partialTick));
		}
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.render(grenade, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}

	public boolean handleRenderType(ItemStack item, CustomItemRenderType type)
	{
		switch(type)
		{
			case EQUIPPED:
			case EQUIPPED_FIRST_PERSON: return item != null && item.getItem() instanceof ItemGrenade && ((ItemGrenade)item.getItem()).type.model != null;
			default: break;
		}
		return false;
	}

	@Override
	public void renderItem(CustomItemRenderType type, InteractionHand hand, ItemStack item, Object... data)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}
}
