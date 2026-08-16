package com.flansmod.apocalypse.client.model;

import com.flansmod.apocalypse.common.entity.EntitySurvivor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class RenderSurvivor extends HumanoidMobRenderer<EntitySurvivor, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>
{
	private static final Identifier SURVIVOR_SKIN = Identifier.fromNamespaceAndPath("flansmodapocalypse", "textures/entity/survivor.png");
	
	public RenderSurvivor(EntityRendererProvider.Context context)
	{
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		// TODO APOCALYPSE: 1.12.2 added a HumanoidArmorLayer; armour rendering not ported
	}
	
	@Override
	public HumanoidRenderState createRenderState()
	{
		return new HumanoidRenderState();
	}
	
	@Override
	public Identifier getTextureLocation(HumanoidRenderState state)
	{
		return SURVIVOR_SKIN;
	}
}
