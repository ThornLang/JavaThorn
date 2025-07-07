import time

ITERATIONS = 50000

def mandelbrot(cx, cy, max_iter):
    x = 0.0
    y = 0.0
    i = 0
    
    while i < max_iter:
        x2 = x * x
        y2 = y * y
        
        if x2 + y2 > 4.0:
            return i
        
        xtemp = x2 - y2 + cx
        y = 2.0 * x * y + cy
        x = xtemp
        i = i + 1
    
    return max_iter

start_time = time.time()
sum_val = 0

# Sample points in mandelbrot set
j = 0
while j < ITERATIONS:
    x = (j % 100) * 0.01 - 0.5
    y = (j // 100) * 0.01 - 0.5
    sum_val = sum_val + mandelbrot(x, y, 20)
    j = j + 1

end_time = time.time()

print(f"Arithmetic heavy result: {sum_val}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")