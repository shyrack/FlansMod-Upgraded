package com.shyrack.flansmodupgraded.common;

public interface IFlansModContentProvider 
{
	// This is generally just used for running from MCP
	public String GetContentFolder();
	public void RegisterModelRedirects();
}
