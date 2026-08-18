package com.flansmod.common;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.flansmod.apocalypse.common.entity.EntityFakePlayer;
import com.flansmod.apocalypse.common.entity.EntityFlansModShooter;
import com.flansmod.apocalypse.common.entity.EntityFlyByPlane;
import com.flansmod.apocalypse.common.entity.EntityNukeDrop;
import com.flansmod.apocalypse.common.entity.EntitySkullBoss;
import com.flansmod.apocalypse.common.entity.EntitySkullDrone;
import com.flansmod.apocalypse.common.entity.EntitySkuller;
import com.flansmod.apocalypse.common.entity.EntitySurvivor;
import com.flansmod.apocalypse.common.entity.EntityTeleporter;
import com.flansmod.client.EntityCamera;
import com.flansmod.client.debug.EntityDebugAABB;
import com.flansmod.client.debug.EntityDebugDot;
import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.driveables.EntityWheel;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.guns.EntityAAGun;
import com.flansmod.common.guns.EntityBullet;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.guns.EntityMG;
import com.flansmod.common.teams.EntityFlag;
import com.flansmod.common.teams.EntityFlagpole;
import com.flansmod.common.teams.EntityGunItem;
import com.flansmod.common.EntityItemCustomRender;
import com.flansmod.common.teams.EntityTeamItem;
import com.flansmod.common.tools.EntityParachute;

public class ModEntities
{
	public static EntityType<EntityFlagpole> FLAGPOLE;
	public static EntityType<EntityFlag> FLAG;
	public static EntityType<EntityTeamItem> TEAMS_ITEM;
	public static EntityType<EntityGunItem> GUN_ITEM;
	public static EntityType<EntityItemCustomRender> CUSTOM_ITEM;
	public static EntityType<EntityPlane> PLANE;
	public static EntityType<EntityVehicle> VEHICLE;
	public static EntityType<EntitySeat> SEAT;
	public static EntityType<EntityWheel> WHEEL;
	public static EntityType<EntityParachute> PARACHUTE;
	public static EntityType<EntityMecha> MECHA;
	public static EntityType<EntityBullet> BULLET;
	public static EntityType<EntityGrenade> GRENADE;
	public static EntityType<EntityMG> MG;
	public static EntityType<EntityAAGun> AA_GUN;
	public static EntityType<EntityDebugVector> DEBUG_VECTOR;
	public static EntityType<EntityDebugDot> DEBUG_DOT;
	public static EntityType<EntityDebugAABB> DEBUG_AABB;
	public static EntityType<EntitySurvivor> SURVIVOR;
	public static EntityType<EntitySkuller> SKULLER;
	public static EntityType<EntitySkullDrone> SKULL_DRONE;
	public static EntityType<EntitySkullBoss> SKULL_BOSS;
	public static EntityType<EntityTeleporter> TELEPORTER;
	public static EntityType<EntityNukeDrop> NUKE_DROP;
	public static EntityType<EntityFlyByPlane> FLY_BY_PLANE;
	public static EntityType<EntityFlansModShooter> FLANSMOD_SHOOTER;
	public static EntityType<EntityFakePlayer> FAKE_PLAYER;
	public static EntityType<EntityCamera> CAMERA;

	public static void init()
	{
		FLAGPOLE = register("flagpole", EntityType.Builder.<EntityFlagpole>of(EntityFlagpole::new, MobCategory.MISC).sized(0.5F, 1.5F).clientTrackingRange(40).updateInterval(5));
		FLAG = register("flag", EntityType.Builder.<EntityFlag>of(EntityFlag::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(40).updateInterval(5));
		TEAMS_ITEM = register("teams_item", EntityType.Builder.<EntityTeamItem>of(EntityTeamItem::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(100).updateInterval(10000));
		GUN_ITEM = register("gun_item", EntityType.Builder.<EntityGunItem>of(EntityGunItem::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(100).updateInterval(20));
		CUSTOM_ITEM = register("custom_item", EntityType.Builder.<EntityItemCustomRender>of(EntityItemCustomRender::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(100).updateInterval(20));
		PLANE = register("plane", EntityType.Builder.<EntityPlane>of(EntityPlane::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(250).updateInterval(3));
		VEHICLE = register("vehicle", EntityType.Builder.<EntityVehicle>of(EntityVehicle::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(250).updateInterval(10));
		SEAT = register("seat", EntityType.Builder.<EntitySeat>of(EntitySeat::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(250).updateInterval(3));
		WHEEL = register("wheel", EntityType.Builder.<EntityWheel>of(EntityWheel::new, MobCategory.MISC).sized(0.5F, 1F).clientTrackingRange(250).updateInterval(20));
		PARACHUTE = register("parachute", EntityType.Builder.<EntityParachute>of(EntityParachute::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(40).updateInterval(20));
		MECHA = register("mecha", EntityType.Builder.<EntityMecha>of(EntityMecha::new, MobCategory.MISC).sized(2F, 3F).clientTrackingRange(250).updateInterval(20));
		BULLET = register("bullet", EntityType.Builder.<EntityBullet>of(EntityBullet::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(100).updateInterval(50));
		GRENADE = register("grenade", EntityType.Builder.<EntityGrenade>of(EntityGrenade::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(40).updateInterval(100));
		MG = register("mg", EntityType.Builder.<EntityMG>of(EntityMG::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(40).updateInterval(5));
		AA_GUN = register("aa_gun", EntityType.Builder.<EntityAAGun>of(EntityAAGun::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(40).updateInterval(500));
		DEBUG_VECTOR = register("debug_vector", EntityType.Builder.<EntityDebugVector>of(EntityDebugVector::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(250).updateInterval(20));
		DEBUG_DOT = register("debug_dot", EntityType.Builder.<EntityDebugDot>of(EntityDebugDot::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(250).updateInterval(20));
		DEBUG_AABB = register("debug_aabb", EntityType.Builder.<EntityDebugAABB>of(EntityDebugAABB::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(250).updateInterval(20));
		SURVIVOR = register("survivor", EntityType.Builder.<EntitySurvivor>of(EntitySurvivor::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(100).updateInterval(5));
		SKULLER = register("skuller", EntityType.Builder.<EntitySkuller>of(EntitySkuller::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(100).updateInterval(5));
		SKULL_DRONE = register("skull_drone", EntityType.Builder.<EntitySkullDrone>of(EntitySkullDrone::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(100).updateInterval(5));
		SKULL_BOSS = register("skull_boss", EntityType.Builder.<EntitySkullBoss>of(EntitySkullBoss::new, MobCategory.MISC).sized(4F, 4F).clientTrackingRange(250).updateInterval(5));
		TELEPORTER = register("teleporter", EntityType.Builder.<EntityTeleporter>of(EntityTeleporter::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(100).updateInterval(20));
		NUKE_DROP = register("nuke_drop", EntityType.Builder.<EntityNukeDrop>of(EntityNukeDrop::new, MobCategory.MISC).sized(0.5F, 1F).clientTrackingRange(250).updateInterval(20));
		FLY_BY_PLANE = register("fly_by_plane", EntityType.Builder.<EntityFlyByPlane>of(EntityFlyByPlane::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(250).updateInterval(3));
		FLANSMOD_SHOOTER = register("flansmod_shooter", EntityType.Builder.<EntityFlansModShooter>of(EntityFlansModShooter::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(100).updateInterval(5));
		FAKE_PLAYER = register("fake_player", EntityType.Builder.<EntityFakePlayer>of(EntityFakePlayer::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(100).updateInterval(5));
		CAMERA = register("camera", EntityType.Builder.<EntityCamera>of(EntityCamera::new, MobCategory.MISC).sized(0.1F, 0.1F));
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder)
	{
		Identifier id = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
		ResourceKey<EntityType<?>> key = ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), id);
		EntityType<T> type = builder.build(key);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
		return type;
	}
}
