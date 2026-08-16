package com.flansmod.client.handlers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.types.InfoType;

public class FlansModResourceHandler
{
	private static HashMap<InfoType, Identifier> iconMap = new HashMap<>();
	private static HashMap<InfoType, Identifier> textureMap = new HashMap<>();
	private static HashMap<String, Identifier> trailTextureMap = new HashMap<>();
	private static HashMap<Paintjob, Identifier> paintjobMap = new HashMap<>();
	private static HashMap<Paintjob, Identifier> paintjobIconMap = new HashMap<>();
	private static HashMap<String, Identifier> scopeMap = new HashMap<>();
	private static HashMap<String, SoundEvent> soundMap = new HashMap<>();
	private static HashMap<String, Identifier> blockMap = new HashMap<>();
	
	public static Identifier flag = Identifier.fromNamespaceAndPath("flansmod", "textures/item/flagpole.png");
	public static Identifier[] opStick = new Identifier[]{Identifier.fromNamespaceAndPath("flansmod", "textures/item/opstick_ownership.png"),
			Identifier.fromNamespaceAndPath("flansmod", "textures/item/opstick_connecting.png"), Identifier.fromNamespaceAndPath("flansmod", "textures/item/opstick_mapping.png"),
			Identifier.fromNamespaceAndPath("flansmod", "textures/item/opstick_destruction.png")};
	
	public static Identifier getIcon(InfoType infoType)
	{
		if(iconMap.containsKey(infoType))
		{
			return iconMap.get(infoType);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "textures/item/" + infoType.iconPath.toLowerCase() + ".png");
		iconMap.put(infoType, resLoc);
		return resLoc;
	}
	
	public static Identifier getTexture(InfoType infoType)
	{
		if(textureMap.containsKey(infoType))
		{
			return textureMap.get(infoType);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "skins/" + infoType.texture.toLowerCase() + ".png");
		if(infoType.texture != null)
		{
			textureMap.put(infoType, resLoc);
			return resLoc;
		}
		else return null;
	}
	
	public static Identifier getDeployableTexture(GunType gunType)
	{
		if(textureMap.containsKey(gunType))
		{
			return textureMap.get(gunType);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "skins/" + gunType.deployableTexture.toLowerCase() + ".png");
		textureMap.put(gunType, resLoc);
		return resLoc;
	}
	
	public static Identifier getScope(String scopeTexture)
	{
		if(scopeMap.containsKey(scopeTexture))
		{
			return scopeMap.get(scopeTexture);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "gui/" + scopeTexture + ".png");
		scopeMap.put(scopeTexture, resLoc);
		return resLoc;
	}
	
	public static SoundEvent getSoundEvent(String sound)
	{
		String soundName = sound.toLowerCase();
		if(soundMap.containsKey(soundName))
		{
			return soundMap.get(soundName);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", soundName);
		SoundEvent event = SoundEvent.createVariableRangeEvent(resLoc);
		soundMap.put(soundName, event);
		return event;
	}
	
	public static Identifier getPaintjobTexture(Paintjob paintjob)
	{
		if(paintjobMap.containsKey(paintjob))
		{
			return paintjobMap.get(paintjob);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "skins/" + paintjob.textureName.toLowerCase() + ".png");
		paintjobMap.put(paintjob, resLoc);
		return resLoc;
	}
	
	public static Identifier getBlockTexture(String texturePath)
	{
		if(blockMap.containsKey(texturePath))
		{
			return blockMap.get(texturePath);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "blocks/" + texturePath.toLowerCase());
		blockMap.put(texturePath, resLoc);
		return resLoc;
	}
	
	public static Identifier getIcon(PaintableType paintableType, Paintjob paintjob)
	{
		if(paintjobIconMap.containsKey(paintjob))
		{
			return paintjobIconMap.get(paintjob);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "textures/items/" + paintjob.iconName + ".png");
		paintjobIconMap.put(paintjob, resLoc);
		return resLoc;
	}
	
	public static Identifier getTrailTexture(String trailTexture)
	{
		if(trailTextureMap.containsKey(trailTexture))
		{
			return trailTextureMap.get(trailTexture);
		}
		Identifier resLoc = Identifier.fromNamespaceAndPath("flansmod", "skins/" + trailTexture + ".png");
		trailTextureMap.put(trailTexture, resLoc);
		return resLoc;
	}
	
	private static HashMap<Integer, Identifier> customPaintjobSkins = new HashMap<>();
	private static HashMap<Integer, Identifier> customPaintjobIcons = new HashMap<>();
	private static final int BYTES_PER_PIXEL = 4;
	
	public static boolean HasResourceForHash(int customPaintHash)
	{
		return customPaintjobSkins.containsKey(customPaintHash) && customPaintjobIcons.containsKey(customPaintHash);
	}
	
	public static void CreateSkinResourceFromByteArray(byte[] byteArray, int textureWidth, int textureHeight, int customPaintHash)
	{
		String internalLocation = "skins/skin_" + customPaintHash + ".png";
		String fileLocation = "Flan/Customs/assets/flansmod/" + internalLocation;
		
		try
		{
			DataBuffer buffer = new DataBufferByte(byteArray, byteArray.length);
			
			WritableRaster raster = Raster.createInterleavedRaster(buffer, textureWidth, textureHeight, BYTES_PER_PIXEL * textureWidth, BYTES_PER_PIXEL, new int[]{0, 1, 2}, null);
			ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			BufferedImage image = new BufferedImage(cm, raster, true, null);
			File file = new File(fileLocation);
			
			if(!file.exists())
			{
				file.mkdirs();
				file.createNewFile();
			}
			ImageIO.write(image, "png", file);
			
			customPaintjobSkins.put(customPaintHash, Identifier.fromNamespaceAndPath("flansmod", internalLocation));
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to create custom skin!");
			return;
		}
	}
	
	public static void CreateIconResourceFromByteArray(byte[] byteArray, int textureWidth, int textureHeight, int customPaintHash)
	{
		String location = "customs/icon_" + customPaintHash + ".png";
		
		try
		{
			DataBuffer buffer = new DataBufferByte(byteArray, byteArray.length);
			
			WritableRaster raster = Raster.createInterleavedRaster(buffer, textureWidth, textureHeight, BYTES_PER_PIXEL * textureWidth, BYTES_PER_PIXEL, new int[]{0, 1, 2, 3}, null);
			ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			BufferedImage image = new BufferedImage(cm, raster, true, null);
			
			ImageIO.write(image, "png", new File(location));
			
			customPaintjobIcons.put(customPaintHash, Identifier.fromNamespaceAndPath("flansmod", location));
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to create custom icon!");
			return;
		}
	}
	
	public static Identifier GetSkinResourceFromHash(int customPaintHash)
	{
		return customPaintjobSkins.get(customPaintHash);
	}
	
	public static Identifier GetIconResourceFromHash(int customPaintHash)
	{
		return customPaintjobIcons.get(customPaintHash);
	}
}
