//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: 2xScope
// Model Creator: 
// Created on: 15.05.2020 - 15:19:41
// Last changed on: 15.05.2020 - 15:19:41

package com.shyrack.flansmodupgraded.modernweapons.client.model; //Path where the model is located

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class Model2xScope extends ModelAttachment //Same as Filename
{
	int textureX = 512;
	int textureY = 64;

	public Model2xScope() //Same as Filename
	{
		attachmentModel = new ModelRendererTurbo[17];
		attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 01
		attachmentModel[1] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Import 03
		attachmentModel[2] = new ModelRendererTurbo(this, 161, 1, textureX, textureY); // Import 06
		attachmentModel[3] = new ModelRendererTurbo(this, 241, 1, textureX, textureY); // Import 01
		attachmentModel[4] = new ModelRendererTurbo(this, 289, 1, textureX, textureY); // Import 01
		attachmentModel[5] = new ModelRendererTurbo(this, 337, 1, textureX, textureY); // Import 06
		attachmentModel[6] = new ModelRendererTurbo(this, 393, 1, textureX, textureY); // Import 06
		attachmentModel[7] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Import 06
		attachmentModel[8] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Import 06
		attachmentModel[9] = new ModelRendererTurbo(this, 145, 1, textureX, textureY); // Import 06
		attachmentModel[10] = new ModelRendererTurbo(this, 225, 1, textureX, textureY); // Import 06
		attachmentModel[11] = new ModelRendererTurbo(this, 473, 1, textureX, textureY); // Import 06
		attachmentModel[12] = new ModelRendererTurbo(this, 321, 33, textureX, textureY); // Import 06
		attachmentModel[13] = new ModelRendererTurbo(this, 361, 33, textureX, textureY); // Import 01
		attachmentModel[14] = new ModelRendererTurbo(this, 465, 33, textureX, textureY); // Import 01
		attachmentModel[15] = new ModelRendererTurbo(this, 489, 33, textureX, textureY); // Import 01
		attachmentModel[16] = new ModelRendererTurbo(this, 169, 41, textureX, textureY); // Import 01

		attachmentModel[0].addBox(-16F, 0F, -6F, 22, 3, 12, 0F); // Import 01
		attachmentModel[0].setRotationPoint(22F, -4F, 0F);

		attachmentModel[1].addBox(-16F, 20F, -8F, 32, 6, 19, 0F); // Import 03
		attachmentModel[1].setRotationPoint(16F, -25F, -2F);

		attachmentModel[2].addBox(-15F, 10.5F, -1.5F, 20, 16, 16, 0F); // Import 06
		attachmentModel[2].setRotationPoint(-28F, -42F, 6F);
		attachmentModel[2].rotateAngleY = 3.14159265F;

		attachmentModel[3].addBox(-16F, 0F, -6F, 6, 16, 17, 0F); // Import 01
		attachmentModel[3].setRotationPoint(20F, -32F, -3F);

		attachmentModel[4].addBox(-16F, 0F, -6F, 6, 16, 17, 0F); // Import 01
		attachmentModel[4].setRotationPoint(38F, -32F, -3F);

		attachmentModel[5].addBox(-15F, 10.5F, -1.5F, 10, 14, 14, 0F); // Import 06
		attachmentModel[5].setRotationPoint(-18F, -41F, 5F);
		attachmentModel[5].rotateAngleY = 3.14159265F;

		attachmentModel[6].addBox(-15F, 10.5F, -1.5F, 20, 16, 16, 0F); // Import 06
		attachmentModel[6].setRotationPoint(52F, -42F, 6F);
		attachmentModel[6].rotateAngleY = 3.14159265F;

		attachmentModel[7].addBox(-15F, 10.5F, -1.5F, 10, 14, 14, 0F); // Import 06
		attachmentModel[7].setRotationPoint(32F, -41F, 5F);
		attachmentModel[7].rotateAngleY = 3.14159265F;

		attachmentModel[8].addBox(-15F, 10.5F, -1.5F, 40, 12, 12, 0F); // Import 06
		attachmentModel[8].setRotationPoint(22F, -40F, 4F);
		attachmentModel[8].rotateAngleY = 3.14159265F;

		attachmentModel[9].addBox(-15F, 10.5F, -1.5F, 8, 8, 4, 0F); // Import 06
		attachmentModel[9].setRotationPoint(6F, -38F, 8F);
		attachmentModel[9].rotateAngleY = 3.14159265F;

		attachmentModel[10].addBox(-15F, 10.5F, -1.5F, 8, 4, 8, 0F); // Import 06
		attachmentModel[10].setRotationPoint(6F, -44F, 2F);
		attachmentModel[10].rotateAngleY = 3.14159265F;

		attachmentModel[11].addBox(-15F, 10.5F, -1.5F, 2, 15, 15, 0F); // Import 06
		attachmentModel[11].setRotationPoint(-46.01F, -41.5F, 5.5F);
		attachmentModel[11].rotateAngleY = 3.14159265F;

		attachmentModel[12].addBox(-15F, 10.5F, -1.5F, 2, 15, 15, 0F); // Import 06
		attachmentModel[12].setRotationPoint(52.1F, -41.5F, 5.5F);
		attachmentModel[12].rotateAngleY = 3.14159265F;

		attachmentModel[13].addBox(-16F, 0F, -6F, 6, 16, 4, 0F); // Import 01
		attachmentModel[13].setRotationPoint(20F, -16F, -3F);

		attachmentModel[14].addBox(-16F, 0F, -6F, 6, 16, 4, 0F); // Import 01
		attachmentModel[14].setRotationPoint(38F, -16F, -3F);

		attachmentModel[15].addBox(-16F, 0F, -6F, 6, 16, 4, 0F); // Import 01
		attachmentModel[15].setRotationPoint(20F, -16F, 10F);

		attachmentModel[16].addBox(-16F, 0F, -6F, 6, 16, 4, 0F); // Import 01
		attachmentModel[16].setRotationPoint(38F, -16F, 10F);



		flipAll();
	}
}