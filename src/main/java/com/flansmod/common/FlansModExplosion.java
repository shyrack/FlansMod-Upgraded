package com.flansmod.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.flansmod.common.guns.EntityDamageSourceFlan;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.types.InfoType;

public class FlansModExplosion implements Explosion
{
	
	private final boolean causesFire;
	private final boolean breaksBlocks;
	private final Random random;
	private final Level world;
	private final double x, y, z;
	private final Optional<? extends Player> player;
	private final Entity explosive;
	private final float size;
	private final List<BlockPos> affectedBlockPositions;
	private final Map<Player, Vec3> playerKnockbackMap;
	private final Vec3 position;
	private final InfoType type; // type of Flan's Mod weapon causing explosion
	private final ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator();
	
	public FlansModExplosion(Level world, Entity entity, Optional<? extends Player> player, InfoType type, double x, double y, double z, float size, boolean causesFire, boolean smoking, boolean breaksBlocks)
	{
		this.random = new Random();
		this.affectedBlockPositions = Lists.newArrayList();
		this.playerKnockbackMap = Maps.newHashMap();
		this.world = world;
		this.player = player;
		this.size = size;
		this.x = x;
		this.y = y;
		this.z = z;
		this.causesFire = causesFire;
		this.breaksBlocks = breaksBlocks && TeamsManager.explosions;
		this.position = new Vec3(this.x, this.y, this.z);
		this.type = type;
		this.explosive = entity;
		
		this.doExplosionA();
		this.doExplosionB(smoking);
	}
	
	@Override
	public ServerLevel level()
	{
		return (ServerLevel)this.world;
	}
	
	@Override
	public Explosion.BlockInteraction getBlockInteraction()
	{
		return Explosion.BlockInteraction.DESTROY;
	}
	
	@Override
	public LivingEntity getIndirectSourceEntity()
	{
		return player.isPresent() ? player.get() : (explosive instanceof LivingEntity ? (LivingEntity)explosive : null);
	}
	
	@Override
	public Entity getDirectSourceEntity()
	{
		return explosive;
	}
	
	@Override
	public float radius()
	{
		return this.size;
	}
	
	@Override
	public Vec3 center()
	{
		return this.position;
	}
	
	@Override
	public boolean canTriggerBlocks()
	{
		return false;
	}
	
	@Override
	public boolean shouldAffectBlocklikeEntities()
	{
		return true;
	}
	
	/**
	 * Does the first part of the explosion (destroy blocks)
	 */
	public void doExplosionA()
	{
		Set<BlockPos> set = Sets.newHashSet();
		
		if(breaksBlocks)
		{
			for(int j = 0; j < 16; ++j)
			{
				for(int k = 0; k < 16; ++k)
				{
					for(int l = 0; l < 16; ++l)
					{
						if(j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15)
						{
							double d0 = (double)((float)j / 15.0F * 2.0F - 1.0F);
							double d1 = (double)((float)k / 15.0F * 2.0F - 1.0F);
							double d2 = (double)((float)l / 15.0F * 2.0F - 1.0F);
							double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
							d0 /= d3;
							d1 /= d3;
							d2 /= d3;
							float f = this.size * (0.7F + this.world.getRandom().nextFloat() * 0.6F);
							double d4 = this.x;
							double d6 = this.y;
							double d8 = this.z;
							
							for(; f > 0.0F; f -= 0.22500001F)
							{
								BlockPos blockpos = new BlockPos((int)d4, (int)d6, (int)d8);
								BlockState iblockstate = this.world.getBlockState(blockpos);
								
								if(!iblockstate.isAir())
								{
									float f2 = this.damageCalculator.getBlockExplosionResistance(this, this.world, blockpos, iblockstate, this.world.getFluidState(blockpos)).orElse(0.0F);
									f -= (f2 + 0.3F) * 0.3F;
								}
								
								if(f > 0.0F)
								{
									set.add(blockpos);
								}
								
								d4 += d0 * 0.30000001192092896D;
								d6 += d1 * 0.30000001192092896D;
								d8 += d2 * 0.30000001192092896D;
							}
						}
					}
				}
			}
		}
		
		this.affectedBlockPositions.addAll(set);
		float f3 = this.size * 2.0F;
		int k1 = Mth.floor(this.x - (double)f3 - 1.0D);
		int l1 = Mth.floor(this.x + (double)f3 + 1.0D);
		int i2 = Mth.floor(this.y - (double)f3 - 1.0D);
		int i1 = Mth.floor(this.y + (double)f3 + 1.0D);
		int j2 = Mth.floor(this.z - (double)f3 - 1.0D);
		int j1 = Mth.floor(this.z + (double)f3 + 1.0D);
		List<Entity> list = this.world.getEntities(this.explosive, new AABB((double)k1, (double)i2, (double)j2, (double)l1, (double)i1, (double)j1));
		Vec3 vec3d = new Vec3(this.x, this.y, this.z);
		
		for(Entity entity : list)
		{
			if(!entity.isRemoved())
			{
				double d12 = entity.distanceToSqr(this.x, this.y, this.z) / (double)(f3 * f3);
				d12 = Math.sqrt(d12);
				
				if(d12 <= 1.0D)
				{
					double d5 = entity.getX() - this.x;
					double d7 = entity.getY() + (double)entity.getEyeHeight() - this.y;
					double d9 = entity.getZ() - this.z;
					double d13 = (double)Mth.sqrt((float)(d5 * d5 + d7 * d7 + d9 * d9));
					
					if(d13 != 0.0D)
					{
						d5 /= d13;
						d7 /= d13;
						d9 /= d13;
						double d10 = 1.0D - d12;
						if(player.isPresent())
						{
							this.hurtEntity(entity, new EntityDamageSourceFlan(type.shortName, explosive, player.get(), type).setExplosion(),
									(float)((int)((d10 * d10 + d10) / 2.0D * 7.0D * (double)f3 + 1.0D)));
						} else {
							this.hurtEntity(entity, this.world.damageSources().explosion(explosive, explosive),
									(float)((int)((d10 * d10 + d10) / 2.0D * 7.0D * (double)f3 + 1.0D)));
						}
						double d11 = d10;
						
						entity.setDeltaMovement(entity.getDeltaMovement().add(d5 * d11, d7 * d11, d9 * d11));
						
						if(entity instanceof Player)
						{
							Player entityplayer = (Player)entity;
							
							if(!entityplayer.isSpectator() && (!entityplayer.isCreative() || !entityplayer.getAbilities().flying))
							{
								this.playerKnockbackMap.put(entityplayer, new Vec3(d5 * d10, d7 * d10, d9 * d10));
							}
						}
					}
				}
			}
		}
	}
	
	private void hurtEntity(Entity entity, DamageSource source, float amount)
	{
		if(this.world instanceof ServerLevel)
			entity.hurtServer((ServerLevel)this.world, source, amount);
		else
			entity.hurtClient(source);
	}
	
	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	public void doExplosionB(boolean spawnParticles)
	{
		this.world.playSound(null, this.x, this.y, this.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.world.getRandom().nextFloat() - this.world.getRandom().nextFloat()) * 0.2F) * 0.7F);
		
		if(this.size >= 2.0F && this.breaksBlocks)
		{
			this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		}
		else
		{
			this.world.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
		}
		
		if(this.breaksBlocks)
		{
			for(BlockPos blockpos : this.affectedBlockPositions)
			{
				BlockState iblockstate = this.world.getBlockState(blockpos);
				Block block = iblockstate.getBlock();
				
				if(spawnParticles)
				{
					double d0 = (double)((float)blockpos.getX() + this.world.getRandom().nextFloat());
					double d1 = (double)((float)blockpos.getY() + this.world.getRandom().nextFloat());
					double d2 = (double)((float)blockpos.getZ() + this.world.getRandom().nextFloat());
					double d3 = d0 - this.x;
					double d4 = d1 - this.y;
					double d5 = d2 - this.z;
					double d6 = (double)Mth.sqrt((float)(d3 * d3 + d4 * d4 + d5 * d5));
					d3 /= d6;
					d4 /= d6;
					d5 /= d6;
					double d7 = 0.5D / (d6 / (double)this.size + 0.1D);
					d7 *= (double)(this.world.getRandom().nextFloat() * this.world.getRandom().nextFloat() + 0.3F);
					d3 *= d7;
					d4 *= d7;
					d5 *= d7;
					this.world.addParticle(ParticleTypes.EXPLOSION, (d0 + this.x) / 2.0D, (d1 + this.y) / 2.0D, (d2 + this.z) / 2.0D, d3, d4, d5);
					this.world.addParticle(ParticleTypes.LARGE_SMOKE, d0, d1, d2, d3, d4, d5);
				}
				
				if(!iblockstate.isAir())
				{
					if(block.dropFromExplosion(this) && this.world.getRandom().nextFloat() < 1.0F / this.size)
					{
						Block.dropResources(iblockstate, this.world, blockpos);
					}
					
					this.world.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		
		if(this.causesFire)
		{
			for(BlockPos blockpos1 : this.affectedBlockPositions)
			{
				if(this.world.getBlockState(blockpos1).isAir() && this.world.getBlockState(blockpos1.below()).isSolid() && this.random.nextInt(3) == 0)
				{
					this.world.setBlock(blockpos1, Blocks.FIRE.defaultBlockState(), 3);
				}
			}
		}
	}
	
	public Map<Player, Vec3> getPlayerKnockbackMap()
	{
		return this.playerKnockbackMap;
	}
	
	public void clearAffectedBlockPositions()
	{
		this.affectedBlockPositions.clear();
	}
	
	public List<BlockPos> getAffectedBlockPositions()
	{
		return this.affectedBlockPositions;
	}
	
	public Vec3 getPosition()
	{
		return this.position;
	}
}
