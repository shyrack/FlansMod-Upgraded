package com.flansmod.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class WorldRenderer
{
	private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(1024);
	private BufferBuilder bufferBuilder;

	public WorldRenderer()
	{
	}

	public void startDrawingQuads()
	{
		bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
	}

	public void addVertexWithUV(double x, double y, double z, double u, double v)
	{
		bufferBuilder.addVertex((float)x, (float)y, (float)z).setUv((float)u, (float)v);
	}

	public void draw()
	{
		bufferBuilder.build();
		byteBufferBuilder.clear();
		bufferBuilder = null;
	}
}
