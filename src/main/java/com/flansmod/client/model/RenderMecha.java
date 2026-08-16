package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.DriveablePosition;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.ShootPoint;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.driveables.mechas.EnumMechaSlotType;
import com.flansmod.common.driveables.mechas.ItemMecha;
import com.flansmod.common.driveables.mechas.ItemMechaAddon;
import com.flansmod.common.driveables.mechas.MechaItemType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.Paintjob;

public class RenderMecha extends EntityRenderer<EntityMecha, RenderMecha.State> implements CustomItemRenderer
{
	public static class State extends EntityRenderState
	{
		public EntityMecha mecha;
		public MechaType type;
		public ModelMecha model;
		public Identifier texture;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderMecha(EntityRendererProvider.Context context)
	{
		super(context);
		shadowRadius = 0.5F;
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(EntityMecha mecha, State state, float partialTick)
	{
		super.extractRenderState(mecha, state, partialTick);
		state.mecha = mecha;
		state.type = mecha.getMechaType();
		state.model = (ModelMecha)state.type.model;
		Paintjob paintjob = state.type.getPaintjob(mecha.getDriveableData().paintjobID);
		state.texture = FlansModResourceHandler.getPaintjobTexture(paintjob);
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		EntityMecha mecha = state.mecha;
		float scale = 1F / 16F;
		MechaType type = state.type;
		ModelMecha model = state.model;
		float f1 = state.partialTick;

		float dYaw = (mecha.axes.getYaw() - mecha.prevAxes.getYaw());
		for(; dYaw > 180F; dYaw -= 360F)
		{
		}
		for(; dYaw <= -180F; dYaw += 360F)
		{
		}
		float dPitch = (mecha.axes.getPitch() - mecha.prevAxes.getPitch());
		for(; dPitch > 180F; dPitch -= 360F)
		{
		}
		for(; dPitch <= -180F; dPitch += 360F)
		{
		}
		float dRoll = (mecha.axes.getRoll() - mecha.prevRotationRoll);
		for(; dRoll > 180F; dRoll -= 360F)
		{
		}
		for(; dRoll <= -180F; dRoll += 360F)
		{
		}
		float modelScale = mecha.getMechaType().modelScale;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-mecha.prevAxes.getYaw() - dYaw * f1));
		pose.mulPose(Axis.ZP.rotationDegrees(mecha.prevAxes.getPitch() + dPitch * f1));
		pose.mulPose(Axis.XP.rotationDegrees(mecha.prevRotationRoll + dRoll * f1));

		//Body EntityRenderer
		pose.pushPose();
		{
			pose.scale(modelScale, modelScale, modelScale);
			if(model != null)
			{
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
				{
					poseStack.pushPose();
					poseStack.last().set(p);
					ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
					model.render(mecha, f1);
					ModelRenderer.endRender();
					poseStack.popPose();
				});
			}

			//EntityRenderer hips slot : jetpack item
			ItemStack hipsSlot = mecha.inventory.getItem(EnumMechaSlotType.hips);
			if(hipsSlot != null && hipsSlot.getItem() instanceof ItemMechaAddon)
			{
				MechaItemType hipsAddon = ((ItemMechaAddon)hipsSlot.getItem()).type;
				if(hipsAddon.model != null)
				{
					pose.pushPose();
					pose.translate(model.hipsAttachmentPoint.x, model.hipsAttachmentPoint.y, model.hipsAttachmentPoint.z);
					pose.scale(type.heldItemScale, type.heldItemScale, type.heldItemScale);
					Identifier hipsTexture = hipsAddon.texture != null ? FlansModResourceHandler.getTexture(hipsAddon) : state.texture;
					collector.submitCustomGeometry(pose, RenderTypes.entityCutout(hipsTexture), (p, consumer) ->
					{
						poseStack.pushPose();
						poseStack.last().set(p);
						ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
						hipsAddon.model.render(mecha, f1);
						ModelRenderer.endRender();
						poseStack.popPose();
					});
					pose.popPose();
				}
			}
		}
		pose.popPose();

		//Left arm render
		if(mecha.isPartIntact(EnumDriveablePart.leftArm))
		{
			pose.pushPose();

			//Get the arm pitch from the mecha entity
			float smoothedPitch = 0F;
			if(mecha.getSeat(0) != null)
				smoothedPitch = mecha.getSeat(0).prevLooking.getPitch() + (mecha.getSeat(0).looking.getPitch() - mecha.getSeat(0).prevLooking.getPitch()) * f1;

			//Lower Limit
			if(smoothedPitch > type.lowerArmLimit)
				smoothedPitch = type.lowerArmLimit;
			//Upper Limit
			if(smoothedPitch < -type.upperArmLimit)
				smoothedPitch = -type.upperArmLimit;

			//Translate to the arm origin, rotate and render
			pose.translate(type.leftArmOrigin.x, mecha.getMechaType().leftArmOrigin.y, mecha.getMechaType().leftArmOrigin.z);
			pose.mulPose(Axis.ZP.rotationDegrees(90F - smoothedPitch));
			pose.pushPose();
			pose.scale(modelScale, modelScale, modelScale);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.renderLeftArm(scale, mecha, f1);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
			pose.popPose();

			//Move to the end of the arm and render the held item
			pose.translate(0F + type.leftHandModifierY, -type.armLength - type.leftHandModifierX, 0F + type.leftHandModifierZ);
			ItemStack holdingStack = mecha.inventory.getItem(EnumMechaSlotType.leftTool);
			pose.scale(modelScale, modelScale, modelScale);
			if(holdingStack == null || holdingStack.isEmpty())
			{
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
				{
					poseStack.pushPose();
					poseStack.last().set(p);
					ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
					model.renderLeftHand(scale, mecha, f1);
					ModelRenderer.endRender();
					poseStack.popPose();
				});
			}
			else
			{
				pose.scale(type.heldItemScale, type.heldItemScale, type.heldItemScale);
				renderItem(collector, pose, mecha, holdingStack, true, f1, state.lightCoords);
			}
			pose.popPose();
		}

		//Right arm render
		if(mecha.isPartIntact(EnumDriveablePart.rightArm))
		{
			pose.pushPose();

			//Get the arm pitch from the mecha entity
			float smoothedPitch = 0F;
			if(mecha.getSeat(0) != null)
				smoothedPitch = mecha.getSeat(0).prevLooking.getPitch() + (mecha.getSeat(0).looking.getPitch() - mecha.getSeat(0).prevLooking.getPitch()) * f1;

			//Lower Limit
			if(smoothedPitch > type.lowerArmLimit)
				smoothedPitch = type.lowerArmLimit;
			//Upper Limit
			if(smoothedPitch < -type.upperArmLimit)
				smoothedPitch = -type.upperArmLimit;

			//Translate to the arm origin, rotate and render
			pose.translate(type.rightArmOrigin.x, mecha.getMechaType().rightArmOrigin.y, mecha.getMechaType().rightArmOrigin.z);
			pose.mulPose(Axis.ZP.rotationDegrees(90F - smoothedPitch));
			pose.pushPose();
			pose.scale(modelScale, modelScale, modelScale);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
				model.renderRightArm(scale, mecha, f1);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
			pose.popPose();

			//Move to the end of the arm and render the held item
			pose.translate(0F + type.rightHandModifierY, -type.armLength - type.rightHandModifierX, 0F + type.rightHandModifierZ);
			pose.scale(modelScale, modelScale, modelScale);
			ItemStack holdingStack = mecha.inventory.getItem(EnumMechaSlotType.rightTool);
			if(holdingStack == null || holdingStack.isEmpty())
			{
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
				{
					poseStack.pushPose();
					poseStack.last().set(p);
					ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
					model.renderRightHand(scale, mecha, f1);
					ModelRenderer.endRender();
					poseStack.popPose();
				});
			}
			else
			{
				pose.scale(type.heldItemScale, type.heldItemScale, type.heldItemScale);
				renderItem(collector, pose, mecha, holdingStack, false, f1, state.lightCoords);
			}
			pose.popPose();
		}

		//Debug rendering
		if(FlansMod.DEBUG)
		{
			collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
			{
				//EntityRenderer boxes
				for(DriveablePart part : mecha.getDriveableData().parts.values())
				{
					if(part.box == null)
						continue;

					ModelDriveable.renderOffsetAABB(consumer, p, new AABB(part.box.x / 16F, part.box.y / 16F, part.box.z / 16F, (part.box.x + part.box.w) / 16F, (part.box.y + part.box.h) / 16F, (part.box.z + part.box.d) / 16F), 0, 0, 0);
				}

				//EntityRenderer shoot points
				for(ShootPoint point : type.shootPointsPrimary)
				{
					DriveablePosition driveablePosition = point.rootPos;
					ModelDriveable.renderOffsetAABB(consumer, p, new AABB(
							driveablePosition.position.x - 0.25F,
							driveablePosition.position.y - 0.25F,
							driveablePosition.position.z - 0.25F,
							driveablePosition.position.x + 0.25F,
							driveablePosition.position.y + 0.25F,
							driveablePosition.position.z + 0.25F),
						0, 0, 0);
				}
				for(ShootPoint point : type.shootPointsSecondary)
				{
					DriveablePosition driveablePosition = point.rootPos;
					ModelDriveable.renderOffsetAABB(consumer, p, new AABB(
							driveablePosition.position.x - 0.25F,
							driveablePosition.position.y - 0.25F,
							driveablePosition.position.z - 0.25F,
							driveablePosition.position.x + 0.25F,
							driveablePosition.position.y + 0.25F,
							driveablePosition.position.z + 0.25F),
						0, 0, 0);
				}
			});
		}
		pose.popPose();

		//Leg render
		if(mecha.isPartIntact(EnumDriveablePart.hips))
		{
			pose.pushPose();
			dYaw = mecha.legAxes.getYaw() - mecha.prevLegsYaw;
			for(; dYaw > 180F; dYaw -= 360F)
			{
			}
			for(; dYaw <= -180F; dYaw += 360F)
			{
			}
			pose.mulPose(Axis.YP.rotationDegrees(-dYaw * f1 - mecha.prevLegsYaw));
			pose.mulPose(Axis.ZP.rotationDegrees(mecha.prevAxes.getPitch() + dPitch * f1));
			pose.mulPose(Axis.XP.rotationDegrees(mecha.prevRotationRoll + dRoll * f1));
			pose.scale(modelScale, modelScale, modelScale);
			if(model != null)
			{
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
				{
					poseStack.pushPose();
					poseStack.last().set(p);
					ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
					float legLength = type.legLength;
					float rearlegLength = type.RearlegLength;
					float frontlegLength = type.FrontlegLength;
					float legTrans = type.LegTrans;
					float rearlegTrans = type.RearLegTrans;
					float frontlegTrans = type.FrontLegTrans;

					float legsYaw = (float)Math.sin(((mecha.tickCount) + f1) / type.legSwingTime) * mecha.legSwing;
					float footH = (float)Math.sin(legsYaw) * legLength;
					float footV = (float)Math.cos(legsYaw) * legLength;
					float footRH = (float)Math.sin(legsYaw) * rearlegLength;
					float footRV = (float)Math.cos(legsYaw) * rearlegLength;
					float footFH = (float)Math.sin(legsYaw) * frontlegLength;
					float footFV = (float)Math.cos(legsYaw) * frontlegLength;

					//Hips
					model.renderHips(scale, mecha, f1);

					poseStack.pushPose();
					{
						poseStack.translate(legTrans, legLength, 0F);

						//Left Foot
						poseStack.pushPose();
						poseStack.translate(footH, -footV, 0F);
						model.renderLeftFoot(scale, mecha, f1);
						poseStack.popPose();

						//Right Foot
						poseStack.pushPose();
						poseStack.translate(-footH, -footV, 0F);
						model.renderRightFoot(scale, mecha, f1);
						poseStack.popPose();

						//Left Leg
						poseStack.pushPose();
						poseStack.mulPose(Axis.ZP.rotationDegrees(legsYaw * 180F / 3.14159265F));
						poseStack.translate(0F, -legLength, 0F);
						model.renderLeftLeg(scale, mecha, f1);
						poseStack.popPose();

						//Right Leg
						poseStack.pushPose();
						poseStack.mulPose(Axis.ZP.rotationDegrees(-legsYaw * 180F / 3.14159265F));
						poseStack.translate(0F, -legLength, 0F);
						model.renderRightLeg(scale, mecha, f1);
						poseStack.popPose();
					}
					poseStack.popPose();

					poseStack.pushPose();
					{
						poseStack.translate(rearlegTrans, rearlegLength, 0F);

						//Left Rear Foot
						poseStack.pushPose();
						poseStack.translate(-footRH, -footRV, 0F);
						model.renderLeftRearFoot(scale, mecha, f1);
						poseStack.popPose();

						//Right Rear Foot
						poseStack.pushPose();
						poseStack.translate(footRH, -footRV, 0F);
						model.renderRightRearFoot(scale, mecha, f1);
						poseStack.popPose();

						//Left Rear Leg
						poseStack.pushPose();
						poseStack.mulPose(Axis.ZP.rotationDegrees(-legsYaw * 180F / 3.14159265F));
						poseStack.translate(0F, -rearlegLength, 0F);
						model.renderLeftRearLeg(scale, mecha, f1);
						poseStack.popPose();

						//Right Rear Leg
						poseStack.pushPose();
						poseStack.mulPose(Axis.ZP.rotationDegrees(legsYaw * 180F / 3.14159265F));
						poseStack.translate(0F, -rearlegLength, 0F);
						model.renderRightRearLeg(scale, mecha, f1);
						poseStack.popPose();
					}
					poseStack.popPose();

					poseStack.pushPose();
					{
						poseStack.translate(frontlegTrans, frontlegLength, 0F);

						//Left Front Foot
						poseStack.pushPose();
						poseStack.translate(-footFH, -footFV, 0F);
						model.renderLeftFrontFoot(scale, mecha, f1);
						poseStack.popPose();

						//Right Front Foot
						poseStack.pushPose();
						poseStack.translate(footFH, -footFV, 0F);
						model.renderRightFrontFoot(scale, mecha, f1);
						poseStack.popPose();

						//Left Front Leg
						poseStack.pushPose();
						poseStack.mulPose(Axis.ZP.rotationDegrees(-legsYaw * 180F / 3.14159265F));
						poseStack.translate(0F, -frontlegLength, 0F);
						model.renderLeftFrontLeg(scale, mecha, f1);
						poseStack.popPose();

						//Right Front Leg
						poseStack.pushPose();
						poseStack.mulPose(Axis.ZP.rotationDegrees(legsYaw * 180F / 3.14159265F));
						poseStack.translate(0F, -frontlegLength, 0F);
						model.renderRightFrontLeg(scale, mecha, f1);
						poseStack.popPose();
					}
					poseStack.popPose();
					ModelRenderer.endRender();
					poseStack.popPose();
				});
			}
			pose.popPose();
		}
	}

	private void renderItem(SubmitNodeCollector collector, PoseStack pose, EntityMecha mecha, ItemStack stack, boolean leftHand, float dT, int light)
	{
		pose.pushPose();
		pose.mulPose(Axis.ZP.rotationDegrees(-90F));

		//EntityRenderer tools
		if(stack.getItem() instanceof ItemMechaAddon)
		{
			ItemMechaAddon toolItem = (ItemMechaAddon)stack.getItem();
			MechaItemType toolType = toolItem.type;
			Identifier texture = FlansModResourceHandler.getTexture(toolType);
			if(toolType.model != null)
			{
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
				{
					poseStack.pushPose();
					poseStack.last().set(p);
					ModelRenderer.beginRender(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
					toolType.model.render(mecha, dT);
					poseStack.pushPose();
					if((leftHand && mecha.primaryShootHeld) || (!leftHand && mecha.secondaryShootHeld))
					{
						poseStack.mulPose(Axis.XP.rotationDegrees(25F * (float)mecha.tickCount));
					}
					toolType.model.renderDrill(mecha, dT);
					poseStack.popPose();
					toolType.model.renderSaw(mecha, dT, (leftHand && mecha.primaryShootHeld) || (!leftHand && mecha.secondaryShootHeld));
					ModelRenderer.endRender();
					poseStack.popPose();
				});
			}
		}
		else if(stack.getItem() instanceof ItemGun && ((ItemGun)stack.getItem()).GetType() != null && ((ItemGun)stack.getItem()).GetType().model != null)
		{
			GunType gunType = ((ItemGun)stack.getItem()).GetType();
			ModelGun model = gunType.model;
			Identifier texture = FlansModResourceHandler.getTexture(gunType);
			GunAnimations animations = leftHand ? mecha.leftAnimations : mecha.rightAnimations;

			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
			{
				poseStack.pushPose();
				poseStack.last().set(p);
				ModelRenderer.beginRender(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
				RenderGun.renderGunModel(poseStack, stack, gunType, model, animations, 1F / 16F);
				ModelRenderer.endRender();
				poseStack.popPose();
			});
		}
		// TODO: [26.1.2] other held items are rendered through the modern item model system, to be redone
		pose.popPose();
	}

	@Override
	public void renderItem(CustomItemRenderType type, InteractionHand hand, ItemStack item, Object... data)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}
}
