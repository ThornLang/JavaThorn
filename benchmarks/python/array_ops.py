import time

SIZE = 1000

start_time = time.time()

# Create and populate array
arr = []
for i in range(SIZE):
    arr.append(i * 2)

# Sum all elements
total = sum(arr)

end_time = time.time()

print(f"Array size: {SIZE}")
print(f"Sum: {total}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")