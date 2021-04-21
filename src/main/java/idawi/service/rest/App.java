package idawi.service.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class App {

	public static void main(String[] args) throws IOException {
		GenerationData gd = new GenerationData();
		// Endpoint.publish("http://localhost:9000/test", new WebsocketServer( new
		// GenerationData()));

		// SERVEUR WEB
		int port = 8000;
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/index.html"));
		server.createContext("/graphs", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/graphs.html"));
		server.createContext("/css/main.css",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/css/main.css"));
		server.createContext("/js/init.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/init.js"));
		server.createContext("/js/initGraphs.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/initGraphs.js"));
		server.createContext("/js/classes/Network.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/Network.js"));
		server.createContext("/js/classes/InformationNode.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/InformationNode.js"));
		server.createContext("/js/classes/HotbarNetwork.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/HotbarNetwork.js"));
		server.createContext("/js/classes/SelectionNodes.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/SelectionNodes.js"));
		server.createContext("/js/classes/TokensPlotly.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/TokensPlotly.js"));
		server.createContext("/js/scripts/jquery.js",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/scripts/jquery-3.5.1.min.js"));

//		server.createContext("/ajax/getAllTokens", (HttpExchange exchange) -> getAllTokens(exchange, gd));
		/*
		server.createContext("/ajax/getAllData", (HttpExchange exchange) -> {
			StringBuilder sb = new StringBuilder();
			InputStream ios = exchange.getRequestBody();
			int i;
			while ((i = ios.read()) != -1) {
				sb.append((char) i);
			}

			HashMap<String, String> params = new HashMap<String, String>();
			System.out.println(sb.toString());
			for (String tuple : sb.toString().split("&")) {
				params.put(tuple.split("=")[0], tuple.split("=")[1]);
			}

			System.out.println("/ajax/getAllData called from " + exchange.getRemoteAddress().toString());
			for (String key : params.keySet()) {
				System.out.println(" >>> " + key + " : " + params.get(key));
			}
			getAllData(exchange, gd, params.get("machine"), params.get("token"));
		});*/
		/*
		server.createContext("/ajax/getLastData", (HttpExchange exchange) -> {
			StringBuilder sb = new StringBuilder();
			InputStream ios = exchange.getRequestBody();
			int i;
			while ((i = ios.read()) != -1) {
				sb.append((char) i);
			}

			HashMap<String, String> params = new HashMap<String, String>();
			for (String tuple : sb.toString().split("&")) {
				params.put(tuple.split("=")[0], tuple.split("=")[1]);
			}

			System.out.println("/ajax/getLastData called from " + exchange.getRemoteAddress().toString());
			for (String key : params.keySet()) {
				System.out.println(" >>> " + key + " : " + params.get(key));
			}
			getLastData(exchange, gd, params.get("machine"), params.get("token"));
		});*/

		server.createContext("/js/listTokens.json",
				(HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/listTokens.json"));

		server.createContext("/data/main",
				(HttpExchange exchange) -> loadOnlineFile(exchange, "http://127.0.0.1:8080/api"));

		server.setExecutor(null);
		server.start();

		System.out.println("Serveur lancé en localhost sur le port " + port);
	}

	static public void loadLocalFile(HttpExchange e, String path) throws IOException {
		File file = new File(path);
		e.sendResponseHeaders(200, file.length());
		try (OutputStream os = e.getResponseBody()) {
			Files.copy(file.toPath(), os);
		}
	}

	static public void loadOnlineFile(HttpExchange e, String path) throws IOException {
		InputStream is = new URL(path).openStream();
		byte[] file = is.readAllBytes();
		e.sendResponseHeaders(200, file.length);
		try (OutputStream os = e.getResponseBody()) {
			os.write(file);
		}
	}
/*
	static public void getAllData(HttpExchange e, GenerationData gd, String machine, String token) throws IOException {
		byte[] json = new byte[0];
		for (String gdtoken : gd.getTokensName()) {
			if (token.equals(gdtoken)) {
				json = gd.getAllData(machine, gdtoken).getBytes();
			}
		}

		e.sendResponseHeaders(200, json.length);
		try (OutputStream os = e.getResponseBody()) {
			os.write(json);
		}
	}

	static public void getAllTokens(HttpExchange e, GenerationData gd) throws IOException {
		byte[] json = gd.getAllTokens().getBytes();
		e.sendResponseHeaders(200, json.length);
		try (OutputStream os = e.getResponseBody()) {
			os.write(json);
		}
	}

	static public void getLastData(HttpExchange e, GenerationData gd, String machine, String token) throws IOException {
		byte[] json = gd.lastValue(machine, token).getBytes();
		e.sendResponseHeaders(200, json.length);
		try (OutputStream os = e.getResponseBody()) {
			os.write(json);
		}
	}
	*/
}
