package com.flansmod.yeolde.client.model;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.flansmod.client.model.ModelBase;
import com.flansmod.client.model.ModelRenderer;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.world.entity.Entity;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.util.WorldRenderer;

public class ModelArrow extends ModelBase
{
	public ModelRendererTurbo bulletModel;

	public ModelArrow()
	{
		bulletModel = new ModelRendererTurbo(this, 0, 0);
		bulletModel.addBox(-0.5F, -1F, -0.5F, 1, 2, 1);
	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		ModelRenderer.RenderContext ctx = ModelRenderer.getRenderContext();
		if(ctx == null || ctx.poseStack == null || ctx.consumer == null)
			return;
		PoseStack pose = ctx.poseStack;
		VertexConsumer consumer = ctx.consumer;
		float var11 = 0;
		float var12 = 0.0F;
		float var13 = 0.5F;
		float var14 = (var11 * 10) / 32.0F;
		float var15 = (5 + var11 * 10) / 32.0F;
		float var16 = 0.0F;
		float var17 = 0.15625F;
		float var18 = (5 + var11 * 10) / 32.0F;
		float var19 = (10 + var11 * 10) / 32.0F;
		float var20 = 0.05625F;
		pose.pushPose();
		pose.mulPose(Axis.ZP.rotationDegrees(90F));
		pose.mulPose(Axis.XP.rotationDegrees(45.0F));
		pose.scale(var20, var20, var20);
		pose.translate(-4.0F, 0.0F, 0.0F);
		PoseStack.Pose p = pose.last();
		consumer.setNormal(var20, 0.0F, 0.0F);
		consumer.addVertex(p, -7.0F, -2.0F, -2.0F).setUv(var16, var18);
		consumer.addVertex(p, -7.0F, -2.0F, 2.0F).setUv(var17, var18);
		consumer.addVertex(p, -7.0F, 2.0F, 2.0F).setUv(var17, var19);
		consumer.addVertex(p, -7.0F, 2.0F, -2.0F).setUv(var16, var19);
		consumer.setNormal(-var20, 0.0F, 0.0F);
		consumer.addVertex(p, -7.0F, 2.0F, -2.0F).setUv(var16, var18);
		consumer.addVertex(p, -7.0F, 2.0F, 2.0F).setUv(var17, var18);
		consumer.addVertex(p, -7.0F, -2.0F, 2.0F).setUv(var17, var19);
		consumer.addVertex(p, -7.0F, -2.0F, -2.0F).setUv(var16, var19);

		for(int var23 = 0; var23 < 4; ++var23)
		{
			pose.mulPose(Axis.XP.rotationDegrees(90.0F));
			p = pose.last();
			consumer.setNormal(0.0F, 0.0F, var20);
			consumer.addVertex(p, -8.0F, -2.0F, 0.0F).setUv(var12, var14);
			consumer.addVertex(p, 8.0F, -2.0F, 0.0F).setUv(var13, var14);
			consumer.addVertex(p, 8.0F, 2.0F, 0.0F).setUv(var13, var15);
			consumer.addVertex(p, -8.0F, 2.0F, 0.0F).setUv(var12, var15);
		}
		pose.popPose();
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}
}
