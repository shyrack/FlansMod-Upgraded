package com.shyrack.flansmodupgraded.common.guns;

public interface IScope
{
	float getFOVFactor();
	
	float getZoomFactor();
	
	boolean hasZoomOverlay();
	
	String getZoomOverlay();
}
