import time

ITERATIONS = 1000

start_time = time.time()

result = ""
for i in range(ITERATIONS):
    result += f"Hello{i} "

end_time = time.time()

print(f"Iterations: {ITERATIONS}")
print(f"Result length: {len(result)}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")