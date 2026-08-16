package com.flansmod.client.tmt;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.flansmod.client.model.ModelRenderer;

/**
 * Compatibility wrapper around the modern vertex pipeline, keeping the classic
 * TMT tessellation API. Vertices are accumulated and flushed into the current
 * {@link ModelRenderer.RenderContext} vertex consumer on {@link #draw()}.
 */
public class TmtTessellator
{
	public static TmtTessellator instance = new TmtTessellator(2097152);
	public boolean defaultTexture = false;
	public int textureID = 0;
	public int drawMode = 7;
	public double xOffset;
	public double yOffset;
	public double zOffset;
	public boolean isDrawing = false;

	private final int capacity;
	private final List<Vertex> vertices = new ArrayList<>();
	private boolean hasColor;
	private float colorR = 1F;
	private float colorG = 1F;
	private float colorB = 1F;
	private float colorA = 1F;
	private float normalX;
	private float normalY;
	private float normalZ;
	private boolean hasNormal;
	private float textureU;
	private float textureV;
	private float textureW = 1F;
	private boolean hasTexture;
	private int brightness;
	private boolean hasBrightness;

	private static class Vertex
	{
		float x;
		float y;
		float z;
		float u;
		float v;
		float w;
		float r;
		float g;
		float b;
		float a;
		float nx;
		float ny;
		float nz;
		boolean hasNormal;
		boolean hasTexture;
		int brightness;
		boolean hasBrightness;
	}

	public TmtTessellator(int capacity)
	{
		this.capacity = capacity;
	}

	public TmtTessellator()
	{
		this(2097152);
	}

	public VertexConsumer getConsumer()
	{
		ModelRenderer.RenderContext context = ModelRenderer.getRenderContext();
		return context == null ? null : context.consumer;
	}

	public PoseStack.Pose getPose()
	{
		ModelRenderer.RenderContext context = ModelRenderer.getRenderContext();
		return context == null ? null : context.poseStack.last();
	}

	public int getLight()
	{
		ModelRenderer.RenderContext context = ModelRenderer.getRenderContext();
		return context == null ? 0 : context.light;
	}

	public int getOverlay()
	{
		ModelRenderer.RenderContext context = ModelRenderer.getRenderContext();
		return context == null ? 0 : context.overlay;
	}

	public void startDrawingQuads()
	{
		this.startDrawing(7);
	}

	public void startDrawing(int par1)
	{
		if(this.isDrawing)
			throw new IllegalStateException("Already tesselating!");
		this.isDrawing = true;
		this.drawMode = par1;
		this.hasColor = false;
		this.hasNormal = false;
		this.hasTexture = false;
		this.hasBrightness = false;
		this.vertices.clear();
	}

	public void setTextureUV(double par1, double par3)
	{
		this.hasTexture = true;
		this.textureU = (float)par1;
		this.textureV = (float)par3;
		this.textureW = 1.0F;
	}

	public void setTextureUVW(double par1, double par3, double par4)
	{
		this.hasTexture = true;
		this.textureU = (float)par1;
		this.textureV = (float)par3;
		this.textureW = (float)par4;
	}

	public void setBrightness(int par1)
	{
		this.hasBrightness = true;
		this.brightness = par1;
	}

	public void setColorOpaque_F(float par1, float par2, float par3)
	{
		this.setColorRGBA((int)(par1 * 255F), (int)(par2 * 255F), (int)(par3 * 255F), 255);
	}

	public void setColorRGBA_F(float par1, float par2, float par3, float par4)
	{
		this.setColorRGBA((int)(par1 * 255F), (int)(par2 * 255F), (int)(par3 * 255F), (int)(par4 * 255F));
	}

	public void setColorOpaque(int par1, int par2, int par3)
	{
		this.setColorRGBA(par1, par2, par3, 255);
	}

	public void setColorRGBA(int par1, int par2, int par3, int par4)
	{
		this.hasColor = true;
		this.colorR = Math.max(0, Math.min(255, par1)) / 255F;
		this.colorG = Math.max(0, Math.min(255, par2)) / 255F;
		this.colorB = Math.max(0, Math.min(255, par3)) / 255F;
		this.colorA = Math.max(0, Math.min(255, par4)) / 255F;
	}

	public void setColorOpaque_I(int par1)
	{
		this.setColorOpaque(par1 >> 16 & 255, par1 >> 8 & 255, par1 & 255);
	}

	public void setColorRGBA_I(int par1, int par2)
	{
		this.setColorRGBA(par1 >> 16 & 255, par1 >> 8 & 255, par1 & 255, par2);
	}

	public void disableColor()
	{
		this.hasColor = false;
	}

	public void setNormal(float par1, float par2, float par3)
	{
		this.hasNormal = true;
		this.normalX = par1;
		this.normalY = par2;
		this.normalZ = par3;
	}

	public void setTranslation(double par1, double par3, double par5)
	{
		this.xOffset = par1;
		this.yOffset = par3;
		this.zOffset = par5;
	}

	public void addTranslation(float par1, float par2, float par3)
	{
		this.xOffset += par1;
		this.yOffset += par2;
		this.zOffset += par3;
	}

	public void addVertexWithUV(double par1, double par3, double par5, double par7, double par9)
	{
		this.setTextureUV(par7, par9);
		this.addVertex(par1, par3, par5);
	}

	public void addVertexWithUVW(double par1, double par3, double par5, double par7, double par9, double par10)
	{
		this.setTextureUVW(par7, par9, par10);
		this.addVertex(par1, par3, par5);
	}

	public void addVertex(double par1, double par3, double par5)
	{
		Vertex vertex = new Vertex();
		vertex.x = (float)(par1 + this.xOffset);
		vertex.y = (float)(par3 + this.yOffset);
		vertex.z = (float)(par5 + this.zOffset);
		vertex.hasTexture = this.hasTexture;
		vertex.u = this.textureU;
		vertex.v = this.textureV;
		vertex.w = this.textureW;
		vertex.hasNormal = this.hasNormal;
		vertex.nx = this.normalX;
		vertex.ny = this.normalY;
		vertex.nz = this.normalZ;
		vertex.hasBrightness = this.hasBrightness;
		vertex.brightness = this.brightness;
		if(this.hasColor)
		{
			vertex.r = this.colorR;
			vertex.g = this.colorG;
			vertex.b = this.colorB;
			vertex.a = this.colorA;
		}
		else
		{
			vertex.r = 1F;
			vertex.g = 1F;
			vertex.b = 1F;
			vertex.a = 1F;
		}
		vertices.add(vertex);
		if(vertices.size() >= capacity)
			draw();
	}

	public void draw()
	{
		if(!this.isDrawing)
			throw new IllegalStateException("Not tesselating!");
		this.isDrawing = false;
		VertexConsumer consumer = getConsumer();
		PoseStack.Pose pose = getPose();
		if(consumer != null && pose != null && !vertices.isEmpty())
		{
			int light = getLight();
			int overlay = getOverlay();
			for(Vertex vertex : vertices)
			{
				consumer.addVertex(pose, vertex.x, vertex.y, vertex.z)
						.setColor((int)(vertex.r * 255F), (int)(vertex.g * 255F), (int)(vertex.b * 255F), (int)(vertex.a * 255F))
						.setUv(vertex.u, vertex.v)
						.setOverlay(overlay)
						.setLight(light)
						.setNormal(pose, vertex.nx, vertex.ny, vertex.nz);
			}
		}
		this.vertices.clear();
		this.xOffset = 0;
		this.yOffset = 0;
		this.zOffset = 0;
	}
}
