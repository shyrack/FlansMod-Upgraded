package com.flansmod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.teams.ArmourType;

public class ModelCustomArmour extends HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState>
{
	public ArmourType type;
	
	public ModelRendererTurbo[] headModel = new ModelRendererTurbo[0];
	public ModelRendererTurbo[] bodyModel = new ModelRendererTurbo[0];
	public ModelRendererTurbo[] leftArmModel = new ModelRendererTurbo[0];
	public ModelRendererTurbo[] rightArmModel = new ModelRendererTurbo[0];
	public ModelRendererTurbo[] leftLegModel = new ModelRendererTurbo[0];
	public ModelRendererTurbo[] rightLegModel = new ModelRendererTurbo[0];
	public ModelRendererTurbo[] skirtFrontModel = new ModelRendererTurbo[0]; //Acts like a leg piece, but its pitch is set to the maximum of the two legs
	public ModelRendererTurbo[] skirtRearModel = new ModelRendererTurbo[0]; //Acts like a leg piece, but its pitch is set to the minimum of the two legs
	
	public ModelCustomArmour()
	{
		super(HumanoidModel.createMesh(CubeDeformation.NONE, 0F).getRoot().bake(64, 32));
	}
	
	public ModelCustomArmour(ModelPart modelPart)
	{
		super(modelPart);
	}
	
	/**
	 * TODO: [26.1.2] armour rendering is done through the modern armour model system and will be redone later.
	 * Kept so content pack models can still expose their part arrays.
	 */
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		render(headModel, head, f5, type == null ? 1F : type.modelScale);
		render(bodyModel, body, f5, type == null ? 1F : type.modelScale);
		render(leftArmModel, leftArm, f5, type == null ? 1F : type.modelScale);
		render(rightArmModel, rightArm, f5, type == null ? 1F : type.modelScale);
		render(leftLegModel, leftLeg, f5, type == null ? 1F : type.modelScale);
		render(rightLegModel, rightLeg, f5, type == null ? 1F : type.modelScale);
		//Skirt front
		{
			for(ModelRendererTurbo mod : skirtFrontModel)
			{
				mod.rotationPointX = (leftLeg.x + rightLeg.x) / 2F / (type == null ? 1F : type.modelScale);
				mod.rotationPointY = (leftLeg.y + rightLeg.y) / 2F / (type == null ? 1F : type.modelScale);
				mod.rotationPointZ = (leftLeg.z + rightLeg.z) / 2F / (type == null ? 1F : type.modelScale);
				mod.rotateAngleX = Math.min(leftLeg.xRot, rightLeg.xRot);
				mod.rotateAngleY = leftLeg.yRot;
				mod.rotateAngleZ = leftLeg.zRot;
				mod.render(f5);
			}
		}
		//Skirt back
		{
			for(ModelRendererTurbo mod : skirtRearModel)
			{
				mod.rotationPointX = (leftLeg.x + rightLeg.x) / 2F / (type == null ? 1F : type.modelScale);
				mod.rotationPointY = (leftLeg.y + rightLeg.y) / 2F / (type == null ? 1F : type.modelScale);
				mod.rotationPointZ = (leftLeg.z + rightLeg.z) / 2F / (type == null ? 1F : type.modelScale);
				mod.rotateAngleX = Math.max(leftLeg.xRot, rightLeg.xRot);
				mod.rotateAngleY = leftLeg.yRot;
				mod.rotateAngleZ = leftLeg.zRot;
				mod.render(f5);
			}
		}
	}
	
	public void render(ModelRendererTurbo[] models, ModelPart bodyPart, float f5, float scale)
	{
		setBodyPart(models, bodyPart, scale);
		for(ModelRendererTurbo mod : models)
		{
			mod.rotateAngleX = bodyPart.xRot;
			mod.rotateAngleY = bodyPart.yRot;
			mod.rotateAngleZ = bodyPart.zRot;
			mod.render(f5);
		}
	}
	
	public void setBodyPart(ModelRendererTurbo[] models, ModelPart bodyPart, float scale)
	{
		for(ModelRendererTurbo mod : models)
		{
			mod.rotationPointX = bodyPart.x / scale;
			mod.rotationPointY = bodyPart.y / scale;
			mod.rotationPointZ = bodyPart.z / scale;
		}
	}
}
