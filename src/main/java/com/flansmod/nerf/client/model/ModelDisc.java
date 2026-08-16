package com.flansmod.nerf.client.model;

import com.flansmod.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;
import net.minecraft.world.entity.Entity;

public class ModelDisc extends ModelBase
{
	public ModelRendererTurbo bulletModel;

	public ModelDisc()
	{
		bulletModel = new ModelRendererTurbo(this, 0, 0);
		bulletModel.addBox(-1F, -1F, -0.5F, 2, 2, 1);
	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		bulletModel.render(f5);
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}
}
