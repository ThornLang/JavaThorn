import time

def fib(n):
    if n <= 1:
        return n
    return fib(n - 1) + fib(n - 2)

N = 30
start_time = time.time()
result = fib(N)
end_time = time.time()

print(f"Fibonacci({N}) = {result}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")