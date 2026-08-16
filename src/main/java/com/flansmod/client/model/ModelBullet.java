package com.flansmod.client.model;

import com.flansmod.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;
import net.minecraft.world.entity.Entity;

public class ModelBullet extends ModelBase
{
	public ModelRendererTurbo bulletModel;
	
	public ModelBullet()
	{
		bulletModel = new ModelRendererTurbo(this, 0, 0);
		bulletModel.addBox(-0.5F, -1.5F, -0.5F, 1, 3, 1);
	}
	
	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		bulletModel.render(f5);
	}
}
