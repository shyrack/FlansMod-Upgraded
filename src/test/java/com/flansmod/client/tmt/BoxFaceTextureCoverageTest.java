package com.flansmod.client.tmt;

import java.lang.reflect.Field;

import com.flansmod.client.model.ModelBase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the canonical TMT box face UV layout. Every box built by
 * {@link ModelRendererTurbo} (via {@code addBox}, {@code addShapeBox} or
 * {@code addTrapezoid}) must map its six faces onto the UV regions documented
 * for Turbo Model Thingy:
 *
 * <pre>
 *   #  face        vertices         u range                     v range
 *   0  right (+X)  v5, v1, v2, v6   u0+d+w .. u0+d+w+d          v0+d .. v0+d+h
 *   1  left  (-X)  v,  v4, v7, v3   u0 .. u0+d                  v0+d .. v0+d+h
 *   2  top   (+Y)  v5, v4, v,  v1   u0+d .. u0+d+w              v0 .. v0+d
 *   3  bottom(-Y)  v2, v3, v7, v6   u0+d+w .. u0+d+w+w          v0 .. v0+d
 *   4  front (-Z)  v1, v,  v3, v2   u0+d .. u0+d+w              v0+d .. v0+d+h
 *   5  back  (+Z)  v4, v5, v6, v7   u0+d+w+d .. u0+d+w+d+w      v0+d .. v0+d+h
 * </pre>
 *
 * The quads are inset by {@code 1/(10*textureWidth)} / {@code 1/(10*textureHeight)}
 * on every side to avoid bleeding, and {@code mirror ^ flip} reverses each face
 * with {@link TexturedPolygon#flipFace()}.
 *
 * Shape construction must not trigger any Minecraft bootstrap, so the private
 * {@code faces} array is read through reflection, like the other plain unit
 * tests in this module.
 */
class BoxFaceTextureCoverageTest
{
	private static final String[] FACE_NAMES = {"right", "left", "top", "bottom", "front", "back"};
	private static final float POSITION_DELTA = 1e-4F;

	@Test
	void addBoxCoversAllSixFacesWithCanonicalUvs() throws Exception
	{
		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), 0, 0, 64, 32);
		renderer.addBox(0, 0, 0, 4, 8, 2);
		assertBoxFaces(renderer, 64, 32, 0F, 4F, 0F, 8F, 0F, 2F, 4, 8, 2, 0, 0, false);
	}

	@Test
	void addBoxRespectsTextureOffset() throws Exception
	{
		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), 0, 0, 64, 32);
		renderer.setTextureOffset(16, 8).addBox(0, 0, 0, 4, 8, 2);
		assertBoxFaces(renderer, 64, 32, 0F, 4F, 0F, 8F, 0F, 2F, 4, 8, 2, 16, 8, false);
	}

	@Test
	void addBoxOnLargeTextureCoversAllSixFaces() throws Exception
	{
		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), 0, 0, 256, 128);
		renderer.addBox(0, 0, 0, 4, 8, 2);
		assertBoxFaces(renderer, 256, 128, 0F, 4F, 0F, 8F, 0F, 2F, 4, 8, 2, 0, 0, false);
	}

	@Test
	void addShapeBoxUsesCanonicalFaceLayout() throws Exception
	{
		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), 0, 0, 64, 32);
		renderer.addShapeBox(0, 0, 0, 4, 8, 2, 0.5F,
				0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F,
				0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F);
		assertBoxFaces(renderer, 64, 32, -0.5F, 4.5F, -0.5F, 8.5F, -0.5F, 2.5F, 4, 8, 2, 0, 0, false);
	}

	@Test
	void addTrapezoidUsesCanonicalFaceLayout() throws Exception
	{
		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), 0, 0, 64, 32);
		renderer.addTrapezoid(0, 0, 0, 4, 8, 2, 0.5F, 0F, ModelRendererTurbo.MR_TOP);
		assertBoxFaces(renderer, 64, 32, -0.5F, 4.5F, -0.5F, 8.5F, -0.5F, 2.5F, 4, 8, 2, 0, 0, false);
	}

	@Test
	void addBoxMirroredFlipsFaceVertexOrder() throws Exception
	{
		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), 0, 0, 64, 32);
		renderer.mirror = true;
		renderer.addBox(0, 0, 0, 4, 8, 2);
		assertBoxFaces(renderer, 64, 32, 4F, 0F, 0F, 8F, 0F, 2F, 4, 8, 2, 0, 0, true);
	}

	@Test
	void addShape3DCapsAndSidesUseDocumentedUvRegions() throws Exception
	{
		int textureW = 64;
		int textureH = 32;
		int u = 8;
		int v = 4;
		int shapeTextureWidth = 4;
		int shapeTextureHeight = 4;
		int sideTextureWidth = 16;
		int sideTextureHeight = 8;
		float depth = 2F;
		float x = 2F;
		float y = 3F;
		float z = 4F;
		Coord2D[] coords = {new Coord2D(0, 0), new Coord2D(4, 0), new Coord2D(4, 4), new Coord2D(0, 4)};

		ModelRendererTurbo renderer = new ModelRendererTurbo(new ModelBase(), u, v, textureW, textureH);
		renderer.addShape3D(x, y, z, coords, depth, shapeTextureWidth, shapeTextureHeight,
				sideTextureWidth, sideTextureHeight, ModelRendererTurbo.MR_BACK);

		TexturedPolygon[] faces = faces(renderer);
		assertEquals(coords.length + 2, faces.length,
				"a shape3D must add one side face per coordinate plus two caps");

		float deltaU = 1.0F / (textureW * 10.0F) + 1e-4F;
		float deltaV = 1.0F / (textureH * 10.0F) + 1e-4F;

		TexturedPolygon topCap = faces[coords.length];
		TexturedPolygon bottomCap = faces[coords.length + 1];
		for(int idx = 0; idx < coords.length; idx++)
		{
			Coord2D coord = coords[idx];
			PositionTextureVertex topVertex = topCap.vertexPositions[idx];
			assertEquals(x + (float)coord.xCoord, (float)topVertex.vector3D.x, POSITION_DELTA, "top cap vertex " + idx + " x");
			assertEquals(y + (float)coord.yCoord, (float)topVertex.vector3D.y, POSITION_DELTA, "top cap vertex " + idx + " y");
			assertEquals(z, (float)topVertex.vector3D.z, POSITION_DELTA, "top cap vertex " + idx + " z");
			assertEquals((coord.uCoord + u) / (float)textureW, topVertex.texturePositionX, deltaU, "top cap vertex " + idx + " u");
			assertEquals((coord.vCoord + v) / (float)textureH, topVertex.texturePositionY, deltaV, "top cap vertex " + idx + " v");

			int bottomCoord = coords.length - idx - 1;
			Coord2D bottomSource = coords[bottomCoord];
			PositionTextureVertex bottomVertex = bottomCap.vertexPositions[idx];
			assertEquals(x + (float)bottomSource.xCoord, (float)bottomVertex.vector3D.x, POSITION_DELTA, "bottom cap vertex " + idx + " x");
			assertEquals(y + (float)bottomSource.yCoord, (float)bottomVertex.vector3D.y, POSITION_DELTA, "bottom cap vertex " + idx + " y");
			assertEquals(z - depth, (float)bottomVertex.vector3D.z, POSITION_DELTA, "bottom cap vertex " + idx + " z");
			assertEquals((shapeTextureWidth * 2 - bottomSource.uCoord + u) / (float)textureW, bottomVertex.texturePositionX, deltaU, "bottom cap vertex " + idx + " u");
			assertEquals((bottomSource.vCoord + v) / (float)textureH, bottomVertex.texturePositionY, deltaV, "bottom cap vertex " + idx + " v");
		}

		float totalLength = 0F;
		for(int idx = 0; idx < coords.length; idx++)
		{
			totalLength += edgeLength(coords, idx);
		}

		float currentLengthPosition = totalLength;
		for(int idx = 0; idx < coords.length; idx++)
		{
			int nextIdx = (idx + 1) % coords.length;
			float currentLength = edgeLength(coords, idx);
			float ratioPosition = currentLengthPosition / totalLength;
			float ratioLength = (currentLengthPosition - currentLength) / totalLength;
			currentLengthPosition -= currentLength;

			float texU1 = (ratioLength * sideTextureWidth + u) / textureW;
			float texU2 = (ratioPosition * sideTextureWidth + u) / textureW;
			float texV1 = (v + shapeTextureHeight) / (float)textureH;
			float texV2 = (v + shapeTextureHeight + sideTextureHeight) / (float)textureH;

			TexturedPolygon side = faces[idx];
			assertEquals(4, side.vertexPositions.length, "side face " + idx + " must be a quad");

			assertSideVertex(side, 0, x + (float)coords[idx].xCoord, y + (float)coords[idx].yCoord, z, texU2, texV1, idx, deltaU, deltaV);
			assertSideVertex(side, 1, x + (float)coords[idx].xCoord, y + (float)coords[idx].yCoord, z - depth, texU2, texV2, idx, deltaU, deltaV);
			assertSideVertex(side, 2, x + (float)coords[nextIdx].xCoord, y + (float)coords[nextIdx].yCoord, z - depth, texU1, texV2, idx, deltaU, deltaV);
			assertSideVertex(side, 3, x + (float)coords[nextIdx].xCoord, y + (float)coords[nextIdx].yCoord, z, texU1, texV1, idx, deltaU, deltaV);
		}
	}

	private static void assertSideVertex(TexturedPolygon side, int vertex, float expectedX, float expectedY, float expectedZ,
			float expectedU, float expectedV, int face, float deltaU, float deltaV)
	{
		String name = "side face " + face + " vertex " + vertex;
		PositionTextureVertex actual = side.vertexPositions[vertex];
		assertEquals(expectedX, (float)actual.vector3D.x, POSITION_DELTA, name + " x");
		assertEquals(expectedY, (float)actual.vector3D.y, POSITION_DELTA, name + " y");
		assertEquals(expectedZ, (float)actual.vector3D.z, POSITION_DELTA, name + " z");
		assertEquals(expectedU, actual.texturePositionX, deltaU, name + " u");
		assertEquals(expectedV, actual.texturePositionY, deltaV, name + " v");
	}

	private static float edgeLength(Coord2D[] coords, int idx)
	{
		Coord2D current = coords[idx];
		Coord2D next = coords[(idx + 1) % coords.length];
		return (float)Math.sqrt(Math.pow(current.xCoord - next.xCoord, 2) + Math.pow(current.yCoord - next.yCoord, 2));
	}

	/**
	 * Asserts the six faces of a box-shaped part against the canonical TMT UV
	 * layout. {@code xV}/{@code xV1} are the x coordinates of the corners named
	 * {@code v}/{@code v1} after any mirror swap; {@code flipped} is
	 * {@code mirror ^ flip}, i.e. whether {@code flipFace()} reversed the faces.
	 */
	private static void assertBoxFaces(ModelRendererTurbo renderer, int textureW, int textureH,
			float xV, float xV1, float yLow, float yHigh, float zLow, float zHigh,
			int w, int h, int d, int u0, int v0, boolean flipped) throws Exception
	{
		TexturedPolygon[] faces = faces(renderer);
		assertEquals(6, faces.length, "a box must add exactly six faces");

		float[][] vertexPositions = {
				{xV1, yLow, zHigh}, {xV1, yLow, zLow}, {xV1, yHigh, zLow}, {xV1, yHigh, zHigh},
				{xV, yLow, zLow}, {xV, yLow, zHigh}, {xV, yHigh, zHigh}, {xV, yHigh, zLow},
				{xV1, yLow, zHigh}, {xV, yLow, zHigh}, {xV, yLow, zLow}, {xV1, yLow, zLow},
				{xV1, yHigh, zLow}, {xV, yHigh, zLow}, {xV, yHigh, zHigh}, {xV1, yHigh, zHigh},
				{xV1, yLow, zLow}, {xV, yLow, zLow}, {xV, yHigh, zLow}, {xV1, yHigh, zLow},
				{xV, yLow, zHigh}, {xV1, yLow, zHigh}, {xV1, yHigh, zHigh}, {xV, yHigh, zHigh}
		};
		int[][] uvRegions = {
				{u0 + d + w, v0 + d, u0 + d + w + d, v0 + d + h},
				{u0, v0 + d, u0 + d, v0 + d + h},
				{u0 + d, v0, u0 + d + w, v0 + d},
				{u0 + d + w, v0, u0 + d + w + w, v0 + d},
				{u0 + d, v0 + d, u0 + d + w, v0 + d + h},
				{u0 + d + w + d, v0 + d, u0 + d + w + d + w, v0 + d + h}
		};

		float deltaU = 1.0F / (textureW * 10.0F) + 1e-4F;
		float deltaV = 1.0F / (textureH * 10.0F) + 1e-4F;

		for(int face = 0; face < 6; face++)
		{
			String name = FACE_NAMES[face] + " face (poly[" + face + "])";
			TexturedPolygon polygon = faces[face];
			assertNotNull(polygon, name + " must exist");
			assertEquals(4, polygon.vertexPositions.length, name + " must be a quad");

			int u1 = uvRegions[face][0];
			int v1 = uvRegions[face][1];
			int u2 = uvRegions[face][2];
			int v2 = uvRegions[face][3];

			float[] uOrder = {uvHigh(u2, textureW), uvLow(u1, textureW), uvLow(u1, textureW), uvHigh(u2, textureW)};
			float[] vOrder = {uvLow(v1, textureH), uvLow(v1, textureH), uvHigh(v2, textureH), uvHigh(v2, textureH)};

			for(int vertex = 0; vertex < 4; vertex++)
			{
				int source = flipped ? 3 - vertex : vertex;
				PositionTextureVertex actual = polygon.vertexPositions[vertex];
				float[] expectedPosition = vertexPositions[face * 4 + source];
				assertEquals(expectedPosition[0], (float)actual.vector3D.x, POSITION_DELTA, name + " vertex " + vertex + " x");
				assertEquals(expectedPosition[1], (float)actual.vector3D.y, POSITION_DELTA, name + " vertex " + vertex + " y");
				assertEquals(expectedPosition[2], (float)actual.vector3D.z, POSITION_DELTA, name + " vertex " + vertex + " z");
				assertEquals(uOrder[source], actual.texturePositionX, deltaU, name + " vertex " + vertex + " u");
				assertEquals(vOrder[source], actual.texturePositionY, deltaV, name + " vertex " + vertex + " v");
			}
		}
	}

	private static float uvLow(int coordinate, int textureSize)
	{
		return coordinate / (float)textureSize + 1.0F / (textureSize * 10.0F);
	}

	private static float uvHigh(int coordinate, int textureSize)
	{
		return coordinate / (float)textureSize - 1.0F / (textureSize * 10.0F);
	}

	private static TexturedPolygon[] faces(ModelRendererTurbo renderer) throws Exception
	{
		Field field = ModelRendererTurbo.class.getDeclaredField("faces");
		field.setAccessible(true);
		return (TexturedPolygon[])field.get(renderer);
	}
}
