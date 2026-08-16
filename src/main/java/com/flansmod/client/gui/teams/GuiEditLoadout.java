package com.flansmod.client.gui.teams;

import java.util.ArrayList;
import java.util.Comparator;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.Tag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.NbtOps;

import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.common.FlansMod;
import com.flansmod.common.util.FlansModUtil;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.EnumAttachmentType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.paintjob.IPaintableItem;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.LoadoutPool.LoadoutEntry;
import com.flansmod.common.teams.LoadoutPool.LoadoutEntryInfoType;
import com.flansmod.common.teams.LoadoutPool.LoadoutEntryPaintjob;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.teams.TeamsManagerRanked;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class GuiEditLoadout extends GuiTeamsBase
{
	/**
	 * The background image
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/loadouteditor.png");
	
	private static final int WIDTH = 326, HEIGHT = 198;
	
	protected int selectedLoadout = 0;
	protected EnumLoadoutSlot selectedSlot = EnumLoadoutSlot.primary;
	protected int selectedCategory = 0;
	protected int scroller = 0;
	
	private com.flansmod.common.teams.PlayerLoadout previousLoadout = null;
	
	protected ArrayList<LoadoutEntry> availableComponents = new ArrayList<>();
	
	private static final String[] WEAPON_COMPONENT_NAMES = new String[]
			{"Weapon", "Paint", "Scope", "Barrel", "Stock", "Grip", "Extra"};
	private static final String[] NON_WEAPON_COMPONENT_NAMES = new String[]
			{"Item", "Paint"};
	
	public GuiEditLoadout(int i)
	{
		super();
		selectedLoadout = i;
		previousLoadout = ClientTeamsData.theRankData.loadouts[selectedLoadout].copy();
		RecalculateAvailableEntries();
	}
	
	@Override
	public void init()
	{
		super.init();
		
		guiOriginX = width / 2 - WIDTH / 2;
		guiOriginY = height / 2 - HEIGHT / 2;
		
		addRenderableWidget(Button.builder(Component.literal("Confirm"), b ->
		{
			// Send data to server
			TeamsManagerRanked.ConfirmLoadoutChanges();
			ClientTeamsData.OpenLandingPage();
		}).bounds(width / 2 - WIDTH / 2 + 10, height / 2 - HEIGHT / 2 + 143, 82, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), b ->
		{
			ClientTeamsData.theRankData.loadouts[selectedLoadout] = previousLoadout.copy();
			ClientTeamsData.OpenLandingPage();
		}).bounds(width / 2 - WIDTH / 2 + 10, height / 2 - HEIGHT / 2 + 165, 82, 20).build());
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
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
		
		// Draw title text
		extractor.centeredText(font, "Edit Loadout " + (selectedLoadout + 1), guiOriginX + WIDTH / 2, guiOriginY + 4, 0xffffff);
		
		// Draw loadout slots panel
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 70, guiOriginY + 32 + 22 * selectedSlot.ordinal(), 70, 203, 36, 22, textureX, textureY);
			
			extractor.centeredText(font, "Loadout", guiOriginX + 51, guiOriginY + 18, 0xffffff);
			for(int n = 0; n < EnumLoadoutSlot.values().length; n++)
			{
				extractor.centeredText(font, EnumLoadoutSlot.values()[n].name, guiOriginX + 39, guiOriginY + 38 + 22 * n, 0xffffff);
				
				ItemStack stack = data.loadouts[selectedLoadout].slots[n];
				drawSlotInventory(extractor, stack, guiOriginX + 73, guiOriginY + 35 + 22 * n);
			}
		}
		
		// Draw slot panel
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 169, guiOriginY + 32 + 22 * selectedCategory, 70, 203, 36, 22, textureX, textureY);
			
			extractor.centeredText(font, selectedSlot.name, guiOriginX + 150, guiOriginY + 18, 0xffffff);
			if(selectedSlot.isWeapon)
			{
				for(int n = 0; n < WEAPON_COMPONENT_NAMES.length; n++)
				{
					ItemStack stack = data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()];
					InfoType type = (stack != null && stack.getItem() instanceof IFlanItem) ? ((IFlanItem)stack.getItem()).getInfoType() : null;
					int numUnlocks = type != null ? data.GetNumUnlocksForType(type) : 0;
					if(n == 1 && type != null && numUnlocks > 0)
					{
						extractor.centeredText(font, WEAPON_COMPONENT_NAMES[n] + " (" + numUnlocks + ")", guiOriginX + 138, guiOriginY + 38 + 22 * n, 0xffffff);
					}
					else
						extractor.centeredText(font, WEAPON_COMPONENT_NAMES[n], guiOriginX + 138, guiOriginY + 38 + 22 * n, 0xffffff);
					
					switch(n)
					{
						case 0: // Main item
						{
							ItemStack copy = ItemStack.EMPTY.copy();
							if(stack != null)
							{
								copy = stack.copy();
								copy.setDamageValue(0);
							}
							
							drawSlotInventory(extractor, copy, guiOriginX + 172, guiOriginY + 35 + 22 * n);
							break;
						}
						case 1: // Paint
						{
							drawSlotInventory(extractor, stack, guiOriginX + 172, guiOriginY + 35 + 22 * n);
							break;
						}
						default:
						{
							if(stack != null && !stack.isEmpty() && getTag(stack) != null)
							{
								CompoundTag attachmentTags = getTag(stack).getCompoundOrEmpty("attachments");
								if(attachmentTags != null)
								{
									ItemStack attachmentStack = ItemStack.EMPTY.copy();
									
									switch(n)
									{
										case 2: attachmentStack = readStackFromNBT(attachmentTags.getCompoundOrEmpty("scope"));
											break;
										case 3: attachmentStack = readStackFromNBT(attachmentTags.getCompoundOrEmpty("barrel"));
											break;
										case 4: attachmentStack = readStackFromNBT(attachmentTags.getCompoundOrEmpty("stock"));
											break;
										case 5: attachmentStack = readStackFromNBT(attachmentTags.getCompoundOrEmpty("grip"));
											break;
										case 6: attachmentStack = readStackFromNBT(attachmentTags.getCompoundOrEmpty("generic_0"));
											break;
									}
									
									drawSlotInventory(extractor, attachmentStack, guiOriginX + 172, guiOriginY + 35 + 22 * n);
								}
							}
							
							break;
						}
					}
				}
			}
			else
			{
				for(int n = 0; n < NON_WEAPON_COMPONENT_NAMES.length; n++)
				{
					extractor.centeredText(font, NON_WEAPON_COMPONENT_NAMES[n], guiOriginX + 138, guiOriginY + 38 + 22 * n, 0xffffff);
					ItemStack stack = data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()];
					switch(n)
					{
						case 0: // Main item
						{
							ItemStack copy = ItemStack.EMPTY.copy();
							if(stack != null)
							{
								copy = stack.copy();
								copy.setDamageValue(0);
							}
							drawSlotInventory(extractor, copy, guiOriginX + 172, guiOriginY + 35 + 22 * n);
							break;
						}
						case 1: // Paint
						{
							drawSlotInventory(extractor, stack, guiOriginX + 172, guiOriginY + 35 + 22 * n);
							break;
						}
						default:
						{
							break;
						}
					}
				}
			}
		}
		
		// Draw stats panel
		{
			String name = "";
			ItemStack stack = data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()];
			if(stack != null && !stack.isEmpty())
			{
				name = stack.getHoverName().getString();
			}
			
			extractor.centeredText(font, name, guiOriginX + 262, guiOriginY + 18, 0xffffff);
			
			DrawGun(extractor, stack, guiOriginX + 254, guiOriginY + 48, 40f);
			
			extractor.centeredText(font, "Damage", guiOriginX + 234, guiOriginY + 60, 0xffffff);
			extractor.centeredText(font, "Accuracy", guiOriginX + 234, guiOriginY + 70, 0xffffff);
			extractor.centeredText(font, "Ammo", guiOriginX + 234, guiOriginY + 80, 0xffffff);
			
			if(stack != null && stack.getItem() instanceof ItemGun)
			{
				GunType type = ((ItemGun)stack.getItem()).GetType();
				
				LoadoutEntryInfoType entry = pool.GetLoadoutEntryForInfoType(selectedSlot.ordinal(), type);
				
				ShootableType mainAmmo = null;
				int numClips = 1;
				for(ItemStack extra : entry.extraItems)
				{
					if(extra != null && extra.getItem() instanceof ItemShootable)
					{
						mainAmmo = ((ItemShootable)extra.getItem()).type;
						numClips = extra.getCount();
						break;
					}
				}
				
				if(mainAmmo != null)
				{
					extractor.centeredText(font, String.format("%.0f", type.damage * mainAmmo.damageVsLiving * mainAmmo.numBullets), guiOriginX + 290, guiOriginY + 60, 0xffffff);
					extractor.centeredText(font, String.format("%.0f", (50.0f - type.bulletSpread) * 2.0f), guiOriginX + 290, guiOriginY + 70, 0xffffff);
					extractor.centeredText(font, String.format("%d", mainAmmo.roundsPerItem * numClips), guiOriginX + 290, guiOriginY + 80, 0xffffff);
				}
			}
		}
		
		// Draw selector panel
		{
			extractor.centeredText(font, "Choose " + WEAPON_COMPONENT_NAMES[selectedCategory].toLowerCase(), guiOriginX + 262, guiOriginY + 95, 0xffffff);
			
			for(int row = 0; row < 4; row++)
			{
				for(int col = 0; col < 6; col++)
				{
					int index = scroller * 24 + row * 6 + col;
					
					if(index >= availableComponents.size())
					{
						continue;
					}
					
					LoadoutEntry entry = availableComponents.get(index);
					if(entry instanceof LoadoutEntryInfoType)
					{
						drawSlotInventory(extractor, new ItemStack(((LoadoutEntryInfoType)entry).type.getItem()), guiOriginX + 209 + col * 18, guiOriginY + 107 + row * 18);
					}
					else if(entry instanceof LoadoutEntryPaintjob)
					{
						Paintjob paintjob = ((LoadoutEntryPaintjob)entry).paintjob;
						
						DrawRarityBackground(extractor, paintjob.rarity, guiOriginX + 209 + col * 18, guiOriginY + 107 + row * 18);
						
						ItemStack paintjobStack = new ItemStack(paintjob.parent.getItem());
						paintjobStack.setDamageValue(paintjob.ID);
						drawSlotInventory(extractor, paintjobStack, guiOriginX + 209 + col * 18, guiOriginY + 107 + row * 18);
					}
					
					if(!entry.available)
					{
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 209 + col * 18, guiOriginY + 107 + row * 18, 332, 161, 16, 16, textureX, textureY);
						if(entry.unlockLevel > 0)
						{
							extractor.centeredText(font, "" + entry.unlockLevel, guiOriginX + 218 + col * 18, guiOriginY + 112 + row * 18, 0xffffff);
						}
					}
				}
			}
		}
	}
	
	private boolean IsInSquare(int clickX, int clickY, int x, int y, int w, int h)
	{
		return x <= clickX && clickX < x + w
				&& y <= clickY && clickY < y + h;
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int i = (int)event.x();
		int j = (int)event.y();
		int k = event.button();
		int x = i - guiOriginX;
		int y = j - guiOriginY;
		if(k == 0 || k == 1)
		{
			// Loadout slots panel
			for(int n = 0; n < EnumLoadoutSlot.values().length; n++)
			{
				if(IsInSquare(x, y, 70, 32 + 22 * n, 22, 22))
				{
					selectedSlot = EnumLoadoutSlot.values()[n];
					//if(!selectedSlot.isWeapon && selectedCategory > 2)
					selectedCategory = 0;
					RecalculateAvailableEntries();
				}
			}
			
			// Slot panel
			if(selectedSlot.isWeapon)
			{
				for(int n = 0; n < WEAPON_COMPONENT_NAMES.length; n++)
				{
					if(IsInSquare(x, y, 169, 32 + 22 * n, 22, 22))
					{
						selectedCategory = n;
						RecalculateAvailableEntries();
					}
				}
			}
			else
			{
				for(int n = 0; n < NON_WEAPON_COMPONENT_NAMES.length; n++)
				{
					if(IsInSquare(x, y, 169, 32 + 22 * n, 22, 22))
					{
						selectedCategory = n;
						RecalculateAvailableEntries();
					}
				}
			}
			
			// Selector panel
			for(int row = 0; row < 4; row++)
			{
				for(int col = 0; col < 6; col++)
				{
					int index = scroller * 24 + row * 6 + col;
					
					if(index >= availableComponents.size()) continue;
					
					if(!availableComponents.get(index).available) continue;
					
					if(IsInSquare(x, y, 209 + col * 18, 107 + row * 18, 18, 18))
					{
						LoadoutEntry entry = availableComponents.get(index);
						SelectItem(entry);
					}
				}
			}
			
			if(IsInSquare(x, y, 257, 179, 10, 10))
			{
				SelectItem(null);
			}
		}
		return true;
	}
	
	public void SelectItem(LoadoutEntry entry)
	{
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		switch(selectedCategory)
		{
			case 0: // Main item
			{
				if(entry instanceof LoadoutEntryInfoType)
				{
					data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()] = new ItemStack(((LoadoutEntryInfoType)entry).type.getItem());
				}
				else if(entry != null)
				{
					FlansMod.log.warn("Loadout entry doesn't match for slot");
				}
				break;
			}
			case 1: // Paint
			{
				if(entry instanceof LoadoutEntryPaintjob)
				{
					if(data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()] != null)
					{
						data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()].setDamageValue(((LoadoutEntryPaintjob)entry).paintjob.ID);
					}
					else FlansMod.log.warn("Applying paintjob to null item!");
				}
				else if(entry != null)
				{
					FlansMod.log.warn("Loadout entry doesn't match slot");
				}
			}
			default: // Attachments
			{
				if(entry instanceof LoadoutEntryInfoType || entry == null)
				{
					ItemStack stack = data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()];
					if(stack != null && !stack.isEmpty())
					{
						CompoundTag stackTags = getTag(stack);
						if(stackTags == null)
						{
							stackTags = new CompoundTag();
						}
						CompoundTag attachmentTags = stackTags.getCompoundOrEmpty("attachments");
						if(attachmentTags == null)
						{
							attachmentTags = new CompoundTag();
						}
						
						CompoundTag ourTags = new CompoundTag();
						if(entry != null)
						{
							ItemStack attachmentStack = new ItemStack(((LoadoutEntryInfoType)entry).type.getItem());
							ourTags = writeStackToNBT(attachmentStack);
						}
						
						switch(selectedCategory)
						{
							case 2: attachmentTags.put("scope", ourTags);
								break;
							case 3: attachmentTags.put("barrel", ourTags);
								break;
							case 4: attachmentTags.put("stock", ourTags);
								break;
							case 5: attachmentTags.put("grip", ourTags);
								break;
							case 6: attachmentTags.put("generic_0", ourTags);
								break;
						}
						
						stackTags.put("attachments", attachmentTags);
						setTag(stack, stackTags);
					}
					else FlansMod.log.warn("Applying attachment to null item!");
				}
				else FlansMod.log.warn("Loadout entry doesn't match for slot");
			}
			
		}
		
	}
	
	private static CompoundTag getTag(ItemStack stack)
	{
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}
	
	private static void setTag(ItemStack stack, CompoundTag tag)
	{
		if(tag == null || tag.isEmpty())
			stack.remove(DataComponents.CUSTOM_DATA);
		else
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}
	
	private static CompoundTag writeStackToNBT(ItemStack stack)
	{
		DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
		return (CompoundTag)ItemStack.CODEC.encodeStart(ops, stack).result().orElse(new CompoundTag());
	}
	
	private static ItemStack readStackFromNBT(CompoundTag tag)
	{
		if(tag == null || tag.isEmpty())
			return ItemStack.EMPTY;
		DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
		return ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
	}
	
	public class LoadoutComparator implements Comparator<LoadoutEntry>
	{
		@Override
		public int compare(LoadoutEntry a, LoadoutEntry b)
		{
			if(a.unlockLevel < b.unlockLevel) return -1;
			if(a.unlockLevel > b.unlockLevel) return 1;
			
			if(a instanceof LoadoutEntryPaintjob && b instanceof LoadoutEntryPaintjob)
			{
				if(((LoadoutEntryPaintjob)a).paintjob.rarity.ordinal() < ((LoadoutEntryPaintjob)b).paintjob.rarity.ordinal())
					return -1;
				if(((LoadoutEntryPaintjob)a).paintjob.rarity.ordinal() > ((LoadoutEntryPaintjob)b).paintjob.rarity.ordinal())
					return 1;
			}
			
			return 0;
		}
	}
	
	public void RecalculateAvailableEntries()
	{
		availableComponents.clear();
		
		PlayerRankData data = ClientTeamsData.theRankData;
		LoadoutPool pool = ClientTeamsData.currentPool;
		
		ArrayList<LoadoutEntry> unlockedEntries = new ArrayList<>();
		ArrayList<LoadoutEntry> lockedEntries = new ArrayList<>();
		
		if(selectedCategory == 1) // Paint
		{
			ItemStack stack = data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()];
			if(stack != null && stack.getItem() instanceof IPaintableItem)
			{
				PaintableType type = ((IPaintableItem)stack.getItem()).GetPaintableType();
				for(int i = 0; i < type.paintjobs.size(); i++)
				{
					LoadoutEntryPaintjob entry = new LoadoutEntryPaintjob();
					entry.unlockLevel = 0;
					entry.paintjob = type.paintjobs.get(i);
					if(i == 0)
					{
						entry.available = true;
					}
					else
					{
						entry.available = TeamsManagerRanked.LocalPlayerOwnsUnlock(entry.paintjob.hashCode());
					}
					
					if(entry.available)
						unlockedEntries.add(entry);
					else
						lockedEntries.add(entry);
				}
			}
		}
		else
		{
			for(LoadoutEntryInfoType entry : pool.unlocks[selectedSlot.ordinal()])
			{
				switch(selectedCategory)
				{
					case 0: // Main item
					{
						if(entry.type instanceof AttachmentType) continue;
						break;
					}
					case 1: break; // Paint. Shouldn't even get here
					default: // Attachments. Check sub-type
					{
						// Check it is an attachment
						if(!(entry.type instanceof AttachmentType)) continue;
						
						// Check that the gun allows it
						GunType gunType = ((ItemGun)data.loadouts[selectedLoadout].slots[selectedSlot.ordinal()].getItem()).GetType();
						if(!gunType.allowAllAttachments && !gunType.allowedAttachments.contains(entry.type)) continue;
						
						// And check that it is right for the slot
						EnumAttachmentType attachType = ((AttachmentType)entry.type).type;
						switch(selectedCategory)
						{
							case 2: if(attachType != EnumAttachmentType.sights) continue;
							else break;
							case 3: if(attachType != EnumAttachmentType.barrel) continue;
							else break;
							case 4: if(attachType != EnumAttachmentType.stock) continue;
							else break;
							case 5: if(attachType != EnumAttachmentType.grip) continue;
							else break;
							case 6: if(attachType != EnumAttachmentType.generic) continue;
							else break;
						}
						break;
					}
				}
				LoadoutEntryInfoType copy = new LoadoutEntryInfoType();
				
				copy.type = entry.type;
				copy.unlockLevel = entry.unlockLevel;
				copy.available = data.currentLevel >= copy.unlockLevel;
				
				if(copy.available)
					unlockedEntries.add(copy);
				else
					lockedEntries.add(copy);
			}
		}
		
		unlockedEntries.sort(new LoadoutComparator());
		lockedEntries.sort(new LoadoutComparator());
		
		availableComponents.addAll(unlockedEntries);
		availableComponents.addAll(lockedEntries);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
