package com.flansmod.client.model;

import com.flansmod.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;

import com.flansmod.common.teams.EntityFlag;
import com.flansmod.common.teams.EntityFlagpole;

public class ModelFlagpole extends ModelBase
{
	public ModelFlagpole()
	{
		poleModel = new ModelRendererTurbo[3];
		
		poleModel[0] = new ModelRendererTurbo(this, 0, 16);
		poleModel[1] = new ModelRendererTurbo(this, 0, 16);
		poleModel[2] = new ModelRendererTurbo(this, 0, 20);
		
		poleModel[0].addBox(-48F, -1F, -1F, 24, 2, 2, 0.0F);
		poleModel[1].addBox(-24F, -1F, -1F, 24, 2, 2, 0.0F);
		poleModel[2].addBox(-2F, -2F, -2F, 4, 2, 4, 0.0F);
		
		poleModel[0].rotateAngleZ = 1.57079633F;
		poleModel[1].rotateAngleZ = 1.57079633F;
		
		flagModel = new ModelRendererTurbo[1];

		flagModel[0] = new ModelRendererTurbo(this, 0, 0);
		flagModel[0].addBox(-8F, -16F, 0F, 16, 16, 0, 0.0F);
		flagModel[0].setRotationPoint(8F, 0F, 0F);
	}
	
	public void renderPole(float f, float f1, float f2, float f3, float f4, float f5, EntityFlagpole entityflag)
	{
		for(ModelRendererTurbo model : poleModel)
		{
			model.render(f5);
		}
	}
	
	public void renderFlag(float f, float f1, float f2, float f3, float f4, float f5, EntityFlag entityflag)
	{
		for(ModelRendererTurbo model : flagModel)
		{
			model.render(f5);
		}
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public ModelRendererTurbo poleModel[];
	public ModelRendererTurbo flagModel[];
}
