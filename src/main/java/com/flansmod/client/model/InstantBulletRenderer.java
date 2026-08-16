package com.flansmod.client.model;

import java.util.ArrayList;

import net.minecraft.resources.Identifier;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.vector.Vector3f;

public class InstantBulletRenderer
{
	private static ArrayList<InstantShotTrail> trails = new ArrayList<>();
	
	public static void AddTrail(InstantShotTrail trail)
	{
		trails.add(trail);
	}
	
	/**
	 * TODO: [26.1.2] trails need a world render hook that no longer exists in the new renderer.
	 * Kept as a no-op so trail bookkeeping still works.
	 */
	public static void RenderAllTrails(float partialTicks)
	{
	}
	
	public static void UpdateAllTrails()
	{
		for(int i = trails.size() - 1; i >= 0; i--)
		{
			if(trails.get(i).Update())
			{
				trails.remove(i);
			}
		}
	}
	
	public static class InstantShotTrail
	{
		private Vector3f origin;
		private Vector3f hitPos;
		private float width;
		private float length;
		private float distanceToTarget;
		private float bulletSpeed;
		private int ticksExisted;
		
		private Identifier texture;
		
		public InstantShotTrail(Vector3f origin, Vector3f hitPos, float width, float length, float bulletSpeed, String trailTexture)
		{
			this.origin = origin;
			this.hitPos = hitPos;
			this.width = width;
			this.length = length;
			this.bulletSpeed = bulletSpeed;
			
			this.ticksExisted = 0;
			this.texture = FlansModResourceHandler.getTrailTexture(trailTexture);
			
			Vector3f dPos = Vector3f.sub(hitPos, origin, null);
			this.distanceToTarget = dPos.length();
			
			if(Math.abs(distanceToTarget) > 300.0f)
			{
				distanceToTarget = 300.0f;
			}
		}
		
		// Return true if this needs deleting
		public boolean Update()
		{
			ticksExisted++;
			return (ticksExisted) * bulletSpeed >= distanceToTarget - length;
		}
		
		public void EntityRenderer(float partialTicks)
		{
			// TODO: [26.1.2] trails need a world render hook that no longer exists in the new renderer
		}
	}
}
