import time

SIZE = 1000

class DataProcessor:
    def __init__(self):
        self.data = []
        self.cache = {}
    
    def process(self, values):
        result = 0
        
        # Test for-in loop optimization
        for v in values:
            # Test arithmetic fast paths
            squared = v * v
            result = result + squared
            
            # Test method calls and property access
            self.data.append(squared)
        
        # Test while loop optimization
        i = 0
        while i < len(self.data):
            result = result / 2
            i = i + 1
        
        return result
    
    def recursive_process(self, n, acc):
        if n <= 0:
            return acc
        
        # Test return optimization with multiple recursive calls
        return self.recursive_process(n - 1, acc + n)

# Generate test data
test_data = []
i = 0
while i < SIZE:
    test_data.append(i * 0.5)
    i = i + 1

start_time = time.time()

processor = DataProcessor()
result1 = processor.process(test_data)
result2 = processor.recursive_process(100, 0)

# String building test
message = ""
j = 0
while j < 100:
    message = message + f"Result: {j} "
    j = j + 1

end_time = time.time()

print(f"Mixed workload results: {result1}, {result2}")
print(f"Message length: {len(message)}")
print(f"Time: {(end_time - start_time) * 1000:.2f}ms")