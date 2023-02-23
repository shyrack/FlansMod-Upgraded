//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: Jury
// Model Creator: 
// Created on: 02.03.2020 - 22:50:09
// Last changed on: 02.03.2020 - 22:50:09

package com.shyrack.flansmodupgraded.modernweapons.client.model; //Path where the model is located

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelJury extends ModelGun //Same as Filename
{
	int textureX = 512;
	int textureY = 64;

	public ModelJury() //Same as Filename
	{
		gunModel = new ModelRendererTurbo[16];
		gunModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Box0
		gunModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Import Box0
		gunModel[2] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Import Box0
		gunModel[3] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Import Box0
		gunModel[4] = new ModelRendererTurbo(this, 145, 1, textureX, textureY); // Box 34
		gunModel[5] = new ModelRendererTurbo(this, 161, 1, textureX, textureY); // Box 34
		gunModel[6] = new ModelRendererTurbo(this, 177, 1, textureX, textureY); // Import Box0
		gunModel[7] = new ModelRendererTurbo(this, 201, 1, textureX, textureY); // Import Box0
		gunModel[8] = new ModelRendererTurbo(this, 225, 1, textureX, textureY); // Import Box0
		gunModel[9] = new ModelRendererTurbo(this, 249, 1, textureX, textureY); // Import Box0
		gunModel[10] = new ModelRendererTurbo(this, 273, 1, textureX, textureY); // Import Box0
		gunModel[11] = new ModelRendererTurbo(this, 297, 1, textureX, textureY); // Import Box0
		gunModel[12] = new ModelRendererTurbo(this, 369, 1, textureX, textureY); // Import Box0
		gunModel[13] = new ModelRendererTurbo(this, 409, 1, textureX, textureY); // Import Box0
		gunModel[14] = new ModelRendererTurbo(this, 441, 1, textureX, textureY); // Import Box0
		gunModel[15] = new ModelRendererTurbo(this, 473, 1, textureX, textureY); // Import Box0

		gunModel[0].addBox(0F, -17F, 0F, 7, 8, 6, 0F); // Import Box0
		gunModel[0].setRotationPoint(-7.5F, 7F, -3F);
		gunModel[0].rotateAngleZ = -0.17453293F;

		gunModel[1].addBox(0F, -17F, 0F, 11, 21, 5, 0F); // Import Box0
		gunModel[1].setRotationPoint(-4F, -4F, -2.5F);
		gunModel[1].rotateAngleZ = -0.17453293F;

		gunModel[2].addBox(0F, -17F, 0F, 4, 4, 4, 0F); // Import Box0
		gunModel[2].setRotationPoint(-6F, 4F, -2F);
		gunModel[2].rotateAngleZ = -0.17453293F;

		gunModel[3].addBox(0F, -17F, 0F, 15, 2, 3, 0F); // Import Box0
		gunModel[3].setRotationPoint(2F, 4F, -1.5F);

		gunModel[4].addBox(0F, 0F, 0F, 3, 5, 1, 0F); // Box 34
		gunModel[4].setRotationPoint(2.5F, -35F, -2.5F);
		gunModel[4].rotateAngleX = 1.57079633F;

		gunModel[5].addBox(0F, 0F, 0F, 3, 5, 1, 0F); // Box 34
		gunModel[5].setRotationPoint(54.5F, -35F, -2.5F);
		gunModel[5].rotateAngleX = 1.57079633F;

		gunModel[6].addBox(0F, -17F, 0F, 4, 46, 6, 0F); // Import Box0
		gunModel[6].setRotationPoint(22F, -22F, -3F);
		gunModel[6].rotateAngleZ = -1.57079633F;

		gunModel[7].addBox(0F, -17F, 0F, 4, 4, 4, 0F); // Import Box0
		gunModel[7].setRotationPoint(-7F, -4F, -2F);
		gunModel[7].rotateAngleZ = -0.17453293F;

		gunModel[8].addBox(0F, -17F, 0F, 8, 2, 3, 0F); // Import Box0
		gunModel[8].setRotationPoint(32F, -13F, -1.5F);
		gunModel[8].rotateAngleZ = 1.57079633F;

		gunModel[9].addBox(0F, -17F, 0F, 2, 57, 6, 0F); // Import Box0
		gunModel[9].setRotationPoint(41F, -35F, -3F);
		gunModel[9].rotateAngleZ = -1.57079633F;

		gunModel[10].addBox(0F, -17F, 0F, 4, 36, 7, 0F); // Import Box0
		gunModel[10].setRotationPoint(41F, -26F, -3.5F);
		gunModel[10].rotateAngleZ = -1.57079633F;

		gunModel[11].addBox(0F, -17F, 0F, 4, 14, 6, 0F); // Import Box0
		gunModel[11].setRotationPoint(14F, -18F, -3F);
		gunModel[11].rotateAngleZ = -1.57079633F;

		gunModel[12].addBox(0F, -17F, 0F, 12, 8, 7, 0F); // Import Box0
		gunModel[12].setRotationPoint(-10F, -33F, -3.5F);
		gunModel[12].rotateAngleZ = -1.57079633F;

		gunModel[13].addBox(0F, -17F, 0F, 7, 38, 7, 0F); // Import Box0
		gunModel[13].setRotationPoint(43F, -33F, -3.5F);
		gunModel[13].rotateAngleZ = -1.57079633F;

		gunModel[14].addBox(0F, -17F, 0F, 8, 4, 5, 0F); // Import Box0
		gunModel[14].setRotationPoint(-18F, -30F, -2.5F);
		gunModel[14].rotateAngleZ = -1.57079633F;

		gunModel[15].addBox(0F, -17F, 0F, 4, 7, 4, 0F); // Import Box0
		gunModel[15].setRotationPoint(2F, 11F, -2F);
		gunModel[15].rotateAngleZ = -0.17453293F;


		defaultScopeModel = new ModelRendererTurbo[3];
		defaultScopeModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 34
		defaultScopeModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 34
		defaultScopeModel[2] = new ModelRendererTurbo(this, 137, 1, textureX, textureY); // Box 34

		defaultScopeModel[0].addBox(0F, 0F, 0F, 1, 4, 1, 0F); // Box 34
		defaultScopeModel[0].setRotationPoint(4F, -38F, 1F);

		defaultScopeModel[1].addBox(0F, 0F, 0F, 1, 4, 1, 0F); // Box 34
		defaultScopeModel[1].setRotationPoint(4F, -38F, -2F);

		defaultScopeModel[2].addBox(0F, 0F, 0F, 1, 5, 1, 0F); // Box 34
		defaultScopeModel[2].setRotationPoint(56F, -38F, -0.5F);


		ammoModel = new ModelRendererTurbo[1];
		ammoModel[0] = new ModelRendererTurbo(this, 321, 1, textureX, textureY); // Import Box0

		ammoModel[0].addBox(0F, -17F, 0F, 10, 16, 10, 0F); // Import Box0
		ammoModel[0].setRotationPoint(5F, -32.5F, -5F);
		ammoModel[0].rotateAngleZ = -1.57079633F;



		barrelAttachPoint = new Vector3f(58F /16F, 29F /16F, 0F /16F);

		scopeAttachPoint = new Vector3f(28F /16F, 35F /16F, 0F /16F);

		gripAttachPoint = new Vector3f(50 /16F, 28F /16F, 0F /16F);


		gunSlideDistance = 0.45F;


		animationType = EnumAnimationType.REVOLVER;


		translateAll(0F, 0F, 0F);


		flipAll();
	}
}