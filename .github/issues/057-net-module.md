# Implement Networking Module for ThornLang Standard Library

## Overview
Implement a high-performance networking module (`net.thorn`) for TCP/UDP socket programming and HTTP client functionality in ThornLang. This module will be implemented as a Java native module using Java NIO for optimal performance and modern networking capabilities.

## Technical Requirements

### HTTP Client Functions
- [ ] `httpGet(url, headers)` - Perform HTTP GET request
- [ ] `httpPost(url, body, headers)` - Perform HTTP POST request
- [ ] `httpPut(url, body, headers)` - Perform HTTP PUT request
- [ ] `httpDelete(url, headers)` - Perform HTTP DELETE request
- [ ] `httpPatch(url, body, headers)` - Perform HTTP PATCH request
- [ ] `httpHead(url, headers)` - Perform HTTP HEAD request
- [ ] `httpOptions(url, headers)` - Perform HTTP OPTIONS request

### HTTP Advanced Features
- [ ] `downloadFile(url, filename)` - Download file with progress
- [ ] `uploadFile(url, filename, headers)` - Upload file with multipart
- [ ] `httpRequest(options)` - Generic HTTP request with full options
- [ ] Connection pooling and keep-alive support
- [ ] Automatic retry with exponential backoff
- [ ] Request/response interceptors
- [ ] Cookie jar management

### WebSocket Support
- [ ] `createWebSocket(url)` - Create WebSocket connection
- [ ] `wsConnect(ws)` - Connect WebSocket
- [ ] `wsSend(ws, message)` - Send WebSocket message
- [ ] `wsReceive(ws)` - Receive WebSocket message
- [ ] `wsClose(ws)` - Close WebSocket connection
- [ ] `wsOnMessage(ws, callback)` - Message event handler
- [ ] `wsOnError(ws, callback)` - Error event handler

### TCP Networking
- [ ] `tcpConnect(host, port)` - Create TCP connection
- [ ] `tcpListen(port, callback)` - Create TCP server
- [ ] `tcpAccept(server)` - Accept incoming connection
- [ ] `tcpSend(socket, data)` - Send data over TCP
- [ ] `tcpReceive(socket)` - Receive data from TCP
- [ ] `tcpClose(socket)` - Close TCP connection
- [ ] `tcpSetTimeout(socket, timeout)` - Set socket timeout
- [ ] `tcpSetNoDelay(socket, enabled)` - Configure Nagle's algorithm

### UDP Networking
- [ ] `udpSocket(port)` - Create UDP socket
- [ ] `udpSend(socket, data, host, port)` - Send UDP datagram
- [ ] `udpReceive(socket)` - Receive UDP datagram
- [ ] `udpBroadcast(socket, data, port)` - Broadcast UDP message
- [ ] `udpMulticast(socket, group)` - Join multicast group
- [ ] `udpClose(socket)` - Close UDP socket

### DNS and Network Utilities
- [ ] `dnsLookup(hostname)` - Resolve hostname to IP
- [ ] `dnsReverse(ip)` - Reverse DNS lookup
- [ ] `ping(host)` - ICMP ping functionality
- [ ] `traceroute(host)` - Traceroute implementation
- [ ] `getLocalIP()` - Get local IP address
- [ ] `getPublicIP()` - Get public IP via external service
- [ ] `getNetworkInterfaces()` - List network interfaces
- [ ] `isPortOpen(host, port)` - Check if port is open

### URL Utilities
- [ ] `parseURL(url)` - Parse URL into components
- [ ] `buildURL(components)` - Build URL from components
- [ ] `encodeURL(url)` - URL encoding
- [ ] `decodeURL(url)` - URL decoding
- [ ] `encodeQueryParams(params)` - Encode query parameters
- [ ] `decodeQueryParams(query)` - Decode query parameters

### SSL/TLS Support
- [ ] `createSSLContext(options)` - Create SSL context
- [ ] `loadCertificate(filename)` - Load SSL certificate
- [ ] `loadPrivateKey(filename)` - Load private key
- [ ] SSL/TLS version configuration
- [ ] Certificate validation options
- [ ] Client certificate authentication

## Implementation Details

### Java Native Implementation
```java
public class NetModule {
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    
    public static class HttpGet extends ThornCallable {
        @Override
        public Object call(List<Object> arguments) {
            String url = (String) arguments.get(0);
            Map<String, String> headers = arguments.size() > 1 ? 
                (Map<String, String>) arguments.get(1) : new HashMap<>();
            
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();
                
                headers.forEach(builder::header);
                
                HttpResponse<String> response = httpClient.send(
                    builder.build(), 
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> result = new HashMap<>();
                result.put("status", response.statusCode());
                result.put("body", response.body());
                result.put("headers", response.headers().map());
                
                return result;
            } catch (Exception e) {
                throw new RuntimeError(e.getMessage());
            }
        }
    }
}
```

## Testing Requirements
- [ ] Unit tests for all network functions
- [ ] Integration tests with mock servers
- [ ] Performance benchmarks for concurrent requests
- [ ] SSL/TLS certificate validation tests
- [ ] Error handling and timeout tests
- [ ] WebSocket communication tests

## Documentation Requirements
- [ ] API reference for all functions
- [ ] Networking best practices guide
- [ ] Security considerations
- [ ] Performance tuning guide
- [ ] Example applications (HTTP client, chat server)

## References

### Python Standard Library Equivalents
- `urllib` - URL handling modules
- `http.client` - HTTP protocol client
- `socket` - Low-level networking
- `ssl` - TLS/SSL wrapper
- `asyncio` - Asynchronous networking
- Third-party: `requests`, `aiohttp`, `websockets`

### Rust Standard Library Equivalents
- `std::net` - Networking primitives
- `TcpStream`, `TcpListener` - TCP networking
- `UdpSocket` - UDP networking
- Third-party: `reqwest` - HTTP client
- `tokio` - Async networking runtime
- `hyper` - HTTP implementation
- `tungstenite` - WebSocket implementation

## Acceptance Criteria
- [ ] Full HTTP/1.1 and HTTP/2 support
- [ ] WebSocket client implementation
- [ ] TCP and UDP socket support
- [ ] SSL/TLS with certificate validation
- [ ] Connection pooling for HTTP
- [ ] Comprehensive error handling
- [ ] Thread-safe implementation
- [ ] Performance comparable to Java HTTP clients

## Priority
High - Essential for modern networked applications

## Labels
- stdlib
- networking
- java-native
- performance-critical