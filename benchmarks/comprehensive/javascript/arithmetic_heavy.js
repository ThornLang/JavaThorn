const ITERATIONS = 50000;

function mandelbrot(cx, cy, maxIter) {
    let x = 0.0;
    let y = 0.0;
    let i = 0;
    
    while (i < maxIter) {
        const x2 = x * x;
        const y2 = y * y;
        
        if (x2 + y2 > 4.0) {
            return i;
        }
        
        const xtemp = x2 - y2 + cx;
        y = 2.0 * x * y + cy;
        x = xtemp;
        i = i + 1;
    }
    
    return maxIter;
}

const startTime = Date.now();
let sum = 0;

// Sample points in mandelbrot set
let j = 0;
while (j < ITERATIONS) {
    const x = (j % 100) * 0.01 - 0.5;
    const y = Math.floor(j / 100) * 0.01 - 0.5;
    sum = sum + mandelbrot(x, y, 20);
    j = j + 1;
}

const endTime = Date.now();

console.log(`Arithmetic heavy result: ${sum}`);
console.log(`Time: ${endTime - startTime}ms`);