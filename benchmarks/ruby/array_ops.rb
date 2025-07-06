SIZE = 1000

start_time = Time.now

# Create and populate array
arr = []
(0...SIZE).each do |i|
    arr << i * 2
end

# Sum all elements
total = arr.sum

end_time = Time.now

puts "Array size: #{SIZE}"
puts "Sum: #{total}"
puts "Time: #{((end_time - start_time) * 1000).round(2)}ms"