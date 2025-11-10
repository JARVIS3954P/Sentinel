package org.jarvis.enforcer;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class BlockHttpServer {

    private HttpServer server;

    public void start() {
        try {
            // Start a server on localhost, port 80 (standard HTTP port)
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 80), 0);
            server.createContext("/", new BlockPageHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("Block page HTTP server started on port 80.");
        } catch (IOException e) {
            System.err.println("Could not start block page server. Port 80 might be in use or you may need higher privileges.");
            // This is a common issue if another service like Apache is running.
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Block page HTTP server stopped.");
        }
    }

    static class BlockPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Load the block.html file from the resources
            byte[] response;
            try (InputStream is = getClass().getResourceAsStream("/block.html")) {
                if (is == null) {
                    // Fallback if the html file is not found
                    String fallback = "<h1>Blocked by Sentinel</h1>";
                    response = fallback.getBytes();
                } else {
                    response = is.readAllBytes();
                }
            }

            t.sendResponseHeaders(200, response.length);
            try (OutputStream os = t.getResponseBody()) {
                os.write(response);
            }
        }
    }
}