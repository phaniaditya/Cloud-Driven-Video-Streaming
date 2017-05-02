package com.project.server;
/* multitaskhadler program
   Team 16*/

import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerMultiTaskHandler {

	static Executor pool = Executors.newFixedThreadPool(5);
	static int start_port = 20000;
	static int range = 5;

	static public void init() {
		for (int i = 0; i < range; i++) {
			start_port++;
			Server theServer = new Server();
			
			theServer.pack();
			theServer.setVisible(true);
			String ServerHost = "127.0.0.1";

			InetAddress ServerIPAddr;
			try {
				ServerIPAddr = InetAddress.getByName(ServerHost);
				theServer.setServerIPAddr(ServerIPAddr);
				theServer.setRTSPport(start_port);
				// The TCP connection with the client for the RTSP session is established
				pool.execute(theServer);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	public static void main(String[] args) {
		init();
	}

}
