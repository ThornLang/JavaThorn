import time

def tree_sum(depth, value):
    if depth == 0:
        return value
    
    # Binary tree recursion
    left = tree_sum(depth - 1, value * 2)
    right = tree_sum(depth - 1, value * 2 + 1)
    
    return left + right + value

DEPTH = 15
start_time = time.time()
result = tree_sum(DEPTH, 1)
end_time = time.time()

print(f"Tree sum (depth {DEPTH}): {result}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")