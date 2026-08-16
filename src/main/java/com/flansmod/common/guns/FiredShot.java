package com.flansmod.common.guns;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/**
 * Class for creating object containing all necessary informations about a fired shot
 */
public class FiredShot {
	protected Level world;

	/**
	 * The weapon used to fire the shot
	 */
	private FireableGun weapon;
	/**
	 * The BulletType of the fired bullet
	 */
	private BulletType bullet;
	/**
	 * Optional containing a player, if one can be associated with the shot
	 */
	private Optional<ServerPlayer> player;
	/**
	 * Optional of the entity which fired the shot. Can be the same as the Player optional
	 */
	private Optional<? extends Entity> shooter;
	
	/**
	 * @param weapon weapon used to fire the shot
	 * @param bullet BulletType of the fired bullet
	 */
	public FiredShot(FireableGun weapon, BulletType bullet)
	{
		this.weapon = weapon;
		this.bullet = bullet;
		this.player = Optional.empty();
		this.shooter = this.player;
	}
	
	/**
	 * @param weapon weapon used to fire the shot
	 * @param bullet BulletType of the fired bullet
	 * @param player The player who shot
	 */
	public FiredShot(FireableGun weapon, BulletType bullet, ServerPlayer player)
	{
		this(weapon,bullet, player, player);
	}
	
	/**
	 * This constructor should be used when an entity shot, but no player is involved
	 * e.g a zombie holding a gun or a sentry
	 * 
	 * @param weapon weapon used to fire the shot
	 * @param bullet BulletType of the fired bullet
	 * @param shooter Entity which fired the shot
	 */
	public FiredShot(FireableGun weapon, BulletType bullet, Entity shooter)
	{
		this(weapon, bullet, shooter, null);
	}
	
	/**
	 * This constructor should be used if a player causes a shot, but the player is actually not the entity shooting it
	 * e.g a player flying a plane
	 * 
	 * @param weapon  weapon used to fire the shot
	 * @param bullet  BulletType of the fired bullet
	 * @param shooter the Entity firing the shot
	 * @param player  the Player causing the shot
	 */
	public FiredShot(FireableGun weapon, BulletType bullet, Entity shooter, ServerPlayer player)
	{
		this.weapon = weapon;
		this.bullet = bullet;
		this.player = Optional.ofNullable(player);
		this.shooter = Optional.of(shooter);
	}
	
	/**
	 * @return The gun used for this shot
	 */
	public FireableGun getFireableGun()
	{
		return this.weapon;
	}
	
	/**
	 * @return The BulletType of the bullet used in the shot
	 */
	public BulletType getBulletType()
	{
		return this.bullet;
	}
	
	/**
	 * @return the matching DamageSource for the shot
	 */
	public DamageSource getDamageSource()
	{
		return getDamageSource(false);
	}

	/**
	 * @return the matching DamageSource for the shot with the additional 'headshot' information
	 */
	public DamageSource getDamageSource(Boolean headshot)
	{
		if (player.isPresent()) {
			return new EntityDamageSourceFlan(weapon.getShortName(), player.get(), player.get(), weapon.getInfoType(), headshot).setProjectile();
		}
		else if(shooter.isPresent())
		{
			return new EntityDamageSourceFlan(weapon.getShortName(), null, null, weapon.getInfoType(), headshot).setProjectile();
		}
		return new DamageSource(Holder.direct(new DamageType("generic", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F)));
	}
	
	/**
	 * @return Optional containing a player if one is involved in the cause of the shot
	 */
	public Optional<ServerPlayer> getPlayerOptional()
	{
		return this.player;
	}
	
	/**
	 * @return Optional containing the Entity which shot. Might be the same as the player optional
	 */
	public Optional<? extends Entity> getShooterOptional()
	{
		return this.shooter;
	}
}
