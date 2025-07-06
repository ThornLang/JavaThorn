def fib(n)
    return n if n <= 1
    fib(n - 1) + fib(n - 2)
end

N = 30
start_time = Time.now
result = fib(N)
end_time = Time.now

puts "Fibonacci(#{N}) = #{result}"
puts "Time: #{((end_time - start_time) * 1000).round(2)}ms"