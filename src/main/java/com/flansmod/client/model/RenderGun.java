package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.EnumFireMode;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.vector.Vector3f;

public class RenderGun implements CustomItemRenderer
{
	public static float smoothing;
	public static boolean bindTextures = true;

	@Override
	public void renderItem(CustomItemRenderType type, InteractionHand hand, ItemStack item, Object... data)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}

	//EntityRenderer off-hand gun in 3rd person
	public void renderOffHandGun(Player player, ItemStack offHandItemStack)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}

	/**
	 * Gun render method, separated from transforms so that the mecha renderer
	 * may also call this. Renders the gun model with the given animations into
	 * the current model render context (set up by ModelRenderer.beginRender).
	 */
	public static void renderGunModel(PoseStack poseStack, ItemStack item, GunType gunType, ModelGun model, GunAnimations animations, float f)
	{
		if(model == null)
			return;

		//If we have no animation variables, use defaults
		if(animations == null)
			animations = GunAnimations.defaults;

		// Do we have a muzzle flash
		ModelMuzzleFlash mfModel = gunType.muzzleFlashModel;
		boolean renderMuzzleFlash = mfModel != null && animations.muzzleFlash > 0;

		//Get all the attachments that we may need to render
		AttachmentType scopeAttachment = gunType.getScope(item);
		AttachmentType barrelAttachment = gunType.getBarrel(item);
		AttachmentType stockAttachment = gunType.getStock(item);
		AttachmentType gripAttachment = gunType.getGrip(item);

		ItemStack[] bulletStacks = new ItemStack[gunType.numAmmoItemsInGun];
		boolean empty = true;
		for(int i = 0; i < gunType.numAmmoItemsInGun; i++)
		{
			bulletStacks[i] = ((ItemGun)item.getItem()).getBulletItemStack(item, i);
			if(bulletStacks[i] != null && bulletStacks[i].getItem() instanceof ItemBullet && bulletStacks[i].getDamageValue() < bulletStacks[i].getMaxDamage())
				empty = false;
		}

		//Calculate the amount of tilt required for the reloading animation
		float reloadRotate = 0F;
		if(animations.reloading)
		{
			float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
			reloadRotate = 1F;
			if(effectiveReloadAnimationProgress < model.tiltGunTime)
				reloadRotate = effectiveReloadAnimationProgress / model.tiltGunTime;
			if(effectiveReloadAnimationProgress > model.tiltGunTime + model.unloadClipTime + model.loadClipTime)
				reloadRotate = 1F - (effectiveReloadAnimationProgress - (model.tiltGunTime + model.unloadClipTime + model.loadClipTime)) / model.untiltGunTime;
		}

		if(scopeAttachment != null)
			poseStack.translate(0F, -scopeAttachment.model.renderOffset / 16F, 0F);

		//EntityRenderer the gun and default attachment models
		poseStack.pushPose();
		{
			poseStack.scale(gunType.modelScale, gunType.modelScale, gunType.modelScale);

			model.renderGun(f);
			model.renderCustom(f, animations);
			if(scopeAttachment == null && !model.scopeIsOnSlide && !model.scopeIsOnBreakAction)
				model.renderDefaultScope(f);
			if(barrelAttachment == null)
				model.renderDefaultBarrel(f);
			if(stockAttachment == null)
				model.renderDefaultStock(f);
			if(gripAttachment == null && !model.gripIsOnPump)
				model.renderDefaultGrip(f);

			//EntityRenderer various shoot / reload animated parts
			//EntityRenderer the slide
			poseStack.pushPose();
			{
				poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
				model.renderSlide(f);
				if(scopeAttachment == null && model.scopeIsOnSlide)
					model.renderDefaultScope(f);
			}
			poseStack.popPose();

			//EntityRenderer the break action
			poseStack.pushPose();
			{
				poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
				poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -model.breakAngle));
				poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
				model.renderBreakAction(f);
				if(scopeAttachment == null && model.scopeIsOnBreakAction)
					model.renderDefaultScope(f);
			}
			poseStack.popPose();

			//EntityRenderer the pump-action handle
			poseStack.pushPose();
			{
				poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				model.renderPump(f);
				if(gripAttachment == null && model.gripIsOnPump)
					model.renderDefaultGrip(f);
			}
			poseStack.popPose();

			//EntityRenderer the minigun barrels
			if(gunType.mode == EnumFireMode.MINIGUN)
			{
				poseStack.pushPose();
				poseStack.translate(model.minigunBarrelOrigin.x, model.minigunBarrelOrigin.y, model.minigunBarrelOrigin.z);
				poseStack.mulPose(Axis.XP.rotationDegrees(animations.minigunBarrelRotation));
				poseStack.translate(-model.minigunBarrelOrigin.x, -model.minigunBarrelOrigin.y, -model.minigunBarrelOrigin.z);
				model.renderMinigunBarrel(f);
				poseStack.popPose();
			}

			//EntityRenderer the revolver barrel
			poseStack.pushPose();
			{
				poseStack.translate(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
				poseStack.mulPose(Axis.XP.rotationDegrees(reloadRotate * model.revolverFlipAngle));
				poseStack.translate(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
				model.renderRevolverBarrel(f);
			}
			poseStack.popPose();

			//EntityRenderer the clip
			poseStack.pushPose();
			{
				boolean shouldRender = true;
				//Check to see if the ammo should be rendered first
				switch(model.animationType)
				{
					case END_LOADED: case BACK_LOADED:
				{
					if(empty)
						shouldRender = false;
					break;
				}
					default: break;
				}

				if(shouldRender)
					model.renderAmmo(f);
			}
			poseStack.popPose();
		}
		poseStack.popPose();

		//EntityRenderer static attachments
		//Scope
		if(scopeAttachment != null)
		{
			poseStack.pushPose();
			{
				if(model.scopeIsOnBreakAction)
				{
					poseStack.translate(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
					poseStack.mulPose(Axis.ZP.rotationDegrees(reloadRotate * -model.breakAngle));
					poseStack.translate(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
				}
				poseStack.translate(model.scopeAttachPoint.x * gunType.modelScale, model.scopeAttachPoint.y * gunType.modelScale, model.scopeAttachPoint.z * gunType.modelScale);

				if(model.scopeIsOnSlide)
					poseStack.translate(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
				poseStack.scale(scopeAttachment.modelScale, scopeAttachment.modelScale, scopeAttachment.modelScale);
				ModelAttachment scopeModel = scopeAttachment.model;
				if(scopeModel != null)
					scopeModel.renderAttachment(f);
			}
			poseStack.popPose();
		}

		//Grip
		if(gripAttachment != null)
		{
			poseStack.pushPose();
			{
				poseStack.translate(model.gripAttachPoint.x * gunType.modelScale, model.gripAttachPoint.y * gunType.modelScale, model.gripAttachPoint.z * gunType.modelScale);
				if(model.gripIsOnPump)
					poseStack.translate(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				poseStack.scale(gripAttachment.modelScale, gripAttachment.modelScale, gripAttachment.modelScale);
				ModelAttachment gripModel = gripAttachment.model;
				if(gripModel != null)
					gripModel.renderAttachment(f);
			}
			poseStack.popPose();
		}

		//Barrel
		if(barrelAttachment != null)
		{
			poseStack.pushPose();
			{
				poseStack.translate(model.barrelAttachPoint.x * gunType.modelScale, model.barrelAttachPoint.y * gunType.modelScale, model.barrelAttachPoint.z * gunType.modelScale);
				poseStack.scale(barrelAttachment.modelScale, barrelAttachment.modelScale, barrelAttachment.modelScale);
				ModelAttachment barrelModel = barrelAttachment.model;
				if(barrelModel != null)
					barrelModel.renderAttachment(f);
			}
			poseStack.popPose();
		}

		//Stock
		if(stockAttachment != null)
		{
			poseStack.pushPose();
			{
				poseStack.translate(model.stockAttachPoint.x * gunType.modelScale, model.stockAttachPoint.y * gunType.modelScale, model.stockAttachPoint.z * gunType.modelScale);
				poseStack.scale(stockAttachment.modelScale, stockAttachment.modelScale, stockAttachment.modelScale);
				ModelAttachment stockModel = stockAttachment.model;
				if(stockModel != null)
					stockModel.renderAttachment(f);
			}
			poseStack.popPose();
		}

		if(renderMuzzleFlash)
		{
			Vector3f mfPoint = model.muzzleFlashPoint;
			if(mfPoint == ModelGun.invalid)
			{
				mfPoint = model.barrelAttachPoint;
			}
			if(barrelAttachment != null)
			{
				mfPoint = Vector3f.add(model.barrelAttachPoint, barrelAttachment.model.muzzleFlashPoint, null);
			}

			poseStack.pushPose();
			{
				poseStack.translate(mfPoint.x * gunType.modelScale, mfPoint.y * gunType.modelScale, mfPoint.z * gunType.modelScale);
				mfModel.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, f);
			}
			poseStack.popPose();
		}
	}

	/**
	 * Old item render entry point kept for API compatibility. Use renderGunModel instead.
	 */
	@Deprecated
	public void renderGun(ItemStack item, GunType type, float f, ModelGun model, GunAnimations animations, float reloadRotate)
	{
		// TODO: [26.1.2] item in hand rendering is done through the modern item model system and will be redone later
	}

	/**
	 * Renders a dropped gun item (EntityItemCustomRender) into a submit node collector.
	 */
	public static void renderGunItem(SubmitNodeCollector collector, PoseStack pose, ItemStack item, int light, float yaw, float partialTick)
	{
		if(!(item.getItem() instanceof ItemGun))
			return;

		GunType gunType = ((ItemGun)item.getItem()).GetType();
		if(gunType == null)
			return;

		ModelGun model = gunType.model;
		if(model == null)
			return;

		GunAnimations animations = new GunAnimations();
		Paintjob paintjob = gunType.getPaintjob(item.getDamageValue());
		Identifier texture = FlansModResourceHandler.getPaintjobTexture(paintjob);

		pose.pushPose();
		pose.translate(0F, 0.25F, 0F);
		pose.mulPose(Axis.YP.rotationDegrees(yaw + partialTick));
		pose.translate(-0.45F + model.itemFrameOffset.x, -0.05F + model.itemFrameOffset.y, model.itemFrameOffset.z);

		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, consumer) ->
		{
			poseStack.pushPose();
			poseStack.last().set(p);
			ModelRenderer.beginRender(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
			renderGunModel(poseStack, item, gunType, model, animations, 1F / 16F);
			ModelRenderer.endRender();
			poseStack.popPose();
		});
		pose.popPose();
	}

	private static final PoseStack poseStack = new PoseStack();
}
