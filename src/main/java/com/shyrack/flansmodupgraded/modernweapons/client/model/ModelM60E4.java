//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: M60E4
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

public class ModelM60E4 extends ModelGun //Same as Filename
{
	int textureX = 1024;
	int textureY = 128;

	public ModelM60E4() //Same as Filename
	{
		gunModel = new ModelRendererTurbo[30];
		gunModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Box0
		gunModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Import Box0
		gunModel[2] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Import Box0
		gunModel[3] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Import Box0
		gunModel[4] = new ModelRendererTurbo(this, 217, 1, textureX, textureY); // Import Box0
		gunModel[5] = new ModelRendererTurbo(this, 257, 1, textureX, textureY); // Import Box0
		gunModel[6] = new ModelRendererTurbo(this, 265, 1, textureX, textureY); // Import Box0
		gunModel[7] = new ModelRendererTurbo(this, 297, 1, textureX, textureY); // Import Box0
		gunModel[8] = new ModelRendererTurbo(this, 409, 1, textureX, textureY); // Import Box0
		gunModel[9] = new ModelRendererTurbo(this, 529, 1, textureX, textureY); // Import Box0
		gunModel[10] = new ModelRendererTurbo(this, 609, 1, textureX, textureY); // Import Box0
		gunModel[11] = new ModelRendererTurbo(this, 665, 1, textureX, textureY); // Import Box0
		gunModel[12] = new ModelRendererTurbo(this, 833, 1, textureX, textureY); // Import Box0
		gunModel[13] = new ModelRendererTurbo(this, 889, 1, textureX, textureY); // Import Box0
		gunModel[14] = new ModelRendererTurbo(this, 921, 1, textureX, textureY); // Import Box0
		gunModel[15] = new ModelRendererTurbo(this, 977, 1, textureX, textureY); // Import Box0
		gunModel[16] = new ModelRendererTurbo(this, 57, 17, textureX, textureY); // Import Box0
		gunModel[17] = new ModelRendererTurbo(this, 113, 17, textureX, textureY); // Import Box0
		gunModel[18] = new ModelRendererTurbo(this, 145, 17, textureX, textureY); // Import Box0
		gunModel[19] = new ModelRendererTurbo(this, 209, 17, textureX, textureY); // Import Box0
		gunModel[20] = new ModelRendererTurbo(this, 521, 17, textureX, textureY); // Import Box0
		gunModel[21] = new ModelRendererTurbo(this, 257, 17, textureX, textureY); // Import Box0
		gunModel[22] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Import Box0
		gunModel[23] = new ModelRendererTurbo(this, 697, 17, textureX, textureY); // Import Box0
		gunModel[24] = new ModelRendererTurbo(this, 793, 17, textureX, textureY); // Import Box0
		gunModel[25] = new ModelRendererTurbo(this, 857, 17, textureX, textureY); // Import Box0
		gunModel[26] = new ModelRendererTurbo(this, 929, 17, textureX, textureY); // Import Box0
		gunModel[27] = new ModelRendererTurbo(this, 321, 25, textureX, textureY); // Import Box0
		gunModel[28] = new ModelRendererTurbo(this, 985, 17, textureX, textureY); // Import Box0
		gunModel[29] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Import Box0

		gunModel[0].addBox(0F, -17F, 0F, 7, 8, 4, 0F); // Import Box0
		gunModel[0].setRotationPoint(-4F, 18F, -2F);
		gunModel[0].rotateAngleZ = -0.05235988F;

		gunModel[1].addBox(0F, -17F, 0F, 9, 22, 3, 0F); // Import Box0
		gunModel[1].setRotationPoint(-1F, 6F, -1.5F);
		gunModel[1].rotateAngleZ = -0.17453293F;

		gunModel[2].addBox(0F, -17F, 0F, 5, 12, 2, 0F); // Import Box0
		gunModel[2].setRotationPoint(-3F, 11F, -1F);
		gunModel[2].rotateAngleZ = -0.17453293F;

		gunModel[3].addBox(0F, -17F, 0F, 61, 6, 7, 0F); // Import Box0
		gunModel[3].setRotationPoint(0F, 2F, -3.5F);

		gunModel[4].addBox(0F, -17F, 0F, 11, 6, 5, 0F); // Import Box0
		gunModel[4].setRotationPoint(-5F, 22F, -2.5F);
		gunModel[4].rotateAngleZ = -0.19198622F;

		gunModel[5].addBox(0F, -17F, 0F, 4, 4, 3, 0F); // Import Box0
		gunModel[5].setRotationPoint(-4F, 2F, -1.5F);
		gunModel[5].rotateAngleZ = -0.05235988F;

		gunModel[6].addBox(0F, -17F, 0F, 4, 6, 9, 0F); // Import Box0
		gunModel[6].setRotationPoint(24F, 8F, -4.5F);

		gunModel[7].addBox(0F, -17F, 0F, 46, 10, 7, 0F); // Import Box0
		gunModel[7].setRotationPoint(-11F, -8F, -3.5F);

		gunModel[8].addBox(0F, -17F, 0F, 52, 12, 6, 0F); // Import Box0
		gunModel[8].setRotationPoint(-61F, -7F, -3F);

		gunModel[9].addBox(0F, -17F, 0F, 35, 11, 4, 0F); // Import Box0
		gunModel[9].setRotationPoint(-53F, 6F, -2F);
		gunModel[9].rotateAngleZ = 0.4712389F;

		gunModel[10].addBox(0F, -17F, 0F, 19, 6, 5, 0F); // Import Box0
		gunModel[10].setRotationPoint(-44F, 3F, -2.5F);
		gunModel[10].rotateAngleZ = 1.57079633F;

		gunModel[11].addBox(0F, -17F, 0F, 78, 5, 5, 0F); // Import Box0
		gunModel[11].setRotationPoint(32F, -7.5F, -2.5F);

		gunModel[12].addBox(0F, -17F, 0F, 24, 11, 3, 0F); // Import Box0
		gunModel[12].setRotationPoint(35F, -7.5F, 2.5F);

		gunModel[13].addBox(0F, -17F, 0F, 12, 8, 2, 0F); // Import Box0
		gunModel[13].setRotationPoint(59F, -7.5F, 3F);

		gunModel[14].addBox(0F, -17F, 0F, 24, 11, 3, 0F); // Import Box0
		gunModel[14].setRotationPoint(35F, -7.5F, -5.5F);

		gunModel[15].addBox(0F, -17F, 0F, 12, 8, 2, 0F); // Import Box0
		gunModel[15].setRotationPoint(59F, -7.5F, -5F);

		gunModel[16].addBox(0F, -17F, 0F, 24, 8, 2, 0F); // Import Box0
		gunModel[16].setRotationPoint(35F, -24.5F, 13F);
		gunModel[16].rotateAngleX = 1.57079633F;

		gunModel[17].addBox(0F, -17F, 0F, 12, 8, 2, 0F); // Import Box0
		gunModel[17].setRotationPoint(59F, -24.5F, 13F);
		gunModel[17].rotateAngleX = 1.57079633F;

		gunModel[18].addBox(0F, -17F, 0F, 25, 9, 4, 0F); // Import Box0
		gunModel[18].setRotationPoint(35F, -12.5F, 12.5F);
		gunModel[18].rotateAngleX = 1.57079633F;

		gunModel[19].addBox(0F, -17F, 0F, 20, 8, 7, 0F); // Import Box0
		gunModel[19].setRotationPoint(59F, -12.5F, 13F);
		gunModel[19].rotateAngleX = 1.57079633F;

		gunModel[20].addBox(0F, -17F, 0F, 20, 6, 5, 0F); // Import Box0
		gunModel[20].setRotationPoint(-3F, -11F, -2.5F);

		gunModel[21].addBox(0F, -17F, 0F, 16, 1, 3, 0F); // Import Box0
		gunModel[21].setRotationPoint(10F, 13F, -1.5F);

		gunModel[22].addBox(0F, -17F, 0F, 3, 6, 4, 0F); // Import Box0
		gunModel[22].setRotationPoint(95F, -4.5F, -2F);

		gunModel[23].addBox(0F, -17F, 0F, 40, 5, 5, 0F); // Import Box0
		gunModel[23].setRotationPoint(29F, -11F, -2.5F);

		gunModel[24].addBox(0F, -17F, 0F, 21, 6, 8, 0F); // Import Box0
		gunModel[24].setRotationPoint(-38F, -9F, -4F);

		gunModel[25].addBox(0F, -17F, 0F, 27, 3, 6, 0F); // Import Box0
		gunModel[25].setRotationPoint(-47F, 4F, -3F);
		gunModel[25].rotateAngleZ = 1.57079633F;

		gunModel[26].addBox(0F, -17F, 0F, 19, 3, 6, 0F); // Import Box0
		gunModel[26].setRotationPoint(-37F, -10F, -3F);

		gunModel[27].addBox(0F, -17F, 0F, 78, 3, 3, 0F); // Import Box0
		gunModel[27].setRotationPoint(32F, -0.5F, -1.5F);

		gunModel[28].addBox(0F, -17F, 0F, 2, 10, 2, 0F); // Import Box0
		gunModel[28].setRotationPoint(40F, -16.5F, -0.5F);
		gunModel[28].rotateAngleX = 0.41887902F;

		gunModel[29].addBox(0F, -17F, 0F, 22, 5, 5, 0F); // Import Box0
		gunModel[29].setRotationPoint(24F, -19F, -9.5F);


		defaultScopeModel = new ModelRendererTurbo[2];
		defaultScopeModel[0] = new ModelRendererTurbo(this, 265, 25, textureX, textureY); // Import Box0
		defaultScopeModel[1] = new ModelRendererTurbo(this, 289, 25, textureX, textureY); // Import Box0

		defaultScopeModel[0].addBox(0F, -17F, 0F, 12, 1, 1, 0F); // Import Box0
		defaultScopeModel[0].setRotationPoint(108F, -11.5F, -0.5F);
		defaultScopeModel[0].rotateAngleZ = 0.83775804F;

		defaultScopeModel[1].addBox(0F, -17F, 0F, 12, 2, 3, 0F); // Import Box0
		defaultScopeModel[1].setRotationPoint(120F, -20.5F, -1.5F);
		defaultScopeModel[1].rotateAngleZ = 1.57079633F;


		ammoModel = new ModelRendererTurbo[2];
		ammoModel[0] = new ModelRendererTurbo(this, 553, 17, textureX, textureY); // Import Box0
		ammoModel[1] = new ModelRendererTurbo(this, 489, 25, textureX, textureY); // Import Box0

		ammoModel[0].addBox(0F, -17F, 0F, 24, 29, 24, 0F); // Import Box0
		ammoModel[0].setRotationPoint(15.5F, 6F, 3F);

		ammoModel[1].addBox(0F, -17F, 0F, 13, 22, 2, 0F); // Import Box0
		ammoModel[1].setRotationPoint(18.5F, -11F, 13F);
		ammoModel[1].rotateAngleX = 0.90757121F;


		slideModel = new ModelRendererTurbo[2];
		slideModel[0] = new ModelRendererTurbo(this, 633, 17, textureX, textureY); // Import Box0
		slideModel[1] = new ModelRendererTurbo(this, 1001, 9, textureX, textureY); // Import Box0

		slideModel[0].addBox(0F, -17F, 0F, 22, 4, 7, 0F); // Import Box0
		slideModel[0].setRotationPoint(4F, -6F, -4.5F);

		slideModel[1].addBox(0F, -17F, 0F, 3, 6, 6, 0F); // Import Box0
		slideModel[1].setRotationPoint(3F, -6F, -6.5F);



		barrelAttachPoint = new Vector3f(109F /16F, 23F /16F, 0F /16F);

		stockAttachPoint = new Vector3f(41F /16F, 20F /16F, 5F /16F);

		scopeAttachPoint = new Vector3f(4F /16F, 27F /16F, 0F /16F);

		gripAttachPoint = new Vector3f(50 /16F, 14F /16F, 0F /16F);


		gunSlideDistance = 0.45F;


		animationType = EnumAnimationType.BOTTOM_CLIP;


		translateAll(0F, 0F, 0F);


		flipAll();
	}
}