package com.flansmod.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.flansmod.common.ModEntities;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.vector.Vector3f;

public class EntityCamera extends LivingEntity
{
	protected Level world;

	public EntityDriveable driveable;

	public EntityCamera(EntityType<? extends LivingEntity> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityCamera(Level world)
	{
		this(ModEntities.CAMERA, world);
	}

	public EntityCamera(Level world, EntityDriveable d)
	{
		this(world);
		driveable = d;
		setPos(d.getX(), d.getY(), d.getZ());
	}

	@Override
	public void tick()
	{
		super.tick();

		xo = getX();
		yo = getY();
		zo = getZ();

		Vector3f cameraPosition = new Vector3f();
		cameraPosition = driveable.axes.findLocalVectorGlobally(cameraPosition);

		// Lerp it
		double dX = driveable.getX() + cameraPosition.x - getX();
		double dY = driveable.getY() + cameraPosition.y - getY();
		double dZ = driveable.getZ() + cameraPosition.z - getZ();

		float lerpAmount = 0.1F;

		setPos(getX() + dX * lerpAmount, getY() + dY * lerpAmount, getZ() + dZ * lerpAmount);

		setYRot(driveable.axes.getYaw() - 90);
		setXRot(driveable.axes.getPitch());

		while(getYRot() - yRotO >= 180F)
		{
			setYRot(getYRot() - 360F);
		}
		while(getYRot() - yRotO < -180F)
		{
			setYRot(getYRot() + 360F);
		}
	}

	@Override
	public ItemStack getItemBySlot(EquipmentSlot slotIn)
	{
		return ItemStack.EMPTY.copy();
	}

	@Override
	public void setItemSlot(EquipmentSlot slotIn, ItemStack stack)
	{

	}

	@Override
	public HumanoidArm getMainArm()
	{
		return HumanoidArm.RIGHT;
	}

}
