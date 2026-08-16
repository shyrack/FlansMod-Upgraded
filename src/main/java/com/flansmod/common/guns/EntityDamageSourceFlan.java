package com.flansmod.common.guns;

import com.flansmod.common.PlayerHandler;
import com.flansmod.common.types.InfoType;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class EntityDamageSourceFlan extends DamageSource{
	
	private InfoType weapon;
	private Player shooter;
	private boolean headshot;
	/**
	 * @param s        Name of the damage source (Usually the shortName of the gun)
	 * @param entity   The Entity causing the damage (e.g. Grenade). Can be the same as 'player'
	 * @param player   The Player responsible for the damage
	 * @param wep      The InfoType of weapon used
	 */	
	public EntityDamageSourceFlan(String s, Entity entity, Player player, InfoType wep)
	{
		this(s, entity, player, wep, false);
	}
	
	/**
	 * @param s        Name of the damage source (Usually the shortName of the gun)
	 * @param entity   The Entity causing the damage (e.g. Grenade). Can be the same as 'player'
	 * @param player   The Player responsible for the damage
	 * @param wep      The InfoType of weapon used
	 * @param headshot True if this was a headshot, false if not
	 */
	public EntityDamageSourceFlan(String s, Entity entity, Player player, InfoType wep, boolean headshot)
	{
		super(makeType(s), entity, player);
		weapon = wep;
		shooter = player;
		this.headshot = headshot;
	}
	
	private static Holder<DamageType> makeType(String s)
	{
		return Holder.direct(new DamageType(s, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
	}
	
	/**
	 * The projectile flag used to be set here. Modern damage types use tags for
	 * this, so this is kept for API compatibility.
	 */
	public EntityDamageSourceFlan setProjectile()
	{
		return this;
	}
	
	/**
	 * The explosion flag used to be set here. Modern damage types use tags for
	 * this, so this is kept for API compatibility.
	 */
	public EntityDamageSourceFlan setExplosion()
	{
		return this;
	}
	
	public Component getDeathMessage(LivingEntity living)
	{
		if(!(living instanceof Player) || shooter == null || PlayerHandler.getPlayerData(shooter) == null)
		{
			if(shooter == null)
			{
				return Component.literal(living.getName() + " was shot");
			}
			else return Component.literal(living.getName() + " was shot by " + shooter.getName());
		}

		return Component.literal("#flansmod");
	}
	
	/**
	 * @return The weapon (InfoType) used to cause this damage
	 */
	public InfoType getWeapon()
	{
		return weapon;
	}
	
	/**
	 * @return The Player responsible for this damage
	 */
	public Player getCausedPlayer()
	{
		return shooter;
	}
	
	/**
	 * @return True if this is a headshot, false if not
	 */
	public boolean isHeadshot()
	{
		return headshot;
	}
	
	public Vec3 getDamageLocation()
	{
		if(getDirectEntity() == null)
			return new Vec3(0d, 0d, 0d);
		return getDirectEntity().position();
	}
}
