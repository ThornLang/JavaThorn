const SIZE = 1000;

const startTime = Date.now();

// Create and populate array
const arr = [];
for (let i = 0; i < SIZE; i++) {
    arr.push(i * 2);
}

// Sum all elements
const total = arr.reduce((sum, val) => sum + val, 0);

const endTime = Date.now();

console.log(`Array size: ${SIZE}`);
console.log(`Sum: ${total}`);
console.log(`Time: ${endTime - startTime}ms`);