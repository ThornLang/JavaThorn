import time

ITERATIONS = 100000

def compute():
    sum_val = 0
    multiplier = 7
    divisor = 3
    
    i = 0
    while i < ITERATIONS:
        # Repeated access to same variables
        temp = i * multiplier
        sum_val = sum_val + temp
        sum_val = sum_val / divisor
        sum_val = sum_val * divisor  # Keep sum from growing too large
        i = i + 1
    
    return sum_val

start_time = time.time()
result = compute()
end_time = time.time()

print(f"Hot variables result: {result}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")