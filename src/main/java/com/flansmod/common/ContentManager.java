package com.flansmod.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.driveables.mechas.ItemMecha;
import com.flansmod.common.driveables.mechas.ItemMechaAddon;
import com.flansmod.common.driveables.mechas.MechaItemType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.enchantments.GloveType;
import com.flansmod.common.enchantments.ItemGlove;
import com.flansmod.common.guns.AAGunType;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.GrenadeType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemAAGun;
import com.flansmod.common.guns.ItemAttachment;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGrenade;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.ArmourType;
import com.flansmod.common.teams.BlockArmourBox;
import com.flansmod.common.teams.ItemRewardBox;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.teams.RewardBox;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.tools.ToolType;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ContentManager 
{
	public class ContentPackFlanFolder implements IFlansModContentProvider
	{
		public ContentPackFlanFolder(String n, File f) { folder = f; name = n; }
		public String name;
		public File folder;
		
		@Override
		public String GetContentFolder() 
		{
			return name;
		}
		
		@Override
		public void RegisterModelRedirects() 
		{
			try
			{
				if(folder.isDirectory())
				{
					File redirectInfo = new File(folder, "/redirect.info");
					if(redirectInfo.exists())
					{
						BufferedReader reader = new BufferedReader(new FileReader(redirectInfo));
						String src = reader.readLine();
						String dst = reader.readLine();
						
						if(src != null && dst != null)
						{
							FlansMod.log.info("Registered Flan folder model redirect from " + src + " to " + dst);
							FlansMod.RegisterModelRedirect(src, dst);
						}
						
						reader.close();
					}
				}
				else if(zipJar.matcher(folder.getName()).matches())
				{
					ZipFile zip = new ZipFile(folder);
					ZipEntry entry = zip.getEntry("redirect.info");
					
					if(entry != null && !entry.isDirectory())
					{
						BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
						String src = reader.readLine();
						String dst = reader.readLine();
						
						if(src != null && dst != null)
						{
							FlansMod.log.info("Registered Flan folder model redirect from " + src + " to " + dst);
							FlansMod.RegisterModelRedirect(src, dst);
						}
						
						reader.close();
					}
					
					zip.close();
				}
			}
			catch(Exception e)
			{
				
			}
		}
	}
	
	private HashMap<String, IFlansModContentProvider> packs = new HashMap<String, IFlansModContentProvider>();
	protected Pattern zipJar = Pattern.compile("(.+)\\.(zip|jar)$");
	private boolean wasAnythingInFlanFolder = false;
	
	public boolean LoadedAnyContentFromFlanFolder()
	{
		return wasAnythingInFlanFolder;
	}
	
	public void FindContentInFlanFolder()
	{
		for(File file : FlansMod.flanDir.listFiles())
		{
			//Load folders and valid zip files
			if(file.isDirectory() || zipJar.matcher(file.getName()).matches())
			{
				//Add the directory to the content pack list
				if(packs.containsKey(file.getName()))
				{
					FlansMod.log.info("Skipping loading content pack from Flan folder as it is duplicated: " + file.getName());
				}
				else
				{
					FlansMod.log.info("Loaded content pack from Flan folder : " + file.getName());
					packs.put(file.getName(), new ContentPackFlanFolder(file.getName(), file));
					wasAnythingInFlanFolder = true;
				}
			}
		}
		FlansMod.log.info("Loaded content pack list from Flan folder");
	}
	
	
	public void LoadAssetsFromFlanFolder()
	{
		FlansMod.proxy.LoadAssetsFromFlanFolder();
	}
	
	public void RegisterModelRedirects()
	{
		for(IFlansModContentProvider provider : packs.values())
			provider.RegisterModelRedirects();
	}
	
	public void FindContentInModsFolder()
	{
		// Search for content packs in the mods folder
		if(FlansMod.modDir == null || !FlansMod.modDir.exists())
			return;
		for(File file : FlansMod.modDir.listFiles())
		{
			if(!zipJar.matcher(file.getName()).matches())
				continue;
			try
			{
				ZipFile zip = new ZipFile(file);
				boolean isContentPack = false;
				for(ZipEntry entry : java.util.Collections.list(zip.entries()))
				{
					if(entry.isDirectory())
						continue;
					for(EnumType type : EnumType.values())
					{
						if(entry.getName().startsWith(type.folderName + "/"))
						{
							isContentPack = true;
							break;
						}
					}
					if(isContentPack)
						break;
				}
				zip.close();
				if(isContentPack && !packs.containsKey(file.getName()))
				{
					FlansMod.log.info("Found .jar content pack " + file.getName() + " in mods folder. Loading from jar");
					packs.put(file.getName(), new ContentPackFlanFolder(file.getName(), file));
				}
			}
			catch(Exception e)
			{
				FlansMod.log.error("Failed to inspect " + file.getName() + " in mods folder for content pack data", e);
			}
		}
	}
	
	private static java.util.Collection<File> listTypeFiles(File typesDir)
	{
		java.util.List<File> files = new ArrayList<>();
		java.io.File[] children = typesDir.listFiles();
		if(children != null)
		{
			for(File child : children)
			{
				if(child.isDirectory())
					files.addAll(listTypeFiles(child));
				else if(child.getName().toLowerCase().endsWith(".txt"))
					files.add(child);
			}
		}
		return files;
	}

	private void LoadTypesFromDirectory(String contentPackName, File contentPack)
	{
		for(EnumType typeToCheckFor : EnumType.values())
		{
			File typesDir = new File(contentPack, "/" + typeToCheckFor.folderName + "/");
			if(!typesDir.exists())
				continue;
			for(File file : listTypeFiles(typesDir))
			{
				if(!file.isDirectory())
				{
					try
					{
						BufferedReader reader = new BufferedReader(new FileReader(file));
						String[] splitName = file.getName().split("/");
						TypeFile typeFile = new TypeFile(contentPackName, typeToCheckFor, splitName[splitName.length - 1].split("\\.")[0]);
						for(; ; )
						{
							String line = null;
							try
							{
								line = reader.readLine();
							}
							catch(Exception e)
							{
								break;
							}
							if(line == null)
								break;
							typeFile.parseLine(line);
						}
						reader.close();
					}
					catch(IOException e)
					{
						FlansMod.log.error("Failed to read type file " + file.getName(), e);
					}
				}
			}
		}
	}
	
	private void LoadTypesFromArchive(String contentPackName, File contentPack)
	{
		try
		{
			ZipFile zip = new ZipFile(contentPack);
			ZipInputStream zipStream = new ZipInputStream(new FileInputStream(contentPack));
			BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream));
			ZipEntry zipEntry = zipStream.getNextEntry();
			do
			{
				zipEntry = zipStream.getNextEntry();
				if(zipEntry == null)
					continue;
				if(zipEntry.isDirectory())
					continue;
				TypeFile typeFile = null;
				for(EnumType type : EnumType.values())
				{
					if(zipEntry.getName().startsWith(type.folderName + "/") && zipEntry.getName().split(type.folderName + "/").length > 1 && zipEntry.getName().split(type.folderName + "/")[1].length() > 0)
					{
						String[] splitName = zipEntry.getName().split("/");
						typeFile = new TypeFile(zip.getName(), type, splitName[splitName.length - 1].split("\\.")[0]);
					}
				}
				if(typeFile == null)
				{
					continue;
				}
				for(; ; )
				{
					String line = null;
					try
					{
						line = reader.readLine();
					}
					catch(Exception e)
					{
						break;
					}
					if(line == null)
						break;
					typeFile.parseLine(line);
				}
			}
			while(zipEntry != null);
			reader.close();
			zip.close();
			zipStream.close();
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to load type files from archive " + contentPack.getName(), e);
		}
	}
	
	public void LoadTypes()
	{
		for(HashMap.Entry<String, IFlansModContentProvider> entry : packs.entrySet())
		{
			String contentPackName = entry.getKey();
			IFlansModContentProvider provider = entry.getValue();
			
			if(provider instanceof ContentPackFlanFolder)
			{
				ContentPackFlanFolder contentPack = (ContentPackFlanFolder)provider;
				if(contentPack.folder.isDirectory())
				{
					LoadTypesFromDirectory(contentPackName, contentPack.folder);
				}
				else // Let's hope its a zip / jar
				{
					LoadTypesFromArchive(contentPackName, contentPack.folder);
				}
			}
		}
	}
	
	public void CreateItems()
	{
		java.util.Set<String> seenNames = new java.util.HashSet<>();
		for(EnumType type : EnumType.values())
		{
			Class<? extends InfoType> typeClass = type.getTypeClass();
			for(TypeFile typeFile : TypeFile.files.get(type))
			{
				try
				{
					InfoType infoType = (typeClass.getConstructor(TypeFile.class).newInstance(typeFile));
					infoType.read(typeFile);
					if(!seenNames.add(infoType.shortName.toLowerCase()))
					{
						FlansMod.log.warn("Skipping duplicate type " + infoType.shortName + " in " + typeFile.name);
						continue;
					}
					switch(type)
					{
						case bullet:
						{
							Item item = new ItemBullet((BulletType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanGuns.addItem(item);
							break;
						}
						case attachment:
						{
							Item item = new ItemAttachment((AttachmentType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanGuns.addItem(item);
							break;
						}
						case gun:
						{
							Item item = new ItemGun((GunType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanGuns.addItem(item);
							break;
						}
						case grenade:
						{
							Item item = new ItemGrenade((GrenadeType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanGuns.addItem(item);
							break;
						}
						case part:
						{
							ItemPart item = new ItemPart((PartType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.partItems.add(item);
							FlansMod.tabFlanParts.addItem(item);
							break;
						}
						case plane:
						{
							Item item = new ItemPlane((PlaneType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanDriveables.addItem(item);
							break;
						}
						case vehicle:
						{
							Item item = new ItemVehicle((VehicleType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanDriveables.addItem(item);
							break;
						}
						case aa:
						{
							Item item = new ItemAAGun((AAGunType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanGuns.addItem(item);
							break;
						}
						case mechaItem:
						{
							Item item = new ItemMechaAddon((MechaItemType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanMechas.addItem(item);
							break;
						}
						case mecha:
						{
							ItemMecha item = new ItemMecha((MechaType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.mechaItems.add(item);
							FlansMod.tabFlanMechas.addItem(item);
							break;
						}
						case tool:
						{
							ItemTool item = new ItemTool((ToolType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.toolItems.add(item);
							FlansMod.tabFlanParts.addItem(item);
							break;
						}
						case box:
						{
							GunBoxType gunBoxType = (GunBoxType)infoType;
							BlockGunBox block = ModBlocks.registerBlock(nameOf(infoType),
									p -> new BlockGunBox(p.mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2F, 4F)
											.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK), gunBoxType));
							ModItems.registerTypeItem(ModItems.blockItem(block), infoType);
							FlansMod.tabFlanGuns.addItem(block);
							break;
						}
						case armour:
						{
							ItemTeamArmour item = new ItemTeamArmour((ArmourType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.armourItems.add(item);
							FlansMod.tabFlanTeams.addItem(item);
							break;
						}
						case armourBox:
						{
							ArmourBoxType armourBoxType = (ArmourBoxType)infoType;
							BlockArmourBox block = ModBlocks.registerBlock(nameOf(infoType),
									p -> new BlockArmourBox(p.mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2F, 4F)
											.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK), armourBoxType));
							FlansMod.tabFlanTeams.addItem(block);
							break;
						}
						case playerClass: break;
						case team: break;
						case itemHolder:
						{
							ItemHolderType holderType = (ItemHolderType)infoType;
							BlockItemHolder block = ModBlocks.registerBlock(nameOf(infoType),
									p -> new BlockItemHolder(p.mapColor(net.minecraft.world.level.material.MapColor.STONE).strength(2F, 4F), holderType));
							ModItems.registerTypeItem(ModItems.blockItem(block), infoType);
							FlansMod.tabFlanParts.addItem(block);
							break;
						}
						case rewardBox:
						{
							Item item = new ItemRewardBox((RewardBox)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanTeams.addItem(item);
							break;
						}
						case loadout: break;
						case glove:
						{
							Item item = new ItemGlove((GloveType)infoType);
							ModItems.registerTypeItem(item, infoType);
							FlansMod.tabFlanTeams.addItem(item);
							break;
						}
						default: FlansMod.log.warn("Unrecognised type for " + infoType.shortName);
							break;
					}
				}
				catch(Exception e)
				{
					FlansMod.log.error("Failed to add " + type.name() + " : " + typeFile.name, e);
				}
			}
			FlansMod.log.info("Loaded " + type.name() + ".");
		}
	}

	private static String nameOf(InfoType infoType)
	{
		return (infoType.contentPack + "_" + infoType.shortName).toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
	}

	private static void registerBlockAndItem(String name, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> factory, InfoType infoType)
	{
		Block block = ModBlocks.registerBlock(name, factory);
		ModItems.registerTypeItem(ModItems.blockItem(block), infoType);
	}

	public List<File> GetFolderContentPacks() 
	{
		List<File> result = new ArrayList<File>();
		for(HashMap.Entry<String, IFlansModContentProvider> entry : packs.entrySet())
		{
			String contentPackName = entry.getKey();
			IFlansModContentProvider provider = entry.getValue();
			
			if(provider instanceof ContentPackFlanFolder)
			{
				ContentPackFlanFolder contentPack = (ContentPackFlanFolder)provider;
				if(contentPack.folder.isDirectory())
				{
					result.add(contentPack.folder);
				}
			}
		}
		return result;
	}
}
