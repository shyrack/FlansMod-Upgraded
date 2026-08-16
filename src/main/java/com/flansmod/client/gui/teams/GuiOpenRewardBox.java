package com.flansmod.client.gui.teams;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModuloHelper;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.teams.RewardBox;

public class GuiOpenRewardBox extends GuiTeamsBase
{
	private enum EnumPageState
	{
		SPINNING,
		READY_TO_SLOW_DOWN,
		SLOWING_DOWN,
		STOPPED,
	}
	
	/**
	 * The background image
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/opencrates.png");
	private static final int WIDTH = 196, HEIGHT = 200;
	private static final int WAITING_FOR_SERVER = -1;
	private static int spinTime = 30, slowdownTime = 130;
	private static long timeOfLastSound = 0;
	private static Random gunScrambler = new Random();
	private float spinSpeed = 0.555555555f;
	
	private ArrayList<Paintjob> options = new ArrayList<>();
	private int target = WAITING_FOR_SERVER;
	private EnumPageState state = EnumPageState.SPINNING;
	private int timeLeftInState = spinTime;
	private float spinner = 0.0f;
	private Button doneButton;
	
	public void SetTarget(Paintjob paint)
	{
		for(int i = 0; i < options.size(); i++)
		{
			if(options.get(i) == paint)
			{
				target = i;
				return;
			}
		}
		FlansMod.Assert(false, "Could not find paintjob we just unlocked!");
	}
	
	@Override
	public void init()
	{
		super.init();
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), b -> ClientTeamsData.OpenLandingPage()).bounds(width / 2 - 20, guiOriginY + 170, 40, 20).build());
		doneButton.active = false;
	}
	
	public GuiOpenRewardBox(RewardBox rewardBox)
	{
		super();
		state = EnumPageState.SPINNING;
		timeLeftInState = spinTime;
		target = WAITING_FOR_SERVER;
		
		ArrayList<Paintjob> temp = new ArrayList<>(rewardBox.paintjobs);
		
		int size = rewardBox.paintjobs.size();
		for(int i = 0; i < size; i++)
		{
			int random = gunScrambler.nextInt(size - i);
			options.add(temp.get(random));
			temp.remove(random);
		}
		
		spinSpeed = InitialVelocity();
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		timeLeftInState--;
		
		switch(state)
		{
			case SPINNING:
			{
				SimulateSpinner();
				// Make sure we have our target from the server before trying to spin down on to it
				if(timeLeftInState <= 0 && target != WAITING_FOR_SERVER)
				{
					SwitchToState(EnumPageState.READY_TO_SLOW_DOWN);
					timeLeftInState = slowdownTime;
				}
				break;
			}
			case READY_TO_SLOW_DOWN:
			{
				SimulateSpinner();
				float difference = Mth.abs(spinner - target);
				if(difference < 1.0f)
				{
					// We're here (ish). Fix the position and then spin round one last time, slowing down as we go.
					spinner = target;
					timeLeftInState = slowdownTime;
					SwitchToState(EnumPageState.SLOWING_DOWN);
				}
				break;
			}
			case SLOWING_DOWN:
			{
				spinSpeed += Acceleration();
				if(spinSpeed <= -Acceleration())
				{
					spinSpeed = 0.0f;
					Minecraft.getInstance().getSoundManager().play(
							new SimpleSoundInstance(FlansModResourceHandler.getSoundEvent("unlocknotch"), SoundSource.NEUTRAL, 1.0F, 2.0f,
									Minecraft.getInstance().player.getRandom(), Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getY(), Minecraft.getInstance().player.getZ()));
					SwitchToState(EnumPageState.STOPPED);
				}
				int timeInState = slowdownTime - timeLeftInState;
				int preIndex = Mth.floor(spinner) % options.size();
				spinner = target + timeInState * InitialVelocity() + 0.5f * Acceleration() * timeInState * timeInState;
				int postIndex = Mth.floor(spinner) % options.size();
				
				
				break;
				
			}
			case STOPPED:
			{
				spinner = target;
				doneButton.active = true;
				break;
			}
			
			default:
				break;
		}
	}
	
	private void SimulateSpinner()
	{
		int preIndex = Mth.floor(spinner) % options.size();
		spinner += spinSpeed;
		int postIndex = Mth.floor(spinner) % options.size();
		
		if(spinner > options.size())
		{
			spinner -= options.size();
		}
	}
	
	private float InitialVelocity()
	{
		return (2.0f / (float)slowdownTime) * options.size();
	}
	
	private float Acceleration()
	{
		return -(InitialVelocity() * InitialVelocity()) / (2 * options.size());
	}
	
	private void SwitchToState(EnumPageState newState)
	{
		state = newState;
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		int preIndex = Mth.floor(spinner) % options.size();
		int postIndex = Mth.floor(spinner + spinSpeed * partialTick) % options.size();
		
		if(preIndex != postIndex && Util.getMillis() - timeOfLastSound >= 80)
		{
			Minecraft.getInstance().getSoundManager().play(
					new SimpleSoundInstance(FlansModResourceHandler.getSoundEvent("unlocknotch"), SoundSource.NEUTRAL, 0.5F, 1.0f,
							Minecraft.getInstance().player.getRandom(), Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getY(), Minecraft.getInstance().player.getZ()));
			timeOfLastSound = Util.getMillis();
		}
		
		
		extractMenuBackground(extractor);
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		int textureX = 512;
		int textureY = 256;
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		if(data == null || pool == null)
		{
			FlansMod.log.warn("Problem in landing page!");
			return;
		}
		
		//Draw the background
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY, 0F, 0F, WIDTH, HEIGHT, textureX, textureY);
		
		int pixelOffset = ModuloHelper.modulo(Mth.floor(spinner * 18.0f), 18) - 18;
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 9, guiOriginY + 101, 239 + pixelOffset + 10, 101, 180, 18, textureX, textureY);
		
		// Draw text
		extractor.centeredText(font, "Reward Box", guiOriginX + 98, guiOriginY + 12, 0xffffff);
		
		for(int n = 0; n < 10; n++)
		{
			int index = Mth.floor(spinner) - 4 + n;
			Paintjob paintjob = options.get(ModuloHelper.modulo(index, options.size()));
			
			ItemStack stack = new ItemStack(paintjob.parent.getItem());
			stack.setDamageValue(paintjob.ID);
			drawSlotInventory(extractor, stack, guiOriginX + 18 - 18 - pixelOffset + 18 * n, guiOriginY + 102);
		}
		
		for(int n = 0; n < 10; n++)
		{
			int index = Mth.floor(spinner) - 4 + n;
			Paintjob paintjob = options.get(ModuloHelper.modulo(index, options.size()));
			
			DrawRarityBackground(extractor, paintjob.rarity, guiOriginX + 18 - 18 - pixelOffset + 18 * n, guiOriginY + 102);
		}
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 0, guiOriginY + 93, 0, 93, 196, 34, textureX, textureY);
		
		int currentIndex = Mth.floor(spinner) % options.size();
		ItemStack gunStack = new ItemStack(options.get(currentIndex).parent.item);
		gunStack.setDamageValue(options.get(currentIndex).ID);
		DrawGun(extractor, gunStack, guiOriginX + 98, guiOriginY + 65, 60.0f);
		
		if(state == EnumPageState.STOPPED)
		{
			extractor.centeredText(font, "New paintjob unlocked!", guiOriginX + 98, guiOriginY + 130, 0xffffff);
			extractor.centeredText(font, options.get(target).parent.name, guiOriginX + 98, guiOriginY + 142, 0xffffff);
			extractor.centeredText(font, "\"" + options.get(target).iconName + "\"", guiOriginX + 98, guiOriginY + 154, 0xffffff);
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
