const ITERATIONS = 100000;

function compute() {
    let sum = 0;
    const multiplier = 7;
    const divisor = 3;
    
    let i = 0;
    while (i < ITERATIONS) {
        // Repeated access to same variables
        const temp = i * multiplier;
        sum = sum + temp;
        sum = sum / divisor;
        sum = sum * divisor;  // Keep sum from growing too large
        i = i + 1;
    }
    
    return sum;
}

const startTime = Date.now();
const result = compute();
const endTime = Date.now();

console.log(`Hot variables result: ${result}`);
console.log(`Time: ${endTime - startTime}ms`);