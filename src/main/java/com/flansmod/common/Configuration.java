package com.flansmod.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * Minimal replacement for the Forge Configuration class, backed by a simple
 * properties file. Keeps the same method names so ported code compiles
 * unchanged.
 */
public class Configuration
{
	public static final String CATEGORY_GENERAL = "general";
	public static final String CATEGORY_CLIENT = "client";

	private final File file;
	private final Properties properties = new Properties();
	private boolean changed = false;
	private Logger log = FlansMod.LOGGER;

	public Configuration(File file)
	{
		this.file = file;
		if(file.exists())
		{
			try(FileInputStream in = new FileInputStream(file))
			{
				properties.load(in);
			}
			catch(IOException e)
			{
				log.warn("Failed to load config file {}", file, e);
			}
		}
	}

	private String key(String category, String name)
	{
		return category + "." + name;
	}

	public boolean getBoolean(String name, String category, boolean defaultValue)
	{
		String key = key(category, name);
		if(properties.containsKey(key))
			return Boolean.parseBoolean(properties.getProperty(key));
		properties.setProperty(key, String.valueOf(defaultValue));
		changed = true;
		return defaultValue;
	}

	public int getInt(String name, String category, int defaultValue)
	{
		String key = key(category, name);
		if(properties.containsKey(key))
		{
			try
			{
				return Integer.parseInt(properties.getProperty(key));
			}
			catch(NumberFormatException e)
			{
				return defaultValue;
			}
		}
		properties.setProperty(key, String.valueOf(defaultValue));
		changed = true;
		return defaultValue;
	}

	public int getInt(String name, String category, int defaultValue, int min, int max, String comment)
	{
		return getInt(name, category, defaultValue);
	}

	public float getFloat(String name, String category, float defaultValue)
	{
		String key = key(category, name);
		if(properties.containsKey(key))
		{
			try
			{
				return Float.parseFloat(properties.getProperty(key));
			}
			catch(NumberFormatException e)
			{
				return defaultValue;
			}
		}
		properties.setProperty(key, String.valueOf(defaultValue));
		changed = true;
		return defaultValue;
	}

	public double getDouble(String name, String category, double defaultValue)
	{
		String key = key(category, name);
		if(properties.containsKey(key))
		{
			try
			{
				return Double.parseDouble(properties.getProperty(key));
			}
			catch(NumberFormatException e)
			{
				return defaultValue;
			}
		}
		properties.setProperty(key, String.valueOf(defaultValue));
		changed = true;
		return defaultValue;
	}

	public String getString(String name, String category, String defaultValue)
	{
		String key = key(category, name);
		if(properties.containsKey(key))
			return properties.getProperty(key);
		properties.setProperty(key, defaultValue);
		changed = true;
		return defaultValue;
	}

	public boolean hasChanged()
	{
		return changed;
	}

	public void save()
	{
		if(file.getParentFile() != null)
			file.getParentFile().mkdirs();
		try(FileOutputStream out = new FileOutputStream(file))
		{
			properties.store(out, "Flan's Mod configuration");
		}
		catch(IOException e)
		{
			log.warn("Failed to save config file {}", file, e);
		}
		changed = false;
	}
}
