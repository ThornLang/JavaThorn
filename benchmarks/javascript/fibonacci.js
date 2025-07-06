function fib(n) {
    if (n <= 1) {
        return n;
    }
    return fib(n - 1) + fib(n - 2);
}

const N = 30;
const startTime = Date.now();
const result = fib(N);
const endTime = Date.now();

console.log(`Fibonacci(${N}) = ${result}`);
console.log(`Time: ${endTime - startTime}ms`);