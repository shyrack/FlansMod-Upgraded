package com.flansmod.client.util;

/**
 * Formerly a Forge key conflict context. Conflict contexts do not exist in
 * the Fabric port; all Flan's Mod keys use the same category instead.
 */
public final class FlansKeyConflictContext
{
	public static final FlansKeyConflictContext GUN = new FlansKeyConflictContext();
	public static final FlansKeyConflictContext VEHICLE = new FlansKeyConflictContext();

	private FlansKeyConflictContext()
	{
	}
}
