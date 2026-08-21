package com.flansmod.common.driveables;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.flansmod.common.FlansMod;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

import com.flansmod.common.parts.PartType;
import com.flansmod.common.types.EnumType;

/**
 * Headless in-game physics measurement harness.
 *
 * Enabled only with -Dflansmod.physicstest=true. Two modes:
 *
 *  - Client (singleplayer, recommended): boot with
 *      ./gradlew runPhysicsTest
 *    which launches the client with --quickPlaySingleplayer. The harness
 *    waits for the world, flattens a platform, places a real plane through
 *    the actual spawn path (ItemPlane equivalent: ground block + 2.5,
 *    resting pitch applied) on the integrated server, then records every
 *    client tick: BOTH the client-side plane and its integrated-server twin
 *    (positions, velocities, accelerations, orientation, wheels, part
 *    health). This shows exactly what the player sees AND what the server
 *    believes, including any client/server divergence.
 *
 *  - Dedicated server: boot with -Dflansmod.physicstest=true on the server;
 *    records the server-side plane only.
 *
 * The full record is written to physics-report.json in the run directory and
 * the game exits, so an agent (or CI) can drive a real Minecraft instance
 * and read back the exact physics.
 *
 * Properties:
 *   flansmod.physicstest.plane  plane short name (default "Biplane")
 *   flansmod.physicstest.ticks  number of ticks to record (default 600)
 */
public class PhysicsTestHarness
{
	private static final int WARMUP_TICKS = 20;
	private static final int RECORD_TICKS = Integer.getInteger("flansmod.physicstest.ticks", 600);
	private static final String PLANE_NAME = System.getProperty("flansmod.physicstest.plane", "Biplane");

	private static Minecraft mc;
	private static MinecraftServer server;
	private static ServerLevel world;
	private static EntityPlane placedPlane;
	private static int ticksSinceStart = 0;
	private static int recordTick = 0;
	private static final List<Map<String, Object>> records = new ArrayList<>();
	private static double spawnX, spawnY, spawnZ;
	private static double prevSVX, prevSVY, prevSVZ;
	private static double prevCVX, prevCVY, prevCVZ;

	public static void init()
	{
		FlansMod.log.info("[PhysicsTestHarness] enabled: plane={} ticks={}", PLANE_NAME, RECORD_TICKS);
		if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
		{
			ClientTickEvents.END_CLIENT_TICK.register(PhysicsTestHarness::onClientTick);
		}
		else
		{
			ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
			ServerTickEvents.END_SERVER_TICK.register(PhysicsTestHarness::onServerTick);
		}
	}

	// ---------------------------------------------------------------- client

	private static void onClientTick(Minecraft client)
	{
		mc = client;
		if(client.level == null || client.player == null)
			return;

		if(server == null)
		{
			// getSingleplayerServer() returns the client-only IntegratedServer
			// type, so it is reached reflectively to keep this class loadable
			// on a dedicated server
			try
			{
				Object result = Minecraft.class.getMethod("getSingleplayerServer").invoke(client);
				server = result instanceof MinecraftServer ? (MinecraftServer)result : null;
			}
			catch(ReflectiveOperationException e)
			{
				server = null;
			}
			if(server == null)
				return;
			world = server.overworld();
		}

		// Keep the demo/integrated server from kicking the idle player out
		// mid-measurement (demo mode logs out players that never act)
		net.minecraft.server.level.ServerPlayer serverPlayer =
				server.getPlayerList().getPlayer(client.player.getUUID());
		if(serverPlayer != null)
			serverPlayer.resetLastActionTime();

		if(placedPlane == null)
		{
			if(++ticksSinceStart < WARMUP_TICKS)
				return;
			placePlane();
			return;
		}

		if(recordTick < RECORD_TICKS)
		{
			recordClientTick(recordTick);
			recordTick++;
			if(recordTick % 20 == 0)
				FlansMod.log.info("[PhysicsTestHarness] recorded {} ticks", recordTick);
		}
		else
		{
			writeReport();
			FlansMod.log.info("[PhysicsTestHarness] report written, stopping the client");
			client.stop();
		}
	}

	// ----------------------------------------------------------------- server

	private static void onServerTick(MinecraftServer s)
	{
		if(server == null || s != server)
			return;

		if(world == null)
		{
			world = server.overworld();
			if(world == null)
				return;
		}

		if(placedPlane == null)
		{
			if(++ticksSinceStart < WARMUP_TICKS)
				return;
			placePlane();
			return;
		}

		if(recordTick < RECORD_TICKS)
		{
			recordServerTick(recordTick);
			recordTick++;
			if(recordTick % 20 == 0)
				FlansMod.log.info("[PhysicsTestHarness] recorded {} ticks", recordTick);
		}
		else
		{
			writeReport();
			FlansMod.log.info("[PhysicsTestHarness] report written, exiting");
			//System.exit runs shutdown hooks which can hang in the dev server;
			//halt terminates immediately after the report has been flushed
			new Thread(() ->
			{
				try
				{
					Thread.sleep(500);
				}
				catch(InterruptedException ignored)
				{
				}
				Runtime.getRuntime().halt(0);
			}).start();
		}
	}

	// -------------------------------------------------------------- placement

	private static void placePlane()
	{
		PlaneType type = PlaneType.getPlane(PLANE_NAME);
		if(type == null)
		{
			FlansMod.log.error("[PhysicsTestHarness] plane '{}' not found - is the pack loaded?", PLANE_NAME);
			System.exit(1);
			return;
		}

		// Fixed coordinates: chunk-forced and ground-scanned, so the spawn point
		// configuration never matters for the measurement. A 2x2 chunk area is
		// forced so the plane cannot slide out of the forced region and stop
		// ticking mid-measurement.
		int x = 8;
		int z = 8;
		world.getChunk(x >> 4, z >> 4);
		for(int cx = 0; cx < 2; cx++)
		{
			for(int cz = 0; cz < 2; cz++)
			{
				world.setChunkForced((x >> 4) + cx, (z >> 4) + cz, true);
			}
		}
		int ground = 319;
		while(ground > -63 && world.getBlockState(new BlockPos(x, ground, z)).isAir())
			ground--;

		for(int dx = -12; dx <= 12; dx++)
		{
			for(int dz = -12; dz <= 12; dz++)
			{
				for(int dy = ground + 1; dy <= ground + 8; dy++)
				{
					world.setBlock(new BlockPos(x + dx, dy, z + dz),
							Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}

		// Same placement as ItemPlane: resting height (lowest wheel at the
		// ground-contact line), yaw + 90, resting pitch applied, and the
		// spawned orientation is the previous orientation for the collision
		// sweep. The driveable data is built the same way ItemPlane does
		// (Type + default engine tag), so DriveableData.readFromNBT populates
		// every part.
		spawnX = x + 0.5D;
		double wheelClearance = 0D;
		for(DriveablePosition wheel : type.wheelPositions)
			wheelClearance = Math.max(wheelClearance, -wheel.position.y);
		if(wheelClearance <= 0D)
			wheelClearance = 0.9D; // skid-only planes rest on their own box
		spawnY = ground + 1.0D + 0.125D + wheelClearance;
		spawnZ = z + 0.5D;
		FlansMod.log.info("[PhysicsTestHarness] wheelPositions={} clearance={}",
				type.wheelPositions.length, wheelClearance);
		CompoundTag tags = new CompoundTag();
		tags.putString("Type", type.shortName);
		tags.putString("Engine", PartType.defaultEngines.get(EnumType.plane).shortName);
		DriveableData data = new DriveableData(tags);
		placedPlane = new EntityPlane(world, spawnX, spawnY, spawnZ, type, data);
		placedPlane.rotateYaw(90F);
		placedPlane.rotatePitch(type.restingPitch);
		placedPlane.prevAxes = placedPlane.axes.clone();
		placedPlane.xo = placedPlane.getX();
		placedPlane.yo = placedPlane.getY();
		placedPlane.zo = placedPlane.getZ();
		world.addFreshEntity(placedPlane);

		FlansMod.log.info("[PhysicsTestHarness] placed {} at ({}, {}, {}), ground={}, restingPitch={}",
				PLANE_NAME, spawnX, spawnY, spawnZ, ground, type.restingPitch);
	}

	// -------------------------------------------------------------- recording

	private static void recordClientTick(int t)
	{
		Entity clientPlane = mc.level.getEntity(placedPlane.getId());
		Entity serverPlane = world.getEntity(placedPlane.getId());
		record(t, clientPlane, serverPlane, true);
	}

	private static void recordServerTick(int t)
	{
		record(t, world.getEntity(placedPlane.getId()), world.getEntity(placedPlane.getId()), false);
	}

	private static void record(int t, Entity clientPlane, Entity serverPlane, boolean clientMode)
	{
		Map<String, Object> record = new LinkedHashMap<>();
		record.put("t", t);

		if(serverPlane == null)
		{
			record.put("dead", true);
			records.add(record);
			return;
		}

		// Client-side plane
		if(clientMode && clientPlane != null)
		{
			putEntityState(record, "c", clientPlane, clientPlane.getDeltaMovement(), true);
			double vx = clientPlane.getDeltaMovement().x;
			double vy = clientPlane.getDeltaMovement().y;
			double vz = clientPlane.getDeltaMovement().z;
			record.put("cax", (vx - prevCVX) * 20);
			record.put("cay", (vy - prevCVY) * 20);
			record.put("caz", (vz - prevCVZ) * 20);
			prevCVX = vx;
			prevCVY = vy;
			prevCVZ = vz;
		}

		// Server-side plane
		putEntityState(record, "s", serverPlane, serverPlane.getDeltaMovement(), false);
		double vx = serverPlane.getDeltaMovement().x;
		double vy = serverPlane.getDeltaMovement().y;
		double vz = serverPlane.getDeltaMovement().z;
		record.put("sax", (vx - prevSVX) * 20);
		record.put("say", (vy - prevSVY) * 20);
		record.put("saz", (vz - prevSVZ) * 20);
		prevSVX = vx;
		prevSVY = vy;
		prevSVZ = vz;

		// Server-authoritative wheels and parts
		if(serverPlane instanceof EntityPlane plane)
		{
			List<List<Double>> wheels = new ArrayList<>();
			List<Double> wheelBoxHeights = new ArrayList<>();
			List<Boolean> wheelOnGround = new ArrayList<>();
			List<Boolean> wheelVerticalCollision = new ArrayList<>();
			List<String> wheelBlockBelow = new ArrayList<>();
			for(EntityWheel wheel : plane.wheels)
			{
				if(wheel != null)
				{
					wheels.add(List.of(wheel.getX(), wheel.getY(), wheel.getZ()));
					wheelBoxHeights.add(wheel.getBoundingBox().getYsize());
					wheelOnGround.add(wheel.onGround());
					wheelVerticalCollision.add(wheel.verticalCollisionBelow);
					wheelBlockBelow.add(world.getBlockState(
							new BlockPos((int)Math.floor(wheel.getX()), (int)Math.floor(wheel.getY() - 0.4F), (int)Math.floor(wheel.getZ())))
							.getBlock().toString());
				}
			}
			record.put("wheels", wheels);
			record.put("wheelBoxHeights", wheelBoxHeights);
			record.put("wheelOnGround", wheelOnGround);
			record.put("wheelVerticalCollision", wheelVerticalCollision);
			record.put("wheelBlockBelow", wheelBlockBelow);
			record.put("planeBoxHeight", plane.getBoundingBox().getYsize());
			record.put("planeOnGround", plane.onGround());
			record.put("planeVerticalCollision", plane.verticalCollisionBelow);

			Map<String, Integer> parts = new LinkedHashMap<>();
			for(Map.Entry<EnumDriveablePart, DriveablePart> entry : plane.getDriveableData().parts.entrySet())
			{
				parts.put(entry.getKey().getShortName(), entry.getValue().health);
			}
			record.put("parts", parts);
		}

		records.add(record);

		if(t % 20 == 0 && clientMode)
		{
			FlansMod.log.info("[PhysicsTestHarness] t={} client=({}, {}, {}) v=({}, {}, {}) speed={} | server=({}, {}, {}) v=({}, {}, {}) speed={}",
					t,
					f2(clientPlane.getX()), f2(clientPlane.getY()), f2(clientPlane.getZ()),
					f3(clientPlane.getDeltaMovement().x), f3(clientPlane.getDeltaMovement().y), f3(clientPlane.getDeltaMovement().z),
					f3(clientPlane.getDeltaMovement().length()),
					f2(serverPlane.getX()), f2(serverPlane.getY()), f2(serverPlane.getZ()),
					f3(serverPlane.getDeltaMovement().x), f3(serverPlane.getDeltaMovement().y), f3(serverPlane.getDeltaMovement().z),
					f3(serverPlane.getDeltaMovement().length()));
		}
	}

	private static void putEntityState(Map<String, Object> record, String prefix, Entity entity,
									   net.minecraft.world.phys.Vec3 vel, boolean client)
	{
		record.put(prefix + "x", entity.getX());
		record.put(prefix + "y", entity.getY());
		record.put(prefix + "z", entity.getZ());
		record.put(prefix + "vx", vel.x);
		record.put(prefix + "vy", vel.y);
		record.put(prefix + "vz", vel.z);
		record.put(prefix + "speed", vel.length());
		if(entity instanceof EntityDriveable driveable)
		{
			record.put(prefix + "yaw", driveable.axes.getYaw());
			record.put(prefix + "pitch", driveable.axes.getPitch());
			record.put(prefix + "roll", driveable.axes.getRoll());
			record.put(prefix + "onGround", driveable.onGround());
			record.put(prefix + "throttle", driveable.throttle);
		}
	}

	private static String f2(double v)
	{
		return String.format("%.2f", v);
	}

	private static String f3(double v)
	{
		return String.format("%.3f", v);
	}

	private static void writeReport()
	{
		Map<String, Object> report = new LinkedHashMap<>();
		report.put("plane", PLANE_NAME);
		report.put("spawn", List.of(spawnX, spawnY, spawnZ));
		report.put("mode", FabricLoader.getInstance().getEnvironmentType().name());
		report.put("ticks", records.size());
		report.put("records", records);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try
		{
			Files.writeString(Path.of("physics-report.json"), gson.toJson(report), StandardCharsets.UTF_8);
			FlansMod.log.info("[PhysicsTestHarness] wrote physics-report.json ({} ticks)", records.size());
		}
		catch(Exception e)
		{
			FlansMod.log.error("[PhysicsTestHarness] failed to write report", e);
		}
	}
}
