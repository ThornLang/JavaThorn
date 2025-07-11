package com.thorn.stdlib;

import com.thorn.StdlibException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random number generation module for ThornLang
 * Provides pseudo-random and cryptographically secure random generation.
 */
public class Random {
    private static java.util.Random globalRandom = new java.util.Random();
    private static SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate random float in [0, 1)
     * @return Random float
     */
    public static double random() {
        return globalRandom.nextDouble();
    }
    
    /**
     * Generate random integer in [min, max]
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return Random integer
     */
    public static double randint(double min, double max) {
        int minInt = (int) min;
        int maxInt = (int) max;
        if (minInt > maxInt) {
            throw new StdlibException("min must be <= max");
        }
        return minInt + globalRandom.nextInt(maxInt - minInt + 1);
    }
    
    /**
     * Generate random float in [min, max)
     * @param min Minimum value (inclusive)
     * @param max Maximum value (exclusive)
     * @return Random float
     */
    public static double randfloat(double min, double max) {
        if (min >= max) {
            throw new StdlibException("min must be < max");
        }
        return min + (max - min) * globalRandom.nextDouble();
    }
    
    /**
     * Generate random boolean
     * @return Random boolean
     */
    public static boolean randbool() {
        return globalRandom.nextBoolean();
    }
    
    /**
     * Generate n random bytes
     * @param n Number of bytes
     * @return List of bytes (as doubles)
     */
    public static List<Object> randbytes(double n) {
        int count = (int) n;
        byte[] bytes = new byte[count];
        globalRandom.nextBytes(bytes);
        
        List<Object> result = new ArrayList<>();
        for (byte b : bytes) {
            result.add((double)(b & 0xFF));
        }
        return result;
    }
    
    /**
     * Set random seed
     * @param seed Seed value (null for system entropy)
     */
    public static void seed(Double seed) {
        if (seed == null) {
            globalRandom = new java.util.Random();
        } else {
            globalRandom = new java.util.Random(seed.longValue());
        }
    }
    
    /**
     * Random element from array
     * @param items Array to choose from
     * @return Random element
     */
    public static Object choice(List<?> items) {
        if (items.isEmpty()) {
            throw new StdlibException("Cannot choose from empty list");
        }
        return items.get(globalRandom.nextInt(items.size()));
    }
    
    /**
     * Multiple selections with replacement
     * @param items Array to choose from
     * @param k Number of selections
     * @param weights Optional weights (null for uniform)
     * @return List of selected items
     */
    public static List<Object> choices(List<?> items, double k, List<Double> weights) {
        if (items.isEmpty()) {
            throw new StdlibException("Cannot choose from empty list");
        }
        
        int count = (int) k;
        List<Object> result = new ArrayList<>();
        
        if (weights == null) {
            // Uniform selection
            for (int i = 0; i < count; i++) {
                result.add(items.get(globalRandom.nextInt(items.size())));
            }
        } else {
            // Weighted selection
            if (weights.size() != items.size()) {
                throw new StdlibException("Weights must match items length");
            }
            
            // Build cumulative weights
            double[] cumulative = new double[weights.size()];
            cumulative[0] = weights.get(0);
            for (int i = 1; i < weights.size(); i++) {
                cumulative[i] = cumulative[i-1] + weights.get(i);
            }
            
            double total = cumulative[cumulative.length - 1];
            
            for (int i = 0; i < count; i++) {
                double r = globalRandom.nextDouble() * total;
                int index = Arrays.binarySearch(cumulative, r);
                if (index < 0) {
                    index = -index - 1;
                }
                result.add(items.get(index));
            }
        }
        
        return result;
    }
    
    /**
     * Sample without replacement
     * @param items Array to sample from
     * @param k Number of samples
     * @return List of sampled items
     */
    @SuppressWarnings("unchecked")
    public static List<Object> sample(List<?> items, double k) {
        int count = (int) k;
        if (count > items.size()) {
            throw new StdlibException("Sample size cannot exceed population size");
        }
        
        List<Object> copy = new ArrayList<>(items);
        Collections.shuffle(copy, globalRandom);
        return copy.subList(0, count);
    }
    
    /**
     * Shuffle array in-place
     * @param items Array to shuffle
     */
    @SuppressWarnings("unchecked")
    public static void shuffle(List<?> items) {
        Collections.shuffle((List<Object>) items, globalRandom);
    }
    
    /**
     * Return shuffled copy
     * @param items Array to shuffle
     * @return Shuffled copy
     */
    public static List<Object> shuffled(List<?> items) {
        List<Object> copy = new ArrayList<>(items);
        Collections.shuffle(copy, globalRandom);
        return copy;
    }
    
    /**
     * Normal (Gaussian) distribution
     * @param mean Mean value
     * @param stddev Standard deviation
     * @return Random value from normal distribution
     */
    public static double normal(double mean, double stddev) {
        return mean + stddev * globalRandom.nextGaussian();
    }
    
    /**
     * Exponential distribution
     * @param lambda Rate parameter
     * @return Random value from exponential distribution
     */
    public static double exponential(double lambda) {
        return -Math.log(1.0 - globalRandom.nextDouble()) / lambda;
    }
    
    /**
     * Uniform distribution
     * @param a Lower bound
     * @param b Upper bound
     * @return Random value from uniform distribution
     */
    public static double uniform(double a, double b) {
        return randfloat(a, b);
    }
    
    /**
     * Random string generation
     * @param length String length
     * @param chars Character set (null for alphanumeric)
     * @return Random string
     */
    public static String randString(double length, String chars) {
        int len = (int) length;
        if (chars == null) {
            chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        }
        
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(globalRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generate UUID v4
     * @return UUID string
     */
    public static String uuid4() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Random hex string
     * @param bytes Number of bytes
     * @return Hex string
     */
    public static String hexString(double bytes) {
        int byteCount = (int) bytes;
        byte[] data = new byte[byteCount];
        globalRandom.nextBytes(data);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
    
    /**
     * Cryptographically secure random float
     * @return Secure random float in [0, 1)
     */
    public static double cryptoRandom() {
        return secureRandom.nextDouble();
    }
    
    /**
     * Cryptographically secure random integer
     * @param min Minimum value
     * @param max Maximum value
     * @return Secure random integer
     */
    public static double cryptoRandint(double min, double max) {
        int minInt = (int) min;
        int maxInt = (int) max;
        if (minInt > maxInt) {
            throw new StdlibException("min must be <= max");
        }
        return minInt + secureRandom.nextInt(maxInt - minInt + 1);
    }
    
    /**
     * Cryptographically secure random bytes
     * @param n Number of bytes
     * @return List of secure random bytes
     */
    public static List<Object> cryptoRandbytes(double n) {
        int count = (int) n;
        byte[] bytes = new byte[count];
        secureRandom.nextBytes(bytes);
        
        List<Object> result = new ArrayList<>();
        for (byte b : bytes) {
            result.add((double)(b & 0xFF));
        }
        return result;
    }
    
    /**
     * Cryptographically secure token
     * @param length Token length
     * @return URL-safe base64 token
     */
    public static String cryptoToken(double length) {
        int len = (int) length;
        byte[] bytes = new byte[len * 3 / 4 + 1];
        secureRandom.nextBytes(bytes);
        
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return token.substring(0, len);
    }
    
    /**
     * Weighted random selection
     * @param items List of (item, weight) pairs
     * @return Selected item
     */
    @SuppressWarnings("unchecked")
    public static Object weightedChoice(List<?> items) {
        if (items.isEmpty()) {
            throw new StdlibException("Cannot choose from empty list");
        }
        
        double totalWeight = 0;
        for (Object item : items) {
            if (item instanceof List && ((List<?>) item).size() == 2) {
                totalWeight += ((Double) ((List<?>) item).get(1));
            } else {
                throw new StdlibException("Items must be (value, weight) pairs");
            }
        }
        
        double r = globalRandom.nextDouble() * totalWeight;
        double cumulative = 0;
        
        for (Object item : items) {
            List<?> pair = (List<?>) item;
            cumulative += (Double) pair.get(1);
            if (r <= cumulative) {
                return pair.get(0);
            }
        }
        
        // Should not reach here
        return ((List<?>) items.get(items.size() - 1)).get(0);
    }
    
    /**
     * Create a new random generator with optional seed
     */
    public static class RandomGenerator {
        private final java.util.Random random;
        
        public RandomGenerator(Double seed) {
            this.random = seed == null ? new java.util.Random() : new java.util.Random(seed.longValue());
        }
        
        public double random() {
            return random.nextDouble();
        }
        
        public double randint(double min, double max) {
            int minInt = (int) min;
            int maxInt = (int) max;
            return minInt + random.nextInt(maxInt - minInt + 1);
        }
        
        public boolean randbool() {
            return random.nextBoolean();
        }
        
        public void seed(double value) {
            random.setSeed((long) value);
        }
    }
}