const ITERATIONS = 1000;

const startTime = Date.now();

let result = "";
for (let i = 0; i < ITERATIONS; i++) {
    result += `Hello${i} `;
}

const endTime = Date.now();

console.log(`Iterations: ${ITERATIONS}`);
console.log(`Result length: ${result.length}`);
console.log(`Time: ${endTime - startTime}ms`);