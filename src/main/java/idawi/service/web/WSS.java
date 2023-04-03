package idawi.service.web;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WSS extends WebSocketServer {

	private Set<WebSocket> conns = new HashSet<>();

	private GenerationData gd;

	public WSS(int port, GenerationData gd) {
		super(new InetSocketAddress(port));
		this.gd = gd;
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		conns.add(conn);
		System.out.println("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());

		this.sendMessage(conn, "getAllData", gd.getAllData());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		conns.remove(conn);
		System.out.println("Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println("Message from client: " + message);
		for (WebSocket sock : conns) {
			sock.send(message);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// ex.printStackTrace();
		if (conn != null) {
			conns.remove(conn);
			// do some thing if required
		}
		System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
	}

	@Override
	public void onStart() {

	}

	public void sendMessage(WebSocket conn, String eventName, String data) {
		String res = "{";
		res += "\"name\":\"" + eventName + "\",";
		res += "\"data\":" + data;
		res += "}";
		this.onMessage(conn, res);
	}
}