package com.vvk.banque.cli;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;

public final class BankShellDemo {
    private static final BankShell core = new BankShell();
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", BankShellDemo::serveHtml);
        server.createContext("/cli", BankShellDemo::handleCli);
        server.start();
        System.out.println("Demo terminal at http://localhost:" + port);
    }
    private static void serveHtml(HttpExchange ex) throws IOException {
        String html = """
        <!doctype html>
        <title>vvk-bank</title>
        <body style=background:#111;color:#0f0;font-family:monospace>
        <h2>vvk-bank demo terminal</h2>
        <form onsubmit="send();return false">
          <input id=c style=width:60%;background:#000;color:#0f0;border:1px solid #0f0>
          <input type=submit value=run>
        </form>
        <pre id=r></pre>
        <script>
        async function send(){
          const cmd = document.getElementById('c').value;
          const res = await fetch('/cli', {method:'POST', body:cmd});
          const txt = await res.text();
          // Appends the command and the response to the terminal window
          document.getElementById('r').textContent += '$ ' + cmd + '\\n' + txt + '\\n';
          document.getElementById('c').value='';
        }
        </script>
        """;
        ex.sendResponseHeaders(200, html.length());
        try (OutputStream os = ex.getResponseBody()) { os.write(html.getBytes()); }
    }
    private static void handleCli(HttpExchange ex) throws IOException {
        // Read the command from the web request body
        String cmd = new String(ex.getRequestBody().readAllBytes()).trim();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buf, true);
        
        // Temporarily redirect System.out so that the BankShell output is captured
        PrintStream old = System.out;
        System.setOut(ps);
        try { 
            core.handle(cmd); // Execute the BankShell command
        }
        finally { 
            System.setOut(old); // Restore original System.out
        }
        
        // Send the captured output back as the HTTP response
        byte[] out = buf.toByteArray();
        ex.sendResponseHeaders(200, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
}

