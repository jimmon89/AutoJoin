package com.mcf.davidee.autojoin.thread;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import com.mcf.davidee.autojoin.AutoJoin;
import com.mcf.davidee.autojoin.ServerInfo;
import com.mcf.davidee.autojoin.gui.AutoJoinScreen;

public class ThreadPingServer extends Thread {

	private final AutoJoinScreen screen;
	private final ServerInfo info;

	public ThreadPingServer(AutoJoinScreen screen, ServerInfo info) {
		this.screen = screen;
		this.info = info;
	}

	public void newRun() throws UnknownHostException {
		final NetworkManager manager = NetworkManager.provideLanClient(InetAddress.getByName(info.ip), info.port);
		manager.setNetHandler(new INetHandlerStatusClient() {

			private boolean received = false;
			
			@Override
			public void handleServerInfo(S00PacketServerInfo packet) {
				ServerStatusResponse response = packet.getResponse();
				
				String version = "???";
				int protocol = 0;
				int curPlayers = -1, maxPlayers = -1;
				if (response.getProtocolVersionInfo() != null) {
					protocol = response.getProtocolVersionInfo().getProtocol();
					version = response.getProtocolVersionInfo().getName();
				}
				if (response.getPlayerCountData() != null) {
					curPlayers = response.getPlayerCountData().getOnlinePlayerCount();
					maxPlayers = response.getPlayerCountData().getMaxPlayers();
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
			public void handlePong(S01PacketPong packet) {
				manager.closeChannel(new ChatComponentText("Finished"));
			}

			@Override
			public void onDisconnect(IChatComponent p_147231_1_) {
				if (!received)
					screen.pingFail(p_147231_1_.getFormattedText());
			}
		});

		try {
			manager.sendPacket(new C00Handshake(AutoJoin.PROTOCOL_VER, info.ip, info.port, EnumConnectionState.STATUS));
			manager.sendPacket(new C00PacketServerQuery());
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
