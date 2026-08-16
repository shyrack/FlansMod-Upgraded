package com.flansmod.client.tmt;

import java.util.ArrayList;

/**
 * Groups polygons that share a texture. In the modern render pipeline the
 * whole model is drawn through a single vertex consumer, so texture switching
 * per group is not performed; the group structure is kept for data
 * compatibility with content pack models.
 */
public class TextureGroup
{
	public TextureGroup()
	{
		poly = new ArrayList<>();
		texture = "";
	}
	
	public void addPoly(TexturedPolygon polygon)
	{
		poly.add(polygon);
	}
	
	public void loadTexture()
	{
	}
	
	public void loadTexture(int defaultTexture)
	{
	}
	
	public ArrayList<TexturedPolygon> poly;
	public String texture;
}
