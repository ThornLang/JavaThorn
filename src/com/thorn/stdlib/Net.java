package com.thorn.stdlib;

import com.thorn.StdlibException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

/**
 * Network operations module for ThornLang
 * Provides HTTP client, TCP/UDP sockets, and URL utilities.
 */
public class Net {
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Perform an HTTP GET request
     * @param url URL to request
     * @return Response map with status, headers, and body
     */
    public static Map<String, Object> get(String url) {
        return get(url, null, null);
    }
    
    /**
     * Perform an HTTP GET request with options
     * @param url URL to request
     * @param headers Optional headers map
     * @param options Optional options map (timeout, follow_redirects, etc.)
     * @return Response map with status, headers, and body
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> get(String url, Map<String, Object> headers, Map<String, Object> options) {
        return request("GET", url, headers, null, options);
    }
    
    /**
     * Perform an HTTP POST request
     * @param url URL to request
     * @param data Request body (string or byte list)
     * @return Response map
     */
    public static Map<String, Object> post(String url, Object data) {
        return post(url, data, null, null);
    }
    
    /**
     * Perform an HTTP POST request with options
     * @param url URL to request
     * @param data Request body
     * @param headers Optional headers
     * @param options Optional options
     * @return Response map
     */
    public static Map<String, Object> post(String url, Object data, Map<String, Object> headers, Map<String, Object> options) {
        return request("POST", url, headers, data, options);
    }
    
    /**
     * Perform an HTTP PUT request
     * @param url URL to request
     * @param data Request body
     * @return Response map
     */
    public static Map<String, Object> put(String url, Object data) {
        return put(url, data, null, null);
    }
    
    /**
     * Perform an HTTP PUT request with options
     * @param url URL to request
     * @param data Request body
     * @param headers Optional headers
     * @param options Optional options
     * @return Response map
     */
    public static Map<String, Object> put(String url, Object data, Map<String, Object> headers, Map<String, Object> options) {
        return request("PUT", url, headers, data, options);
    }
    
    /**
     * Perform an HTTP DELETE request
     * @param url URL to request
     * @return Response map
     */
    public static Map<String, Object> delete(String url) {
        return delete(url, null, null);
    }
    
    /**
     * Perform an HTTP DELETE request with options
     * @param url URL to request
     * @param headers Optional headers
     * @param options Optional options
     * @return Response map
     */
    public static Map<String, Object> delete(String url, Map<String, Object> headers, Map<String, Object> options) {
        return request("DELETE", url, headers, null, options);
    }
    
    /**
     * Perform an HTTP HEAD request
     * @param url URL to request
     * @return Response map (no body)
     */
    public static Map<String, Object> head(String url) {
        return head(url, null, null);
    }
    
    /**
     * Perform an HTTP HEAD request with options
     * @param url URL to request
     * @param headers Optional headers
     * @param options Optional options
     * @return Response map (no body)
     */
    public static Map<String, Object> head(String url, Map<String, Object> headers, Map<String, Object> options) {
        return request("HEAD", url, headers, null, options);
    }
    
    /**
     * Generic HTTP request method
     * @param method HTTP method
     * @param url URL to request
     * @param headers Optional headers
     * @param data Optional request body
     * @param options Optional options
     * @return Response map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> request(String method, String url, Map<String, Object> headers, 
                                                Object data, Map<String, Object> options) {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            
            // Configure connection
            conn.setRequestMethod(method);
            configureConnection(conn, headers, options);
            
            // Send request body if present
            if (data != null && !method.equals("GET") && !method.equals("HEAD")) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] bytes = toBytes(data);
                    os.write(bytes);
                }
            }
            
            // Read response
            int status = conn.getResponseCode();
            Map<String, Object> responseHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                if (key != null && values != null && !values.isEmpty()) {
                    responseHeaders.put(key, values.size() == 1 ? values.get(0) : values);
                }
            }
            
            // Read body
            Object body = null;
            if (!method.equals("HEAD")) {
                InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
                if (is != null) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, len);
                        }
                        
                        // Try to return as string if it's text
                        String contentType = conn.getContentType();
                        if (contentType != null && (contentType.startsWith("text/") || 
                            contentType.contains("json") || contentType.contains("xml"))) {
                            body = baos.toString(StandardCharsets.UTF_8.name());
                        } else {
                            body = bytesToList(baos.toByteArray());
                        }
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", (double) status);
            response.put("headers", responseHeaders);
            response.put("body", body);
            response.put("url", conn.getURL().toString());
            
            return response;
            
        } catch (Exception e) {
            throw new StdlibException("HTTP request failed: " + e.getMessage());
        }
    }
    
    private static void configureConnection(HttpURLConnection conn, Map<String, Object> headers, 
                                            Map<String, Object> options) {
        // Set timeout
        int timeout = DEFAULT_TIMEOUT;
        if (options != null && options.containsKey("timeout")) {
            timeout = ((Double) options.get("timeout")).intValue();
        }
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        
        // Set headers
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue().toString());
            }
        }
        
        // Default headers
        if (conn.getRequestProperty("User-Agent") == null) {
            conn.setRequestProperty("User-Agent", "ThornLang/1.0");
        }
        
        // Follow redirects
        if (options != null && options.containsKey("follow_redirects")) {
            conn.setInstanceFollowRedirects((Boolean) options.get("follow_redirects"));
        }
    }
    
    /**
     * Parse a URL into components
     * @param url URL string to parse
     * @return Map with scheme, host, port, path, query, fragment
     */
    public static Map<String, Object> parseUrl(String url) {
        try {
            URL urlObj = new URL(url);
            Map<String, Object> result = new HashMap<>();
            
            result.put("scheme", urlObj.getProtocol());
            result.put("host", urlObj.getHost());
            result.put("port", urlObj.getPort() == -1 ? 
                (urlObj.getProtocol().equals("https") ? 443.0 : 80.0) : (double) urlObj.getPort());
            result.put("path", urlObj.getPath().isEmpty() ? "/" : urlObj.getPath());
            result.put("query", urlObj.getQuery());
            result.put("fragment", urlObj.getRef());
            
            // Parse query parameters
            if (urlObj.getQuery() != null) {
                Map<String, Object> params = new HashMap<>();
                String[] pairs = urlObj.getQuery().split("&");
                for (String pair : pairs) {
                    String[] parts = pair.split("=", 2);
                    String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name());
                    String value = parts.length > 1 ? 
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()) : "";
                    params.put(key, value);
                }
                result.put("params", params);
            } else {
                result.put("params", new HashMap<String, Object>());
            }
            
            return result;
            
        } catch (Exception e) {
            throw new StdlibException("Invalid URL: " + e.getMessage());
        }
    }
    
    /**
     * Build a URL from components
     * @param components Map with scheme, host, port, path, query/params
     * @return URL string
     */
    @SuppressWarnings("unchecked")
    public static String buildUrl(Map<String, Object> components) {
        try {
            String scheme = (String) components.getOrDefault("scheme", "http");
            String host = (String) components.get("host");
            if (host == null) {
                throw new StdlibException("host is required");
            }
            
            int port = components.containsKey("port") ? 
                ((Double) components.get("port")).intValue() : -1;
            String path = (String) components.getOrDefault("path", "/");
            
            // Build query string
            String query = null;
            if (components.containsKey("params")) {
                Map<String, Object> params = (Map<String, Object>) components.get("params");
                if (!params.isEmpty()) {
                    StringBuilder qs = new StringBuilder();
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        if (qs.length() > 0) qs.append("&");
                        qs.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
                        qs.append("=");
                        qs.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name()));
                    }
                    query = qs.toString();
                }
            } else if (components.containsKey("query")) {
                query = (String) components.get("query");
            }
            
            String fragment = (String) components.get("fragment");
            
            URI uri = new URI(scheme, null, host, port, path, query, fragment);
            return uri.toString();
            
        } catch (Exception e) {
            throw new StdlibException("Failed to build URL: " + e.getMessage());
        }
    }
    
    /**
     * URL encode a string
     * @param str String to encode
     * @return URL encoded string
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new StdlibException("URL encoding failed: " + e.getMessage());
        }
    }
    
    /**
     * URL decode a string
     * @param str String to decode
     * @return URL decoded string
     */
    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new StdlibException("URL decoding failed: " + e.getMessage());
        }
    }
    
    /**
     * Create a TCP socket client
     * @param host Host to connect to
     * @param port Port to connect to
     * @return Socket object
     */
    public static TcpSocket tcpConnect(String host, double port) {
        try {
            Socket socket = new Socket(host, (int) port);
            return new TcpSocket(socket);
        } catch (Exception e) {
            throw new StdlibException("TCP connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Create a TCP server socket
     * @param port Port to listen on
     * @return Server socket object
     */
    public static TcpServer tcpListen(double port) {
        try {
            ServerSocket serverSocket = new ServerSocket((int) port);
            return new TcpServer(serverSocket);
        } catch (Exception e) {
            throw new StdlibException("TCP server creation failed: " + e.getMessage());
        }
    }
    
    /**
     * TCP Socket wrapper
     */
    public static class TcpSocket {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        
        public TcpSocket(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        
        public void send(String data) {
            writer.println(data);
        }
        
        public void sendBytes(Object data) throws IOException {
            byte[] bytes = toBytes(data);
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
        }
        
        public String receive() throws IOException {
            return reader.readLine();
        }
        
        public List<Object> receiveBytes(double count) throws IOException {
            int size = (int) count;
            byte[] buffer = new byte[size];
            int totalRead = 0;
            
            while (totalRead < size) {
                int read = socket.getInputStream().read(buffer, totalRead, size - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            
            if (totalRead < size) {
                byte[] actual = new byte[totalRead];
                java.lang.System.arraycopy(buffer, 0, actual, 0, totalRead);
                return bytesToList(actual);
            }
            
            return bytesToList(buffer);
        }
        
        public void close() throws IOException {
            socket.close();
        }
        
        public boolean isConnected() {
            return socket.isConnected() && !socket.isClosed();
        }
        
        public String getRemoteAddress() {
            return socket.getInetAddress().getHostAddress();
        }
        
        public double getRemotePort() {
            return socket.getPort();
        }
    }
    
    /**
     * TCP Server wrapper
     */
    public static class TcpServer {
        private final ServerSocket serverSocket;
        
        public TcpServer(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }
        
        public TcpSocket accept() throws IOException {
            Socket clientSocket = serverSocket.accept();
            return new TcpSocket(clientSocket);
        }
        
        public void close() throws IOException {
            serverSocket.close();
        }
        
        public double getPort() {
            return serverSocket.getLocalPort();
        }
    }
    
    /**
     * Create a UDP socket
     * @return UDP socket object
     */
    public static UdpSocket udpSocket() {
        return udpSocket(null);
    }
    
    /**
     * Create a UDP socket bound to a port
     * @param port Port to bind to (null for any port)
     * @return UDP socket object
     */
    public static UdpSocket udpSocket(Double port) {
        try {
            DatagramSocket socket = port == null ? 
                new DatagramSocket() : new DatagramSocket(port.intValue());
            return new UdpSocket(socket);
        } catch (Exception e) {
            throw new StdlibException("UDP socket creation failed: " + e.getMessage());
        }
    }
    
    /**
     * UDP Socket wrapper
     */
    public static class UdpSocket {
        private final DatagramSocket socket;
        
        public UdpSocket(DatagramSocket socket) {
            this.socket = socket;
        }
        
        public void send(String data, String host, double port) throws IOException {
            sendBytes(data.getBytes(StandardCharsets.UTF_8), host, port);
        }
        
        public void sendBytes(Object data, String host, double port) throws IOException {
            byte[] bytes = toBytes(data);
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, (int) port);
            socket.send(packet);
        }
        
        public Map<String, Object> receive(double bufferSize) throws IOException {
            byte[] buffer = new byte[(int) bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            Map<String, Object> result = new HashMap<>();
            result.put("data", new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
            result.put("host", packet.getAddress().getHostAddress());
            result.put("port", (double) packet.getPort());
            
            return result;
        }
        
        public Map<String, Object> receiveBytes(double bufferSize) throws IOException {
            byte[] buffer = new byte[(int) bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            byte[] data = new byte[packet.getLength()];
            java.lang.System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
            
            Map<String, Object> result = new HashMap<>();
            result.put("data", bytesToList(data));
            result.put("host", packet.getAddress().getHostAddress());
            result.put("port", (double) packet.getPort());
            
            return result;
        }
        
        public void close() {
            socket.close();
        }
        
        public double getPort() {
            return socket.getLocalPort();
        }
    }
    
    /**
     * Get IP address of a hostname
     * @param hostname Hostname to resolve
     * @return IP address string
     */
    public static String getIpAddress(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            return address.getHostAddress();
        } catch (Exception e) {
            throw new StdlibException("Failed to resolve hostname: " + e.getMessage());
        }
    }
    
    /**
     * Get hostname of an IP address
     * @param ipAddress IP address to resolve
     * @return Hostname string
     */
    public static String getHostname(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address.getHostName();
        } catch (Exception e) {
            throw new StdlibException("Failed to resolve IP address: " + e.getMessage());
        }
    }
    
    /**
     * Get local IP addresses
     * @return List of local IP addresses
     */
    public static List<String> getLocalIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isUp() && !iface.isLoopback()) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {
                            ips.add(addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return what we have
        }
        return ips;
    }
    
    // Helper methods
    
    private static byte[] toBytes(Object data) {
        if (data instanceof String) {
            return ((String) data).getBytes(StandardCharsets.UTF_8);
        } else if (data instanceof List) {
            return listToBytes((List<?>) data);
        } else if (data instanceof byte[]) {
            return (byte[]) data;
        } else {
            throw new StdlibException("Data must be string or byte list");
        }
    }
    
    private static byte[] listToBytes(List<?> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Double) {
                bytes[i] = ((Double) item).byteValue();
            } else {
                throw new StdlibException("Byte list must contain numbers");
            }
        }
        return bytes;
    }
    
    private static List<Object> bytesToList(byte[] bytes) {
        List<Object> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add((double)(b & 0xFF));
        }
        return list;
    }
}