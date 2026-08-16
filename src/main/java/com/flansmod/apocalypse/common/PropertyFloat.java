package com.flansmod.apocalypse.common;

public class PropertyFloat
{
	
	protected final String name;
	protected final float minValue, maxValue;
	
	public PropertyFloat(String name)
	{
		this(name, Float.MAX_VALUE, Float.MIN_VALUE);
	}
	
	public PropertyFloat(String name, float minValue, float maxValue)
	{
		this.name = name;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean isValid(Float value)
	{
		return minValue > maxValue ? true : (value >= minValue && value <= maxValue);
	}
	
	public Class<Float> getType()
	{
		return Float.class;
	}
	
	public String valueToString(Float value)
	{
		return value.toString();
	}
	
}
