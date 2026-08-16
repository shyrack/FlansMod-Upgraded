package com.flansmod.client.model;

import com.flansmod.client.model.ModelBase;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.driveables.mechas.EntityMecha;

public class ModelMechaTool extends ModelBase
{
	/**
	 * This is the base, common across all Mecha Tools
	 */
	public ModelRendererTurbo[] baseModel = new ModelRendererTurbo[0];
	/**
	 * This bit spins
	 */
	public ModelRendererTurbo[] drillModel = new ModelRendererTurbo[0];
	/**
	 * This bit spins on a different axis
	 */
	public ModelRendererTurbo[] sawModel = new ModelRendererTurbo[0];
	
	public void render(EntityMecha mecha, float f1)
	{
		float f5 = 1F / 16F;
		
		for(ModelRendererTurbo model : baseModel)
			model.render(f5);
	}
	
	public void renderDrill(EntityMecha mecha, float f1)
	{
		float f5 = 1F / 16F;
		
		for(ModelRendererTurbo model : drillModel)
			model.render(f5);
	}
	
	public void renderSaw(EntityMecha mecha, float f1, boolean spin)
	{
		float f5 = 1F / 16F;
		
		for(ModelRendererTurbo model : sawModel)
		{
			if(spin)
			{
				model.rotateAngleY = 25F * (float)mecha.tickCount / 180F * 3.14159265F;
			}
			model.render(f5);
		}
		
	}
}
