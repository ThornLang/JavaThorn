function treeSum(depth, value) {
    if (depth === 0) {
        return value;
    }
    
    // Binary tree recursion
    const left = treeSum(depth - 1, value * 2);
    const right = treeSum(depth - 1, value * 2 + 1);
    
    return left + right + value;
}

const DEPTH = 15;
const startTime = Date.now();
const result = treeSum(DEPTH, 1);
const endTime = Date.now();

console.log(`Tree sum (depth ${DEPTH}): ${result}`);
console.log(`Time: ${endTime - startTime}ms`);