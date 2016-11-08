package com.mcf.davidee.autojoin.thread;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;

import com.mcf.davidee.autojoin.AutoJoin;
import com.mcf.davidee.autojoin.ServerInfo;
import com.mcf.davidee.autojoin.gui.AutoJoinScreen;
import net.minecraft.network.status.client.CPacketServerQuery;
import net.minecraft.network.status.server.SPacketPong;
import net.minecraft.network.status.server.SPacketServerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class ThreadPingServer extends Thread {

	private final AutoJoinScreen screen;
	private final ServerInfo info;

	public ThreadPingServer(AutoJoinScreen screen, ServerInfo info) {
		this.screen = screen;
		this.info = info;
	}

	public void newRun() throws UnknownHostException {
		final NetworkManager manager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(info.ip), info.port, true);
		manager.setNetHandler(new INetHandlerStatusClient() {

			private boolean received = false;
			
			@Override
			public void handleServerInfo(SPacketServerInfo packet) {
				ServerStatusResponse response = packet.getResponse();
				
				String version = "???";
				int protocol = 0;
				int curPlayers = -1, maxPlayers = -1;
				if (response.getVersion() != null) {
					protocol = response.getVersion().getProtocol();
					version = response.getVersion().getName();
				}
				if (response.getPlayers() != null) {
					curPlayers = response.getPlayers().getOnlinePlayerCount();
					maxPlayers = response.getPlayers().getMaxPlayers();
				}
				
				if (protocol != AutoJoin.PROTOCOL_VER)
					screen.versionErrror("Version mismatch (" + version + ")");
				else if (curPlayers != -1 && maxPlayers != -1)
					screen.pingSuccess(curPlayers, maxPlayers);
				else
					screen.versionErrror("No population data sent! =/");
				
				received = true;
			}

			@Override
			public void handlePong(SPacketPong packet) {
				manager.closeChannel(new TextComponentString("Finished"));
			}

			@Override
			public void onDisconnect(ITextComponent p_147231_1_) {
				if (!received)
					screen.pingFail(p_147231_1_.getFormattedText());
			}
		});

		try {
			manager.sendPacket(new C00Handshake(AutoJoin.PROTOCOL_VER, info.ip, info.port, EnumConnectionState.STATUS));
			manager.sendPacket(new CPacketServerQuery());
			screen.setManager(manager);
		}
		catch (Throwable throwable) {
			screen.connectError("Packet error: " + throwable.getMessage());
		}
	}
	
	public void run() {
		try {
			newRun();
		}
		catch(UnknownHostException e) {
			screen.connectError("Host error: " + e);
		}
		catch(Exception e) {
			screen.connectError("Error: " + e);
		}
	}

}
