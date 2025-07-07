// Mixed workload benchmark - tests multiple optimizations together
const SIZE = 1000;

class DataProcessor {
    constructor() {
        this.data = [];
        this.cache = {};
    }
    
    process(values) {
        let result = 0;
        
        // Test for-in loop optimization
        for (const v of values) {
            // Test arithmetic fast paths
            const squared = v * v;
            result = result + squared;
            
            // Test method calls and property access
            this.data.push(squared);
        }
        
        // Test while loop optimization
        let i = 0;
        while (i < this.data.length) {
            result = result / 2;
            i = i + 1;
        }
        
        return result;
    }
    
    recursiveProcess(n, acc) {
        if (n <= 0) {
            return acc;
        }
        
        // Test return optimization with multiple recursive calls
        return this.recursiveProcess(n - 1, acc + n);
    }
}

// Generate test data
const testData = [];
let i = 0;
while (i < SIZE) {
    testData.push(i * 0.5);
    i = i + 1;
}

const startTime = Date.now();

const processor = new DataProcessor();
const result1 = processor.process(testData);
const result2 = processor.recursiveProcess(100, 0);

// String building test
let message = "";
let j = 0;
while (j < 100) {
    message = message + `Result: ${j} `;
    j = j + 1;
}

const endTime = Date.now();

console.log(`Mixed workload results: ${result1}, ${result2}`);
console.log(`Message length: ${message.length}`);
console.log(`Time: ${endTime - startTime}ms`);