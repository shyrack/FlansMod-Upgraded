package com.flansmod.client.tmt;

import java.util.ArrayList;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.Vec3;

/**
 * A textured polygon. Vertices are emitted into a modern {@link VertexConsumer}
 * through the given {@link PoseStack} pose.
 */
public class TexturedPolygon
{
	public TexturedPolygon(PositionTextureVertex[] apositionTexturevertex)
	{
		this.invertNormal = false;
		this.vertexPositions = apositionTexturevertex;
		this.nVertices = apositionTexturevertex.length;
		this.iNormals = new ArrayList<>();
		this.normals = new float[0];
	}

	public TexturedPolygon(PositionTextureVertex[] apositionTexturevertex, int par2, int par3, int par4, int par5, float par6, float par7)
	{
		this(apositionTexturevertex);
		float var8 = 0.0F / par6;
		float var9 = 0.0F / par7;
		apositionTexturevertex[0] = apositionTexturevertex[0].setTexturePosition(par4 / par6 - var8, par3 / par7 + var9);
		apositionTexturevertex[1] = apositionTexturevertex[1].setTexturePosition(par2 / par6 + var8, par3 / par7 + var9);
		apositionTexturevertex[2] = apositionTexturevertex[2].setTexturePosition(par2 / par6 + var8, par5 / par7 - var9);
		apositionTexturevertex[3] = apositionTexturevertex[3].setTexturePosition(par4 / par6 - var8, par5 / par7 - var9);
	}

	public void setInvertNormal(boolean isSet)
	{
		invertNormal = isSet;
	}

	public void setNormals(float x, float y, float z)
	{
		normals = new float[]{x, y, z};
	}

	public void flipFace()
	{
		PositionTextureVertex[] var1 = new PositionTextureVertex[this.vertexPositions.length];

		for(int var2 = 0; var2 < this.vertexPositions.length; ++var2)
		{
			var1[var2] = this.vertexPositions[this.vertexPositions.length - var2 - 1];
		}

		this.vertexPositions = var1;
	}

	public void setNormals(ArrayList<Vec3> vec)
	{
		iNormals = vec;
	}

	public void draw(VertexConsumer consumer, PoseStack.Pose pose, float f, int light, int overlay)
	{
		if(nVertices < 3)
			return;

		float normalX;
		float normalY;
		float normalZ;
		if(normals.length == 3)
		{
			normalX = normals[0];
			normalY = normals[1];
			normalZ = normals[2];
			if(invertNormal)
			{
				normalX = -normalX;
				normalY = -normalY;
				normalZ = -normalZ;
			}
		}
		else
		{
			Vec3 vec1 = vertexPositions[1].vector3D.subtract(vertexPositions[0].vector3D);
			Vec3 vec2 = vertexPositions[1].vector3D.subtract(vertexPositions[2].vector3D);
			Vec3 normal = vec2.cross(vec1).normalize();
			normalX = (float)normal.x;
			normalY = (float)normal.y;
			normalZ = (float)normal.z;
			if(invertNormal)
			{
				normalX = -normalX;
				normalY = -normalY;
				normalZ = -normalZ;
			}
		}

		for(int i = 0; i < nVertices; i++)
		{
			PositionTextureVertex vertex = vertexPositions[i];
			if(vertex instanceof PositionTransformVertex)
				((PositionTransformVertex)vertex).setTransformation();
			float vx = (float)vertex.vector3D.x * f;
			float vy = (float)vertex.vector3D.y * f;
			float vz = (float)vertex.vector3D.z * f;
			float nx = normalX;
			float ny = normalY;
			float nz = normalZ;
			if(i < iNormals.size())
			{
				Vec3 in = iNormals.get(i);
				nx = (float)in.x;
				ny = (float)in.y;
				nz = (float)in.z;
				if(invertNormal)
				{
					nx = -nx;
					ny = -ny;
					nz = -nz;
				}
			}
			consumer.addVertex(pose, vx, vy, vz)
					.setColor(255, 255, 255, 255)
					.setUv(vertex.texturePositionX, vertex.texturePositionY)
					.setOverlay(overlay)
					.setLight(light)
					.setNormal(pose, nx, ny, nz);
		}
	}

	public void draw(TmtTessellator tessellator, float f)
	{
		draw(tessellator.getConsumer(), tessellator.getPose(), f, tessellator.getLight(), tessellator.getOverlay());
	}

	public PositionTextureVertex[] vertexPositions;
	public int nVertices;
	private boolean invertNormal;
	private float[] normals;
	private ArrayList<Vec3> iNormals;
}
