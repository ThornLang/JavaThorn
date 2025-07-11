package com.thorn.stdlib;

import com.thorn.StdlibException;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Compression and decompression module for ThornLang
 * Provides support for GZIP, ZIP, and DEFLATE compression.
 */
public class Compression {
    
    /**
     * Compress data using GZIP
     * @param data Data to compress (string or byte list)
     * @return Compressed bytes as list
     */
    public static List<Object> gzipCompress(Object data) {
        try {
            byte[] input = toBytes(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(input);
            }
            
            return bytesToList(baos.toByteArray());
        } catch (IOException e) {
            throw new StdlibException("GZIP compression failed: " + e.getMessage());
        }
    }
    
    /**
     * Decompress GZIP data
     * @param data Compressed data as byte list
     * @return Decompressed data as byte list
     */
    public static List<Object> gzipDecompress(Object data) {
        try {
            byte[] input = toBytes(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(input);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
            }
            
            return bytesToList(baos.toByteArray());
        } catch (IOException e) {
            throw new StdlibException("GZIP decompression failed: " + e.getMessage());
        }
    }
    
    /**
     * Compress data using DEFLATE algorithm
     * @param data Data to compress
     * @param level Compression level (0-9, default 6)
     * @return Compressed bytes
     */
    public static List<Object> deflateCompress(Object data, Double level) {
        try {
            int compressionLevel = level != null ? level.intValue() : Deflater.DEFAULT_COMPRESSION;
            if (compressionLevel < 0 || compressionLevel > 9) {
                throw new StdlibException("Compression level must be 0-9");
            }
            
            byte[] input = toBytes(data);
            Deflater deflater = new Deflater(compressionLevel);
            deflater.setInput(input);
            deflater.finish();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                baos.write(buffer, 0, count);
            }
            
            deflater.end();
            return bytesToList(baos.toByteArray());
            
        } catch (Exception e) {
            throw new StdlibException("DEFLATE compression failed: " + e.getMessage());
        }
    }
    
    /**
     * Decompress DEFLATE data
     * @param data Compressed data
     * @return Decompressed bytes
     */
    public static List<Object> deflateDecompress(Object data) {
        try {
            byte[] input = toBytes(data);
            Inflater inflater = new Inflater();
            inflater.setInput(input);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            
            while (!inflater.finished()) {
                try {
                    int count = inflater.inflate(buffer);
                    baos.write(buffer, 0, count);
                } catch (DataFormatException e) {
                    throw new StdlibException("Invalid DEFLATE data: " + e.getMessage());
                }
            }
            
            inflater.end();
            return bytesToList(baos.toByteArray());
            
        } catch (Exception e) {
            if (e instanceof StdlibException) throw (StdlibException) e;
            throw new StdlibException("DEFLATE decompression failed: " + e.getMessage());
        }
    }
    
    /**
     * Create a ZIP archive from multiple files
     * @param files Map of filename to content (string or bytes)
     * @return ZIP archive as bytes
     */
    @SuppressWarnings("unchecked")
    public static List<Object> zipCreate(Map<String, Object> files) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Map.Entry<String, Object> entry : files.entrySet()) {
                    String filename = entry.getKey();
                    byte[] content = toBytes(entry.getValue());
                    
                    ZipEntry zipEntry = new ZipEntry(filename);
                    zos.putNextEntry(zipEntry);
                    zos.write(content);
                    zos.closeEntry();
                }
            }
            
            return bytesToList(baos.toByteArray());
            
        } catch (IOException e) {
            throw new StdlibException("ZIP creation failed: " + e.getMessage());
        }
    }
    
    /**
     * Extract files from a ZIP archive
     * @param data ZIP archive bytes
     * @return Map of filename to content bytes
     */
    public static Map<String, Object> zipExtract(Object data) {
        try {
            byte[] input = toBytes(data);
            Map<String, Object> files = new HashMap<>();
            
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(input))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        files.put(entry.getName(), bytesToList(baos.toByteArray()));
                    }
                    zis.closeEntry();
                }
            }
            
            return files;
            
        } catch (IOException e) {
            throw new StdlibException("ZIP extraction failed: " + e.getMessage());
        }
    }
    
    /**
     * List files in a ZIP archive without extracting
     * @param data ZIP archive bytes
     * @return List of file information maps
     */
    public static List<Object> zipList(Object data) {
        try {
            byte[] input = toBytes(data);
            List<Object> entries = new ArrayList<>();
            
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(input))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", entry.getName());
                    info.put("size", (double) entry.getSize());
                    info.put("compressed_size", (double) entry.getCompressedSize());
                    info.put("is_directory", entry.isDirectory());
                    info.put("time", (double) entry.getTime());
                    info.put("method", entry.getMethod() == ZipEntry.DEFLATED ? "deflate" : "stored");
                    entries.add(info);
                    zis.closeEntry();
                }
            }
            
            return entries;
            
        } catch (IOException e) {
            throw new StdlibException("ZIP listing failed: " + e.getMessage());
        }
    }
    
    /**
     * Calculate compression ratio
     * @param originalSize Original data size
     * @param compressedSize Compressed data size
     * @return Compression ratio as percentage
     */
    public static double compressionRatio(double originalSize, double compressedSize) {
        if (originalSize == 0) return 0;
        return ((originalSize - compressedSize) / originalSize) * 100;
    }
    
    /**
     * Compress string to base64 encoded GZIP
     * @param str String to compress
     * @return Base64 encoded compressed string
     */
    public static String compressString(String str) {
        List<Object> compressed = gzipCompress(str);
        byte[] bytes = listToBytes(compressed);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * Decompress base64 encoded GZIP to string
     * @param compressedStr Base64 encoded compressed string
     * @return Decompressed string
     */
    public static String decompressString(String compressedStr) {
        byte[] bytes = Base64.getDecoder().decode(compressedStr);
        List<Object> decompressed = gzipDecompress(bytesToList(bytes));
        return new String(listToBytes(decompressed), java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Calculate CRC32 checksum
     * @param data Data to checksum
     * @return CRC32 value
     */
    public static double crc32(Object data) {
        CRC32 crc = new CRC32();
        crc.update(toBytes(data));
        return crc.getValue();
    }
    
    /**
     * Calculate Adler32 checksum
     * @param data Data to checksum
     * @return Adler32 value
     */
    public static double adler32(Object data) {
        Adler32 adler = new Adler32();
        adler.update(toBytes(data));
        return adler.getValue();
    }
    
    // Helper methods
    
    private static byte[] toBytes(Object data) {
        if (data instanceof String) {
            return ((String) data).getBytes(java.nio.charset.StandardCharsets.UTF_8);
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