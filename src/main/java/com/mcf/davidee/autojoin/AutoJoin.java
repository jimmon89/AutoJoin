package com.mcf.davidee.autojoin;

import java.net.InetSocketAddress;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraftforge.common.config.Configuration;

import com.mcf.davidee.autojoin.gui.DisconnectedScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;

@Mod(modid="AutoJoin", name="Auto Join", version=AutoJoin.VERSION, dependencies="after:guilib")
public class AutoJoin {
	
	public static final int PROTOCOL_VER = 5;
	public static final String VERSION = "1.7.10.0";
	
	@Instance("AutoJoin")
	public static AutoJoin instance;
	
	private AJConfig config;
	public ServerInfo lastServer;
	
	private GuiScreen guiCache = null;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new AJConfig(new Configuration(event.getSuggestedConfigurationFile()));
		
		ModMetadata modMeta = event.getModMetadata();
		modMeta.authorList = Arrays.asList(new String[] { "Davidee" });
		modMeta.autogenerated = false;
		modMeta.credits = "Thanks to Mojang, Forge, and all your support.";
		modMeta.description = "Easily join a populated public server.";
		modMeta.url = "http://www.minecraftforum.net/topic/1922957-/";
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void clientTick(ClientTickEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		
		if (mc.currentScreen != guiCache) {
			guiCache = mc.currentScreen;
			
			if (guiCache instanceof GuiDisconnected && lastServer != null) 
				mc.displayGuiScreen(new DisconnectedScreen(lastServer, (GuiDisconnected)guiCache));
			if (guiCache instanceof GuiConnecting && mc.getCurrentServerData() != null) /*getServerData*/
				lastServer = ServerInfo.from(mc.getCurrentServerData());
			if (guiCache instanceof GuiMainMenu)
				resetCache();
		}
	}
	
	@SubscribeEvent
	public void connectedToServer(ClientConnectedToServerEvent event) {
		if (event.isLocal)
			resetCache();
		else
			lastServer = ServerInfo.from((InetSocketAddress) event.manager.getRemoteAddress());
	}
	
	public AJConfig getConfig() {
		return config;
	}
	
	public void resetCache() {
		lastServer = null;
	}
}
