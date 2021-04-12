package idawi.service.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class App {
    public static class ServerThread extends Thread{
        private Socket socket = null;

        public ServerThread() throws IOException {
            super("ServerThread");
            //this.socket = new ServerSocket(8001).accept();
        }

        public void run() {
            while (true) {
                try {
                    this.socket = new ServerSocket(8001).accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()));
                ) {
                    String inputLine, outputLine;

                    while ((inputLine = in.readLine()) != null) {
                        System.out.println(inputLine);
                        outputLine = "Hello World";
                    }

                    //System.out.println(outputLine);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        /*try {
            new ServerThread().start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        // SERVEUR WEB
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/index.html"));
        server.createContext("/graphs", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/graphs.html"));
        server.createContext("/css/main.css", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/css/main.css"));
        server.createContext("/js/init.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/init.js"));
        server.createContext("/js/initGraphs.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/initGraphs.js"));
        server.createContext("/js/classes/Network.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/Network.js"));
        server.createContext("/js/classes/InformationNode.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/InformationNode.js"));
        server.createContext("/js/classes/HotbarNetwork.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/HotbarNetwork.js"));
        server.createContext("/js/classes/SelectionNodes.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/classes/SelectionNodes.js"));
        server.createContext("/js/scripts/jquery.js", (HttpExchange exchange) -> loadLocalFile(exchange, "ressources/js/scripts/jquery-3.5.1.min.js"));
        server.createContext("/data/main", (HttpExchange exchange) -> loadOnlineFile(exchange, "http://127.0.0.1:8080/api"));

        server.setExecutor(null);
        server.start();

        System.out.println("Serveur lancé en localhost sur le port " + port);
    }

    static public void loadLocalFile (HttpExchange e, String path) throws IOException {
        File file = new File(path);
        e.sendResponseHeaders(200, file.length());
        try (OutputStream os = e.getResponseBody()) {
            Files.copy(file.toPath(), os);
        }
    }

    static public void loadOnlineFile (HttpExchange e, String path) throws IOException {
        InputStream is = new URL(path).openStream();
        byte[] file = is.readAllBytes();
        e.sendResponseHeaders(200, file.length);
        try (OutputStream os = e.getResponseBody()) {
            os.write(file);
        }
    }
}
