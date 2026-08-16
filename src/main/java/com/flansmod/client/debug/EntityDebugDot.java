package com.flansmod.client.debug;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.common.ModEntities;
import com.flansmod.common.vector.Vector3f;

/**
 * Entity for debugging purposes
 * On the client side a dot, in the given color, at the location of the entity is rendered
 */
public class EntityDebugDot extends EntityDebugColor
{
	public int life = 1000;
	
	/**
	 * @param w Level for Entity Constructor
	 */
		public EntityDebugDot(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityDebugDot(Level w)
	{
		this(ModEntities.DEBUG_DOT, w);

	}
	
	/**
	 * Creates a white dot at the given location
	 *
	 * @param w   Level for Entity Constructor
	 * @param pos Position of the dot
	 * @param l   Lifetime given in ticks
	 */
	public EntityDebugDot(Level w, Vector3f pos, int l)
	{
		this(w, pos, l, 1F, 1F, 1F);
	}
	
	/**
	 * Creates a dot
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @param w   Level for Entity Constructor
	 * @param pos Position of the dot
	 * @param l   Lifetime given in ticks
	 * @param r   Red color value
	 * @param g   Green color value
	 * @param b   Blue color value
	 */
	public EntityDebugDot(Level w, Vector3f pos, int l, float r, float g, float b)
	{
		this(w);
		setPos(pos.x, pos.y, pos.z);
		setColor(r, g, b);
		life = l;
	}
	
	@Override
	public void tick()
	{
		super.tick();
		life--;
		if(life <= 0)
			discard();
	}
}
