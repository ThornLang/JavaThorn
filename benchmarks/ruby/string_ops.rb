ITERATIONS = 1000

start_time = Time.now

result = ""
(0...ITERATIONS).each do |i|
    result += "Hello#{i} "
end

end_time = Time.now

puts "Iterations: #{ITERATIONS}"
puts "Result length: #{result.length}"
puts "Time: #{((end_time - start_time) * 1000).round(2)}ms"