package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
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
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.Propeller;
import com.flansmod.common.driveables.ShootPoint;
import com.flansmod.common.guns.Paintjob;

public class RenderPlane extends EntityRenderer<EntityPlane, RenderPlane.State> implements CustomItemRenderer
{
	public static class State extends EntityRenderState
	{
		public EntityPlane plane;
		public PlaneType type;
		public ModelPlane model;
		public Identifier texture;
		public float partialTick;
	}

	private final PoseStack poseStack = new PoseStack();

	public RenderPlane(EntityRendererProvider.Context context)
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
	public void extractRenderState(EntityPlane entityPlane, State state, float partialTick)
	{
		super.extractRenderState(entityPlane, state, partialTick);
		state.plane = entityPlane;
		state.type = entityPlane.getPlaneType();
		state.model = (ModelPlane)state.type.model;
		Paintjob paintjob = state.type.getPaintjob(entityPlane.getDriveableData().paintjobID);
		state.texture = FlansModResourceHandler.getPaintjobTexture(paintjob);
		state.partialTick = partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		EntityPlane entityPlane = state.plane;
		PlaneType type = state.type;
		ModelPlane model = state.model;
		if(model == null)
			return;

		float f1 = state.partialTick;

		float dYaw = (entityPlane.axes.getYaw() - entityPlane.prevAxes.getYaw());
		while(dYaw > 180F)
		{
			dYaw -= 360F;
		}
		while(dYaw <= -180F)
		{
			dYaw += 360F;
		}
		float dPitch = (entityPlane.axes.getPitch() - entityPlane.prevAxes.getPitch());
		while(dPitch > 180F)
		{
			dPitch -= 360F;
		}
		while(dPitch <= -180F)
		{
			dPitch += 360F;
		}
		float dRoll = (entityPlane.axes.getRoll() - entityPlane.prevRotationRoll);
		while(dRoll > 180F)
		{
			dRoll -= 360F;
		}
		while(dRoll <= -180F)
		{
			dRoll += 360F;
		}
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180F - entityPlane.prevAxes.getYaw() - dYaw * f1));
		pose.mulPose(Axis.ZP.rotationDegrees(entityPlane.prevAxes.getPitch() + dPitch * f1));
		pose.mulPose(Axis.XP.rotationDegrees(entityPlane.prevRotationRoll + dRoll * f1));

		float modelScale = type.modelScale;
		pose.scale(modelScale, modelScale, modelScale);

		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY);
			model.render(entityPlane, f1);
			// EntityRenderer helicopter main rotors
			for(int i = 0; i < model.heliMainRotorModels.length; i++)
			{
				poseStack.pushPose();
				poseStack.translate(model.heliMainRotorOrigins[i].x, model.heliMainRotorOrigins[i].y, model.heliMainRotorOrigins[i].z);
				poseStack.mulPose(Axis.YP.rotationDegrees((entityPlane.propAngle + f1 * entityPlane.throttle / 7F) * model.heliRotorSpeeds[i] * 1440F / 3.14159265F));
				poseStack.translate(-model.heliMainRotorOrigins[i].x, -model.heliMainRotorOrigins[i].y, -model.heliMainRotorOrigins[i].z);
				model.renderRotor(entityPlane, 0.0625F, i);
				poseStack.popPose();
			}
			// EntityRenderer helicopter tail rotors
			for(int i = 0; i < model.heliTailRotorModels.length; i++)
			{
				poseStack.pushPose();
				poseStack.translate(model.heliTailRotorOrigins[i].x, model.heliTailRotorOrigins[i].y, model.heliTailRotorOrigins[i].z);
				poseStack.mulPose(Axis.ZP.rotationDegrees((entityPlane.propAngle + f1 * entityPlane.throttle / 7F) * 1440F / 3.14159265F));
				poseStack.translate(-model.heliTailRotorOrigins[i].x, -model.heliTailRotorOrigins[i].y, -model.heliTailRotorOrigins[i].z);
				model.renderTailRotor(entityPlane, 0.0625F, i);
				poseStack.popPose();
			}
			ModelRenderer.endRender();
			poseStack.popPose();
		});

		if(FlansMod.DEBUG)
		{
			pose.pushPose();
			pose.scale(-1F, 1F, -1F);
			collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
			{
				for(DriveablePart part : entityPlane.getDriveableData().parts.values())
				{
					if(part.box == null)
						continue;

					ModelDriveable.renderOffsetAABB(consumer, p, new AABB(part.box.x, part.box.y, part.box.z, (part.box.x + part.box.w),
							(part.box.y + part.box.h), (part.box.z + part.box.d)), 0, 0, 0);
				}
				for(Propeller prop : type.propellers)
				{
					ModelDriveable.renderOffsetAABB(consumer, p, new AABB(prop.x / 16F - 0.25F, prop.y / 16F - 0.25F, prop.z / 16F - 0.25F,
							prop.x / 16F + 0.25F, prop.y / 16F + 0.25F, prop.z / 16F + 0.25F), 0, 0, 0);
				}
				// EntityRenderer shoot points
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
			pose.popPose();
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRender(EntityPlane entity, Frustum camera, double camX, double camY, double camZ)
	{
		return true;
	}

	@Override
	public void renderItem(CustomItemRenderType type, InteractionHand hand, ItemStack item, Object... data)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}
}
