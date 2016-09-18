package com.jc.util.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by James on 08/09/2016.
 */
public class SSLConnection {
	public static SSLConnection connectTo(InetAddress address,int port) {
		return new SSLConnection(address,port);
	}
	SSLConnection(InetAddress address,int port) {
		this.addr = address;
		try {
			socket = new Socket(addr, port);
		}catch (IOException e) {
			e.printStackTrace();
			socket = null;
		}
	}
	private InetAddress addr;
	private Socket socket;

	public byte[] Request(byte[] Data) {
		throw new UnsupportedOperationException();//TODO: IMPL
	}

}
