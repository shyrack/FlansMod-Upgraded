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
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.ShootPoint;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.guns.Paintjob;

public class RenderVehicle extends EntityRenderer<EntityVehicle, RenderVehicle.State> implements CustomItemRenderer
{
	public static class State extends EntityRenderState
	{
		public EntityVehicle vehicle;
		public VehicleType type;
		public ModelVehicle model;
		public Identifier texture;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderVehicle(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityVehicle vehicle, State state, float partialTick)
	{
		super.extractRenderState(vehicle, state, partialTick);
		state.vehicle = vehicle;
		state.type = vehicle.getVehicleType();
		if(state.type == null || state.type.model == null)
		{
			state.model = null;
			return;
		}
		state.model = (ModelVehicle)state.type.model;
		Paintjob paintjob = state.type.getPaintjob(vehicle.getDriveableData().paintjobID);
		state.texture = FlansModResourceHandler.getPaintjobTexture(paintjob);
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		EntityVehicle vehicle = state.vehicle;
		VehicleType type = state.type;
		ModelVehicle modVehicle = state.model;
		if(modVehicle == null)
			return;
		float f1 = state.partialTick;

		float dYaw = (vehicle.axes.getYaw() - vehicle.prevAxes.getYaw());
		while(dYaw > 180F)
		{
			dYaw -= 360F;
		}
		while(dYaw <= -180F)
		{
			dYaw += 360F;
		}
		float dPitch = (vehicle.axes.getPitch() - vehicle.prevAxes.getPitch());
		while(dPitch > 180F)
		{
			dPitch -= 360F;
		}
		while(dPitch <= -180F)
		{
			dPitch += 360F;
		}
		float dRoll = (vehicle.axes.getRoll() - vehicle.prevRotationRoll);
		while(dRoll > 180F)
		{
			dRoll -= 360F;
		}
		while(dRoll <= -180F)
		{
			dRoll += 360F;
		}

		float modelScale = type.modelScale;

		float recoilDPos = (float)Math.sin(Math.toRadians(vehicle.recoilPos)) -
			(float)Math.sin(Math.toRadians(vehicle.lastRecoilPos));
		float recoilPos = (float)Math.sin(Math.toRadians(vehicle.lastRecoilPos)) + recoilDPos * f1;

		pose.pushPose();
		{
			pose.mulPose(Axis.YP.rotationDegrees(180F - vehicle.prevAxes.getYaw() - dYaw * f1));
			pose.mulPose(Axis.ZP.rotationDegrees(vehicle.prevAxes.getPitch() + dPitch * f1));
			pose.mulPose(Axis.XP.rotationDegrees(vehicle.prevRotationRoll + dRoll * f1));
			pose.mulPose(Axis.YP.rotationDegrees(180F));

			pose.pushPose();
			{
				pose.scale(modelScale, modelScale, modelScale);
				if(modVehicle != null)
				{
					collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
					{
						poseStack.pushPose();
						poseStack.last().set(p);
						ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
						modVehicle.render(vehicle, f1);

						for(int i = 0; i < vehicle.trackLinksLeft.length; i++)
						{
							AnimTrackLink link = vehicle.trackLinksLeft[i];
							float rotZ = link.zRot;
							poseStack.pushPose();
							poseStack.translate(link.position.x / 16F, link.position.y / 16F, link.position.z / 16F);
							for(; rotZ > 180F; rotZ -= 360F)
							{
							}
							for(; rotZ <= -180F; rotZ += 360F)
							{
							}
							poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ * (float)(180 / Math.PI)));
							modVehicle.renderFancyTracks(vehicle, f1);
							poseStack.popPose();
						}

						for(int i = 0; i < vehicle.trackLinksRight.length; i++)
						{
							AnimTrackLink link = vehicle.trackLinksRight[i];
							float rotZ = link.zRot;
							for(; rotZ > 180F; rotZ -= 360F)
							{
							}
							for(; rotZ <= -180F; rotZ += 360F)
							{
							}
							poseStack.pushPose();
							poseStack.translate(link.position.x / 16F, link.position.y / 16F, link.position.z / 16F);
							poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ * (float)(180 / Math.PI)));
							modVehicle.renderFancyTracks(vehicle, f1);
							poseStack.popPose();
						}
						ModelRenderer.endRender();
						poseStack.popPose();
					});
				}

				if(type.turretOrigin != null && vehicle.isPartIntact(EnumDriveablePart.turret) &&
					vehicle.getSeat(0) != null)
				{
					pose.pushPose();
					dYaw = (vehicle.getSeat(0).looking.getYaw() - vehicle.getSeat(0).prevLooking.getYaw());
					while(dYaw > 180F)
					{
						dYaw -= 360F;
					}
					while(dYaw <= -180F)
					{
						dYaw += 360F;
					}
					float yaw = vehicle.getSeat(0).prevLooking.getYaw() + dYaw * f1;

					pose.translate(type.turretOrigin.x, type.turretOrigin.y, type.turretOrigin.z);
					pose.mulPose(Axis.YP.rotationDegrees(-yaw));
					pose.translate(-type.turretOrigin.x, -type.turretOrigin.y, -type.turretOrigin.z);

					if(modVehicle != null)
					{
						collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
						{
							poseStack.pushPose();
							poseStack.last().set(p);
							ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
							modVehicle.renderTurret(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, vehicle, f1);

							//rotate and render barrel
							EntitySeat[] seats = vehicle.getSeats();
							poseStack.translate(modVehicle.barrelAttach.x,
								modVehicle.barrelAttach.y,
								-modVehicle.barrelAttach.z);
							float bPitch = (seats[0].looking.getPitch() - seats[0].prevLooking.getPitch());
							float aPitch = seats[0].prevLooking.getPitch() + bPitch * f1;

							poseStack.mulPose(Axis.ZP.rotationDegrees(-aPitch));
							poseStack.translate(recoilPos * -(5F / 16F), 0F, 0F);
							modVehicle.renderAnimBarrel(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F, vehicle, f1);
							ModelRenderer.endRender();
							poseStack.popPose();
						});
					}

					if(FlansMod.DEBUG)
					{
						pose.pushPose();
						pose.translate(type.turretOrigin.x, type.turretOrigin.y, type.turretOrigin.z);
						pose.mulPose(Axis.ZP.rotationDegrees(-vehicle.getSeat(0).looking.getPitch()));
						pose.translate(-type.turretOrigin.x, -type.turretOrigin.y, -type.turretOrigin.z);

						//EntityRenderer shoot points
						collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
						{
							for(ShootPoint point : type.shootPointsPrimary)
							{
								DriveablePosition driveablePosition = point.rootPos;
								if(driveablePosition.part == EnumDriveablePart.turret)
								{
									ModelDriveable.renderOffsetAABB(consumer, p, new AABB(
											driveablePosition.position.x - 0.25F,
											driveablePosition.position.y - 0.25F,
											driveablePosition.position.z - 0.25F,
											driveablePosition.position.x + 0.25F,
											driveablePosition.position.y + 0.25F,
											driveablePosition.position.z + 0.25F),
										0, 0, 0);
								}
							}
							for(ShootPoint point : type.shootPointsSecondary)
							{
								DriveablePosition driveablePosition = point.rootPos;
								if(driveablePosition.part == EnumDriveablePart.turret)
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
						pose.popPose();
					}
					pose.popPose();
				}
				if(modVehicle != null)
				{
					pose.pushPose();

					pose.translate(modVehicle.drillHeadOrigin.x, modVehicle.drillHeadOrigin.y,
						modVehicle.drillHeadOrigin.z);
					pose.mulPose(Axis.XP.rotationDegrees(vehicle.harvesterAngle * 50F));
					pose.translate(-modVehicle.drillHeadOrigin.x, -modVehicle.drillHeadOrigin.y,
						-modVehicle.drillHeadOrigin.z);
					collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
					{
						poseStack.pushPose();
						poseStack.last().set(p);
						ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
						modVehicle.renderDrillBit(vehicle, f1);
						ModelRenderer.endRender();
						poseStack.popPose();
					});
					pose.popPose();
				}
			}
			pose.popPose();

			if(FlansMod.DEBUG)
			{
				pose.pushPose();
				collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
				{
					for(DriveablePart part : vehicle.getDriveableData().parts.values())
					{
						if(part.box == null)
							continue;

						ModelDriveable.renderOffsetAABB(consumer, p, new AABB(part.box.x, part.box.y, part.box.z, (part.box.x + part.box.w),
							(part.box.y + part.box.h), (part.box.z + part.box.d)), 0, 0, 0);
					}

					// EntityRenderer shoot points
					for(ShootPoint point : type.shootPointsPrimary)
					{
						DriveablePosition driveablePosition = point.rootPos;
						if(driveablePosition.part != EnumDriveablePart.turret)
						{
							ModelDriveable.renderOffsetAABB(consumer, p, new AABB(
									driveablePosition.position.x - 0.25F,
									driveablePosition.position.y - 0.25F,
									driveablePosition.position.z - 0.25F,
									driveablePosition.position.x + 0.25F,
									driveablePosition.position.y + 0.25F,
									driveablePosition.position.z + 0.25F),
								0, 0, 0);
						}
					}
					for(ShootPoint point : type.shootPointsSecondary)
					{
						DriveablePosition driveablePosition = point.rootPos;
						if(driveablePosition.part != EnumDriveablePart.turret)
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
				pose.popPose();
			}
		}
		pose.popPose();
	}

	@Override
	public void renderItem(CustomItemRenderType type, InteractionHand hand, ItemStack item, Object... data)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}
}
