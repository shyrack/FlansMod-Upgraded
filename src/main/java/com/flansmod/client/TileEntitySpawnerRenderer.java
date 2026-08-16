package com.flansmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.BlockSpawner;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TileEntitySpawner;

public class TileEntitySpawnerRenderer implements BlockEntityRenderer<TileEntitySpawner, TileEntitySpawnerRenderer.State>
{
	public static class State extends BlockEntityRenderState
	{
		public int teamID;
		public String map = "";
		public int type;
	}

	@Override
	public State createRenderState()
	{
		return new State();
	}

	@Override
	public void extractRenderState(TileEntitySpawner te, State state, float partialTick, Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay)
	{
		state.teamID = te.getTeamID();
		state.map = te.map;
		state.type = te.getBlockState().getValue(BlockSpawner.TYPE);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera)
	{
		int spawnerTeamID = state.teamID;
		Team spawnerTeam = FlansModClient.getTeam(spawnerTeamID);

		boolean currentMap = FlansModClient.isCurrentMap(state.map);

		float red = 1F, green = 1F, blue = 1F;

		//Use default colours
		if(spawnerTeam == null || !currentMap)
		{
			switch(spawnerTeamID)
			{
				case 0: red = 0.5f; green = 0.5f; blue = 0.5f;
					break; //No team : light grey
				case 1: red = 0.25f; green = 0.25f; blue = 0.25f;
					break; //Spectators : dark grey
				case 2: red = 0.8f; green = 0.5f; blue = 1.0f;
					break; //Team 1 : purple
				case 3: red = 1.0f; green = 0.5f; blue = 0.8f;
					break; //Team 2 : pink
			}
		}
		else
		{
			red = (float)((spawnerTeam.teamColour >> 16) & 0xff) / 255f;
			green = (float)((spawnerTeam.teamColour >> 8) & 0xff) / 255f;
			blue = (float)((spawnerTeam.teamColour >> 0) & 0xff) / 255f;
		}

		double inset = 0.0d;
		switch(state.type)
		{
			case 0: inset = 0.375d;
				break;
			case 1: inset = 0.25d;
				break;
			case 2: inset = 0.0625d;
				break;
			default: FlansMod.log.warn("" + state.type);
		}

		final float colR = red;
		final float colG = green;
		final float colB = blue;
		final double inset0 = inset;

		collector.submitCustomGeometry(pose, RenderTypes.lines(), (p, consumer) ->
		{
			RenderBox(p, consumer, (float)inset0, (float)(1.0d - inset0), 0.0625F, 0.125F, (float)inset0, (float)(1.0d - inset0), colR, colG, colB);
		});
	}

	private void RenderBox(PoseStack.Pose p, VertexConsumer consumer, float x0, float x1, float y0, float y1, float z0, float z1, float red, float green, float blue)
	{
		line(p, consumer, x0, y0, z0, x0, y0, z1, red, green, blue);
		line(p, consumer, x0, y0, z1, x1, y0, z1, red, green, blue);
		line(p, consumer, x1, y0, z1, x1, y0, z0, red, green, blue);
		line(p, consumer, x1, y0, z0, x0, y0, z0, red, green, blue);
		line(p, consumer, x0, y1, z0, x0, y1, z1, red, green, blue);
		line(p, consumer, x0, y1, z1, x1, y1, z1, red, green, blue);
		line(p, consumer, x1, y1, z1, x1, y1, z0, red, green, blue);
		line(p, consumer, x1, y1, z0, x0, y1, z0, red, green, blue);
		line(p, consumer, x0, y0, z0, x0, y1, z0, red, green, blue);
		line(p, consumer, x1, y0, z0, x1, y1, z0, red, green, blue);
		line(p, consumer, x1, y0, z1, x1, y1, z1, red, green, blue);
		line(p, consumer, x0, y0, z1, x0, y1, z1, red, green, blue);
	}

	private void line(PoseStack.Pose p, VertexConsumer consumer, float x0, float y0, float z0, float x1, float y1, float z1, float red, float green, float blue)
	{
		consumer.addVertex(p, x0, y0, z0).setColor(red, green, blue, 1.0F);
		consumer.addVertex(p, x1, y1, z1).setColor(red, green, blue, 1.0F);
	}
}
