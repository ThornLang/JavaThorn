# ThornLang Standard Library Architecture

## Executive Summary

ThornLang is a modern, dynamically-typed programming language with dual execution models (tree-walking interpreter and register-based VM) that achieves competitive performance. The language features clean syntax with `$` function definitions, immutable variables (`@immut`), lambda functions, classes, and comprehensive data structures.

**Current Status:**
- Core language implementation complete with feature parity between interpreter and VM
- Performance optimized (2.4x slower than Python, down from 7.4x)  
- Module system infrastructure in place (`ModuleSystem` class with import/export)
- Standard library previously deleted - requires complete reconstruction
- Built-in functions: `print()`, `clock()`, array methods (`push`, `pop`, `shift`, `unshift`)

**Architecture Requirements:**
- Leverage existing Java-Thorn interop patterns via `ThornCallable` interface
- Maintain compatibility with both interpreter and VM execution modes
- Utilize module system with search paths and circular dependency detection
- Balance performance (Java natives) with accessibility (Thorn implementations)

---

## Pure Thorn Modules

Pure Thorn modules are implemented entirely in Thorn syntax, offering maximum accessibility for developers and serving as reference implementations. These modules leverage the language's native features and existing built-ins.

### Collections (`collections.thorn`)

**Purpose:** Extended collection utilities beyond basic array operations  
**Implementation:** Pure Thorn using existing `List` and `Dict` types  
**Integration:** Direct import via module system

**Technical Requirements:**
- Must work with existing Thorn array and dictionary types
- Support for function chaining and composition
- Immutable operation semantics (return new collections)
- Performance optimized for common operations
- Compatible with lambda expressions and closures

**Feature List:**
- `map(list, func)` - Transform elements with function
- `filter(list, predicate)` - Filter elements by predicate
- `reduce(list, func, initial)` - Reduce to single value
- `zip(list1, list2)` - Combine two lists element-wise
- `flatten(nestedList)` - Flatten nested arrays
- `unique(list)` - Remove duplicate elements
- `partition(list, predicate)` - Split into two lists
- `sort(list, compareFn)` - Sort with custom comparator
- `reverse(list)` - Reverse order of elements
- `take(list, n)` - Take first n elements
- `drop(list, n)` - Drop first n elements
- `chunk(list, size)` - Split into chunks of size
- `intersect(list1, list2)` - Find common elements
- `union(list1, list2)` - Combine unique elements
- `difference(list1, list2)` - Elements in first but not second

### Functional Programming (`functional.thorn`)

**Purpose:** Higher-order functions and functional programming utilities  
**Implementation:** Pure Thorn with lambda expressions  
**Integration:** Standalone module with utility functions

**Technical Requirements:**
- Support for currying and partial application
- Function composition capabilities
- Memoization for performance optimization
- Support for variadic arguments
- Compatible with existing function syntax

**Feature List:**
- `compose(f, g)` - Function composition
- `curry(func)` - Convert to curried function
- `partial(func, ...args)` - Partial application
- `memoize(func)` - Add memoization to function
- `debounce(func, delay)` - Debounce function calls
- `throttle(func, interval)` - Throttle function calls
- `pipe(...functions)` - Pipeline functions left-to-right
- `flip(func)` - Flip argument order
- `constant(value)` - Always return same value
- `identity(value)` - Return input unchanged
- `negate(predicate)` - Negate predicate function
- `once(func)` - Execute function only once
- `times(n, func)` - Execute function n times
- `trampoline(func)` - Tail-call optimization helper

### Testing Framework (`test.thorn`)

**Purpose:** Unit testing and assertion framework  
**Implementation:** Pure Thorn with detailed error reporting  
**Integration:** Standalone testing utilities

**Technical Requirements:**
- Comprehensive assertion library
- Test suite organization and execution
- Detailed error reporting and stack traces
- Performance timing and benchmarking
- Support for async testing (future enhancement)

**Feature List:**
- `assert(condition, message)` - Basic assertion
- `assertEqual(expected, actual, message)` - Equality assertion
- `assertNotEqual(expected, actual, message)` - Inequality assertion
- `assertNull(value, message)` - Null assertion
- `assertNotNull(value, message)` - Not null assertion
- `assertThrows(func, expectedError)` - Exception assertion
- `assertType(value, type, message)` - Type assertion
- `describe(name, tests)` - Test suite organization
- `before(setupFunc)` - Setup function
- `after(cleanupFunc)` - Cleanup function
- `skip(reason)` - Skip test execution
- `benchmark(name, func, iterations)` - Performance benchmarking
- `mock(object, method, replacement)` - Mock functionality
- `spy(object, method)` - Function call tracking

### Configuration Management (`config.thorn`)

**Purpose:** Configuration file parsing and environment variable handling  
**Implementation:** Pure Thorn with dict-based configuration  
**Integration:** File I/O operations and environment access

**Technical Requirements:**
- Multiple configuration file format support
- Environment variable integration
- Hierarchical configuration merging
- Type conversion and validation
- Default value handling

**Feature List:**
- `parseConfig(configString)` - Parse configuration string
- `loadConfigFile(filename)` - Load from file
- `getConfigValue(config, key, defaultValue)` - Get configuration value
- `setConfigValue(config, key, value)` - Set configuration value
- `mergeConfigs(config1, config2)` - Merge configurations
- `validateConfig(config, schema)` - Validate against schema
- `saveConfig(config, filename)` - Save to file
- `watchConfig(filename, callback)` - Watch for changes
- `expandVars(config)` - Expand environment variables
- `parseJSON(jsonString)` - Parse JSON configuration
- `parseYAML(yamlString)` - Parse YAML configuration
- `parseProperties(propString)` - Parse Java properties format

### Validation (`validation.thorn`)

**Purpose:** Data validation and type checking utilities  
**Implementation:** Pure Thorn with comprehensive validators  
**Integration:** Standalone validation functions

**Technical Requirements:**
- Type checking and validation functions
- Custom validator composition
- Detailed error messages
- Support for nested object validation
- Performance optimized for common cases

**Feature List:**
- `isString(value)` - String type check
- `isNumber(value)` - Number type check
- `isBoolean(value)` - Boolean type check
- `isArray(value)` - Array type check
- `isObject(value)` - Object type check
- `isFunction(value)` - Function type check
- `isEmail(email)` - Email format validation
- `isURL(url)` - URL format validation
- `isPhone(phone)` - Phone number validation
- `validateRequired(value, fieldName)` - Required field validation
- `validateRange(value, min, max, fieldName)` - Range validation
- `validateLength(value, minLen, maxLen, fieldName)` - Length validation
- `validatePattern(value, regex, fieldName)` - Pattern validation
- `validateSchema(object, schema)` - Schema validation
- `createValidator(rules)` - Custom validator creation

### String Utilities (`string.thorn`)

**Purpose:** Extended string manipulation beyond basic operators  
**Implementation:** Pure Thorn building on existing string operations  
**Integration:** Standalone string utility functions

**Technical Requirements:**
- Unicode-aware string operations
- Performance optimized for common operations
- Consistent API with existing string methods
- Support for regular expressions (when available)
- Null-safe operations

**Feature List:**
- `capitalize(str)` - Capitalize first letter
- `upperCase(str)` - Convert to uppercase
- `lowerCase(str)` - Convert to lowercase
- `reverse(str)` - Reverse string
- `padLeft(str, length, char)` - Pad left with character
- `padRight(str, length, char)` - Pad right with character
- `trim(str)` - Remove whitespace
- `trimLeft(str)` - Remove left whitespace
- `trimRight(str)` - Remove right whitespace
- `words(str)` - Split into words
- `lines(str)` - Split into lines
- `repeat(str, count)` - Repeat string n times
- `truncate(str, length)` - Truncate to length
- `slugify(str)` - Convert to URL-safe slug
- `escape(str)` - Escape special characters
- `unescape(str)` - Unescape special characters
- `interpolate(template, values)` - String interpolation
- `similarity(str1, str2)` - String similarity score
- `distance(str1, str2)` - Edit distance

### Template Engine (`template.thorn`)

**Purpose:** Simple template processing for string interpolation  
**Implementation:** Pure Thorn with placeholder replacement  
**Integration:** String utilities and expression evaluation

**Technical Requirements:**
- Template syntax parsing and evaluation
- Variable substitution and expression evaluation
- Conditional rendering and loops
- Partial template support
- Custom helper function support

**Feature List:**
- `render(template, data)` - Basic template rendering
- `renderFile(filename, data)` - Render template from file
- `compile(template)` - Compile template for reuse
- `registerHelper(name, func)` - Register helper function
- `registerPartial(name, template)` - Register partial template
- `conditionalRender(template, data, helpers)` - Conditional rendering
- `loopRender(template, data, helpers)` - Loop rendering
- `escapeHTML(str)` - HTML escaping
- `safeString(str)` - Mark string as safe
- `templateCache` - Template caching system
- `syntax` - Template syntax configuration

### Algorithm Implementations (`algorithms.thorn`)

**Purpose:** Common algorithms implemented in pure Thorn  
**Implementation:** Educational and practical algorithm implementations  
**Integration:** Standalone algorithm library

**Technical Requirements:**
- Efficient algorithm implementations
- Educational value with clear code
- Performance benchmarking capabilities
- Support for custom comparison functions
- Memory efficient implementations

**Feature List:**
- `quicksort(arr, compareFn)` - Quick sort algorithm
- `mergesort(arr, compareFn)` - Merge sort algorithm
- `heapsort(arr, compareFn)` - Heap sort algorithm
- `binarySearch(arr, target, compareFn)` - Binary search
- `linearSearch(arr, target)` - Linear search
- `bfs(graph, start)` - Breadth-first search
- `dfs(graph, start)` - Depth-first search
- `dijkstra(graph, start)` - Shortest path algorithm
- `factorial(n)` - Factorial calculation
- `fibonacci(n)` - Fibonacci sequence
- `gcd(a, b)` - Greatest common divisor
- `lcm(a, b)` - Least common multiple
- `isPrime(n)` - Prime number check
- `primes(n)` - Generate prime numbers
- `permutations(arr)` - Generate permutations
- `combinations(arr, k)` - Generate combinations

---

## Pure Java Modules

Pure Java modules provide maximum performance for system-level operations and computationally intensive tasks. These modules integrate with Thorn via the `ThornCallable` interface and are registered as native functions.

### File I/O (`io.thorn` - Java Native)

**Purpose:** High-performance file operations with proper error handling  
**Implementation:** Java NIO with ThornCallable wrappers  
**Integration:** Native functions registered in global environment

**Technical Requirements:**
- Java NIO for high-performance file operations
- Proper error handling and resource management
- Support for both synchronous and asynchronous operations
- Path manipulation and file system operations
- Cross-platform compatibility

**Feature List:**
- `readFile(filename)` - Read entire file as string
- `writeFile(filename, content)` - Write string to file
- `appendFile(filename, content)` - Append to file
- `readBytes(filename)` - Read file as byte array
- `writeBytes(filename, bytes)` - Write bytes to file
- `fileExists(filename)` - Check if file exists
- `deleteFile(filename)` - Delete file
- `copyFile(source, destination)` - Copy file
- `moveFile(source, destination)` - Move file
- `fileSize(filename)` - Get file size
- `fileModified(filename)` - Get last modified time
- `createDirectory(path)` - Create directory
- `listDirectory(path)` - List directory contents
- `walkDirectory(path, callback)` - Walk directory tree
- `watchFile(filename, callback)` - Watch file for changes
- `tempFile(prefix, suffix)` - Create temporary file
- `lockFile(filename)` - File locking
- `unlockFile(filename)` - File unlocking

### Networking (`net.thorn` - Java Native)

**Purpose:** TCP/UDP socket programming and HTTP client functionality  
**Implementation:** Java NIO and HTTP client APIs  
**Integration:** Native functions for network operations

**Technical Requirements:**
- Java NIO for high-performance networking
- HTTP client implementation with Java 11+ HttpClient
- WebSocket support for real-time communication
- SSL/TLS support for secure connections
- Connection pooling and management

**Feature List:**
- `httpGet(url, headers)` - HTTP GET request
- `httpPost(url, body, headers)` - HTTP POST request
- `httpPut(url, body, headers)` - HTTP PUT request
- `httpDelete(url, headers)` - HTTP DELETE request
- `httpHead(url, headers)` - HTTP HEAD request
- `downloadFile(url, filename)` - Download file
- `uploadFile(url, filename, headers)` - Upload file
- `createWebSocket(url)` - Create WebSocket connection
- `tcpConnect(host, port)` - TCP connection
- `tcpListen(port, callback)` - TCP server
- `udpSocket(port)` - UDP socket
- `dnsLookup(hostname)` - DNS resolution
- `ping(host)` - Ping host
- `getLocalIP()` - Get local IP address
- `getPublicIP()` - Get public IP address
- `parseURL(url)` - Parse URL components
- `encodeURL(url)` - URL encoding
- `decodeURL(url)` - URL decoding

### Concurrency (`concurrent.thorn` - Java Native)

**Purpose:** Thread management and concurrent operations  
**Implementation:** Java ExecutorService and CompletableFuture  
**Integration:** Native thread management functions

**Technical Requirements:**
- Java ExecutorService for thread pool management
- CompletableFuture for asynchronous operations
- Thread-safe data structures and operations
- Deadlock detection and prevention
- Resource cleanup and lifecycle management

**Feature List:**
- `async(task)` - Execute task asynchronously
- `await(future)` - Wait for future completion
- `parallel(tasks)` - Execute tasks in parallel
- `sleep(milliseconds)` - Sleep current thread
- `createLock()` - Create reentrant lock
- `createSemaphore(permits)` - Create semaphore
- `createCountDownLatch(count)` - Create countdown latch
- `createBlockingQueue(capacity)` - Create blocking queue
- `createThreadPool(size)` - Create thread pool
- `getCurrentThread()` - Get current thread info
- `setThreadName(name)` - Set thread name
- `threadDump()` - Get thread dump
- `createScheduler()` - Create scheduled executor
- `schedule(task, delay)` - Schedule task
- `scheduleRepeating(task, interval)` - Schedule repeating task
- `race(futures)` - Race multiple futures
- `timeout(future, milliseconds)` - Add timeout to future

### JSON Processing (`json.thorn` - Java Native)

**Purpose:** High-performance JSON parsing and serialization  
**Implementation:** Minimal JSON parser or Jackson integration  
**Integration:** Native JSON functions

**Technical Requirements:**
- High-performance JSON parsing and serialization
- Support for all JSON data types
- Error handling for malformed JSON
- Streaming support for large JSON files
- Custom serialization for Thorn objects

**Feature List:**
- `parseJSON(jsonString)` - Parse JSON string
- `toJSON(value)` - Convert to JSON string
- `parseJSONStream(inputStream)` - Parse JSON stream
- `toJSONStream(value, outputStream)` - Write JSON stream
- `validateJSON(jsonString)` - Validate JSON syntax
- `formatJSON(jsonString)` - Format JSON string
- `minifyJSON(jsonString)` - Minify JSON string
- `jsonPath(json, path)` - Extract value by path
- `jsonMerge(json1, json2)` - Merge JSON objects
- `jsonDiff(json1, json2)` - Compare JSON objects
- `jsonPatch(json, patch)` - Apply JSON patch
- `jsonSchema(schema, json)` - Validate against schema
- `jsonTransform(json, transformer)` - Transform JSON

### System Interface (`system.thorn` - Java Native)

**Purpose:** System calls, process management, and environment access  
**Implementation:** Java ProcessBuilder and System APIs  
**Integration:** Native system operation functions

**Technical Requirements:**
- Process execution and management
- Environment variable access and modification
- System property access
- Signal handling capabilities
- Cross-platform system information

**Feature List:**
- `exec(command, args)` - Execute system command
- `spawn(command, args)` - Spawn process
- `kill(pid, signal)` - Kill process
- `getpid()` - Get current process ID
- `getppid()` - Get parent process ID
- `getenv(name)` - Get environment variable
- `setenv(name, value)` - Set environment variable
- `unsetenv(name)` - Unset environment variable
- `listenv()` - List all environment variables
- `getProperty(name)` - Get system property
- `setProperty(name, value)` - Set system property
- `getUser()` - Get current user
- `getHostname()` - Get hostname
- `getOS()` - Get operating system
- `getArch()` - Get architecture
- `getCPUCount()` - Get CPU count
- `getMemoryInfo()` - Get memory information
- `exit(code)` - Exit with code
- `addShutdownHook(callback)` - Add shutdown hook

### Cryptography (`crypto.thorn` - Java Native)

**Purpose:** Cryptographic operations and hashing  
**Implementation:** Java security APIs  
**Integration:** Native cryptographic functions

**Technical Requirements:**
- Secure hashing algorithms (SHA-256, SHA-512, etc.)
- Symmetric encryption (AES, DES)
- Asymmetric encryption (RSA, DSA)
- Digital signatures and verification
- Secure random number generation

**Feature List:**
- `hash(algorithm, data)` - Hash data with algorithm
- `hmac(algorithm, key, data)` - HMAC generation
- `encrypt(algorithm, key, data)` - Symmetric encryption
- `decrypt(algorithm, key, data)` - Symmetric decryption
- `generateKey(algorithm, keySize)` - Generate symmetric key
- `generateKeyPair(algorithm, keySize)` - Generate key pair
- `sign(algorithm, privateKey, data)` - Digital signature
- `verify(algorithm, publicKey, signature, data)` - Verify signature
- `pbkdf2(password, salt, iterations, keyLength)` - Key derivation
- `scrypt(password, salt, n, r, p, keyLength)` - Scrypt key derivation
- `bcrypt(password, rounds)` - Bcrypt password hashing
- `verifyBcrypt(password, hash)` - Verify bcrypt hash
- `secureRandom(bytes)` - Generate secure random bytes
- `uuid()` - Generate UUID
- `checksum(algorithm, data)` - Calculate checksum

### Random Number Generation (`random.thorn` - Java Native)

**Purpose:** High-quality random number generation  
**Implementation:** Java SecureRandom and ThreadLocalRandom  
**Integration:** Native random functions

**Technical Requirements:**
- Thread-safe random number generation
- Cryptographically secure random numbers
- Support for various distributions
- Seeding capabilities for reproducibility
- High-performance random operations

**Feature List:**
- `random()` - Random double [0, 1)
- `randomInt(min, max)` - Random integer in range
- `randomLong(min, max)` - Random long in range
- `randomFloat(min, max)` - Random float in range
- `randomDouble(min, max)` - Random double in range
- `randomBoolean()` - Random boolean
- `randomBytes(length)` - Random byte array
- `randomString(length, charset)` - Random string
- `randomChoice(array)` - Random element from array
- `randomSample(array, count)` - Random sample from array
- `shuffle(array)` - Shuffle array in place
- `secureRandom()` - Cryptographically secure random
- `secureRandomInt(min, max)` - Secure random integer
- `secureRandomBytes(length)` - Secure random bytes
- `seed(value)` - Set random seed
- `gaussian(mean, stddev)` - Gaussian distribution
- `exponential(lambda)` - Exponential distribution
- `uniform(min, max)` - Uniform distribution

### Compression (`compression.thorn` - Java Native)

**Purpose:** Data compression and decompression  
**Implementation:** Java compression APIs  
**Integration:** Native compression functions

**Technical Requirements:**
- Multiple compression algorithms (GZIP, DEFLATE, etc.)
- Streaming compression for large data
- Compression level control
- Error handling for corrupted data
- Memory efficient operations

**Feature List:**
- `compress(data, algorithm)` - Compress data
- `decompress(data, algorithm)` - Decompress data
- `gzipCompress(data)` - GZIP compression
- `gzipDecompress(data)` - GZIP decompression
- `deflateCompress(data)` - DEFLATE compression
- `deflateDecompress(data)` - DEFLATE decompression
- `zipCreate(entries)` - Create ZIP archive
- `zipExtract(zipData)` - Extract ZIP archive
- `zipList(zipData)` - List ZIP contents
- `compressFile(filename, algorithm)` - Compress file
- `decompressFile(filename, algorithm)` - Decompress file
- `compressionRatio(original, compressed)` - Calculate ratio
- `supportedAlgorithms()` - List supported algorithms
- `setCompressionLevel(level)` - Set compression level
- `streamCompress(inputStream, outputStream)` - Stream compression
- `streamDecompress(inputStream, outputStream)` - Stream decompression

---

## Mixed Thorn + Java Modules

Mixed modules combine Thorn's expressiveness with Java's performance, providing the best of both worlds. The Java layer handles performance-critical operations while Thorn provides the API and higher-level functionality.

### HTTP Server (`http.thorn` - Mixed)

**Purpose:** HTTP server with middleware support  
**Implementation:** Java NIO server with Thorn routing and middleware  
**Integration:** Java foundation with Thorn API layer

**Technical Requirements:**
- Java NIO HTTP server for performance
- Thorn-based routing and middleware system
- Request/response object abstraction
- Static file serving capabilities
- WebSocket upgrade support

**Feature List:**
- `Server(port)` - Create HTTP server
- `get(path, handler)` - Register GET route
- `post(path, handler)` - Register POST route
- `put(path, handler)` - Register PUT route
- `delete(path, handler)` - Register DELETE route
- `use(middleware)` - Add middleware
- `static(path, directory)` - Serve static files
- `listen(callback)` - Start server
- `stop()` - Stop server
- `json(data)` - JSON response helper
- `html(content)` - HTML response helper
- `redirect(url)` - Redirect response
- `status(code)` - Set status code
- `header(name, value)` - Set response header
- `cookie(name, value, options)` - Set cookie
- `session(config)` - Session management
- `cors(options)` - CORS middleware
- `compress()` - Compression middleware
- `rateLimit(options)` - Rate limiting middleware

### Database Connectivity (`database.thorn` - Mixed)

**Purpose:** Database operations with connection pooling  
**Implementation:** Java JDBC with Thorn query builder  
**Integration:** Java connection management with Thorn API

**Technical Requirements:**
- JDBC connection pooling for performance
- SQL query builder in Thorn
- Transaction support and management
- Multiple database support (MySQL, PostgreSQL, SQLite)
- Connection lifecycle management

**Feature List:**
- `connect(connectionString)` - Database connection
- `query(sql, params)` - Execute SQL query
- `select(table, conditions)` - Select records
- `insert(table, data)` - Insert record
- `update(table, data, conditions)` - Update records
- `delete(table, conditions)` - Delete records
- `createTable(name, schema)` - Create table
- `dropTable(name)` - Drop table
- `beginTransaction()` - Start transaction
- `commit()` - Commit transaction
- `rollback()` - Rollback transaction
- `close()` - Close connection
- `migrate(migrations)` - Run migrations
- `seed(data)` - Seed database
- `backup(filename)` - Backup database
- `restore(filename)` - Restore database
- `execute(sql)` - Execute raw SQL
- `batch(queries)` - Execute batch queries
- `prepare(sql)` - Prepare statement

### Mathematics (`math.thorn` - Mixed)

**Purpose:** Mathematical operations with native optimizations  
**Implementation:** Java native functions with Thorn utilities  
**Integration:** Java math functions with Thorn mathematical utilities

**Technical Requirements:**
- Java Math class integration for performance
- Thorn implementations for complex operations
- Vector and matrix operations
- Statistical functions and distributions
- Numerical analysis capabilities

**Feature List:**
- `sin(x)` - Sine function
- `cos(x)` - Cosine function
- `tan(x)` - Tangent function
- `asin(x)` - Arc sine
- `acos(x)` - Arc cosine
- `atan(x)` - Arc tangent
- `atan2(y, x)` - Arc tangent of y/x
- `sqrt(x)` - Square root
- `pow(base, exp)` - Power function
- `exp(x)` - Exponential function
- `log(x)` - Natural logarithm
- `log10(x)` - Base-10 logarithm
- `abs(x)` - Absolute value
- `floor(x)` - Floor function
- `ceil(x)` - Ceiling function
- `round(x)` - Round function
- `min(a, b)` - Minimum value
- `max(a, b)` - Maximum value
- `clamp(x, min, max)` - Clamp value
- `degrees(radians)` - Convert to degrees
- `radians(degrees)` - Convert to radians
- `factorial(n)` - Factorial function
- `fibonacci(n)` - Fibonacci number
- `gcd(a, b)` - Greatest common divisor
- `lcm(a, b)` - Least common multiple
- `isPrime(n)` - Prime number check
- `Vector(x, y, z)` - 3D vector operations
- `Matrix(rows, cols)` - Matrix operations
- `mean(array)` - Calculate mean
- `median(array)` - Calculate median
- `mode(array)` - Calculate mode
- `stddev(array)` - Standard deviation
- `variance(array)` - Variance
- `correlation(x, y)` - Correlation coefficient

### Image Processing (`image.thorn` - Mixed)

**Purpose:** Image manipulation and processing  
**Implementation:** Java BufferedImage with Thorn filters  
**Integration:** Java image operations with Thorn filter system

**Technical Requirements:**
- Java BufferedImage for high-performance operations
- Thorn-based filter system for extensibility
- Support for common image formats (JPEG, PNG, GIF)
- Pixel-level operations and transformations
- Memory efficient image processing

**Feature List:**
- `loadImage(filename)` - Load image from file
- `saveImage(image, filename)` - Save image to file
- `createImage(width, height)` - Create blank image
- `resize(image, width, height)` - Resize image
- `crop(image, x, y, width, height)` - Crop image
- `rotate(image, angle)` - Rotate image
- `flip(image, direction)` - Flip image
- `blur(image, radius)` - Blur effect
- `sharpen(image)` - Sharpen effect
- `brightness(image, factor)` - Adjust brightness
- `contrast(image, factor)` - Adjust contrast
- `saturation(image, factor)` - Adjust saturation
- `grayscale(image)` - Convert to grayscale
- `sepia(image)` - Sepia tone effect
- `invert(image)` - Invert colors
- `threshold(image, value)` - Threshold filter
- `edge(image)` - Edge detection
- `emboss(image)` - Emboss effect
- `histogram(image)` - Color histogram
- `composite(image1, image2, mode)` - Composite images
- `getPixel(image, x, y)` - Get pixel value
- `setPixel(image, x, y, color)` - Set pixel value
- `getInfo(image)` - Get image information

### Audio Processing (`audio.thorn` - Mixed)

**Purpose:** Audio file processing and manipulation  
**Implementation:** Java Sound API with Thorn effects  
**Integration:** Java audio operations with Thorn effects system

**Technical Requirements:**
- Java Sound API for audio file operations
- Thorn-based effects system for audio processing
- Support for common audio formats (WAV, MP3, OGG)
- Real-time audio processing capabilities
- Digital signal processing functions

**Feature List:**
- `loadAudio(filename)` - Load audio file
- `saveAudio(audio, filename)` - Save audio file
- `createAudio(duration, sampleRate)` - Create blank audio
- `play(audio)` - Play audio
- `pause(audio)` - Pause playback
- `stop(audio)` - Stop playback
- `volume(audio, factor)` - Adjust volume
- `fade(audio, type, duration)` - Fade in/out
- `normalize(audio)` - Normalize audio
- `trim(audio, start, end)` - Trim audio
- `concatenate(audio1, audio2)` - Concatenate audio
- `mix(audio1, audio2)` - Mix audio tracks
- `echo(audio, delay, decay)` - Echo effect
- `reverb(audio, roomSize, damping)` - Reverb effect
- `filter(audio, type, frequency)` - Audio filter
- `distortion(audio, amount)` - Distortion effect
- `chorus(audio, rate, depth)` - Chorus effect
- `flanger(audio, rate, depth)` - Flanger effect
- `pitch(audio, semitones)` - Pitch shift
- `tempo(audio, factor)` - Tempo change
- `spectrum(audio)` - Frequency spectrum
- `waveform(audio)` - Waveform data
- `getInfo(audio)` - Get audio information

### CSV Processing (`csv.thorn` - Mixed)

**Purpose:** CSV file parsing and generation  
**Implementation:** Java stream processing with Thorn data manipulation  
**Integration:** Java CSV parser with Thorn data operations

**Technical Requirements:**
- High-performance CSV parsing with Java streams
- Thorn-based data manipulation and transformation
- Support for custom delimiters and escape characters
- Memory efficient processing of large CSV files
- Data validation and type conversion

**Feature List:**
- `parseCSV(csvString)` - Parse CSV string
- `loadCSV(filename)` - Load CSV from file
- `saveCSV(data, filename)` - Save data to CSV
- `toObjects(csv)` - Convert to object array
- `fromObjects(objects)` - Convert from object array
- `filter(csv, predicate)` - Filter rows
- `map(csv, transformer)` - Transform rows
- `sort(csv, column)` - Sort by column
- `group(csv, column)` - Group by column
- `aggregate(csv, functions)` - Aggregate functions
- `join(csv1, csv2, key)` - Join CSV data
- `pivot(csv, rowKey, colKey, valueKey)` - Pivot table
- `unpivot(csv, columns)` - Unpivot data
- `validate(csv, schema)` - Validate data
- `clean(csv, rules)` - Clean data
- `sample(csv, count)` - Random sample
- `head(csv, count)` - First n rows
- `tail(csv, count)` - Last n rows
- `columns(csv)` - Get column names
- `rows(csv)` - Get row count
- `stats(csv, column)` - Column statistics
- `unique(csv, column)` - Unique values
- `frequency(csv, column)` - Value frequency

---

## Implementation Guidelines

### Module Registration Pattern

**For Pure Java Modules:**
- Register all native functions in `ThornInterpreter.initializeStandardLibrary()`
- Use consistent naming conventions for native functions
- Implement proper error handling with `RuntimeError`
- Ensure thread safety for concurrent operations
- Follow Java best practices for resource management

**For Mixed Modules:**
- Register Java foundation functions in global environment
- Place Thorn API layer in `stdlib/` directory
- Use standard module import mechanism for Thorn layer
- Maintain clear separation between Java and Thorn code
- Document the interface between Java and Thorn components

### Error Handling Standards

**Java Native Functions:**
- Always wrap Java exceptions in `RuntimeError`
- Provide meaningful error messages with context
- Handle resource cleanup in finally blocks
- Log errors appropriately for debugging
- Validate input parameters before processing

**Thorn Error Handling:**
- Use try-catch blocks for error handling
- Provide fallback values when appropriate
- Log errors with sufficient detail for debugging
- Maintain consistent error message format
- Handle null values gracefully

### Performance Considerations

**When to Use Each Approach:**

1. **Pure Thorn:**
   - Educational/reference implementations
   - Business logic and domain-specific operations
   - Rapid prototyping and development
   - When performance is not critical
   - When code readability is paramount

2. **Pure Java:**
   - I/O operations (file, network)
   - System-level operations
   - Computationally intensive tasks
   - When maximum performance is required
   - When using existing Java libraries

3. **Mixed Approach:**
   - Complex APIs with performance requirements
   - When you need both performance and expressiveness
   - Large modules with multiple concerns
   - When building frameworks or libraries
   - When extending existing Java functionality

### Module System Integration

**Import Resolution:**
- ModuleSystem searches in order: current directory, ./stdlib directory, THORN_PATH environment variable
- Modules are cached after first load to improve performance
- Circular dependency detection prevents infinite loops
- Export tracking via `ModuleEnvironment` class

**Best Practices:**
- Keep module interfaces simple and consistent
- Use descriptive export names that clearly indicate functionality
- Avoid circular dependencies between modules
- Provide comprehensive error messages with context
- Document module APIs thoroughly with examples
- Follow semantic versioning for module updates
- Write unit tests for all exported functions
- Optimize for common use cases while maintaining flexibility

---

## Migration Strategy

### Phase 1: Core Infrastructure (Week 1)
**Restore Basic Modules:**
- `io.thorn` - File operations, directory management
- `math.thorn` - Mathematical functions and utilities
- `string.thorn` - String manipulation and utilities
- `collections.thorn` - Collection operations and functional programming

**Set up Module Registration:**
- Modify main interpreter to initialize standard library
- Create module registration infrastructure
- Test module loading and import resolution
- Establish error handling patterns

### Phase 2: Essential Services (Week 2)
**System Integration:**
- `system.thorn` - System calls and process management
- `json.thorn` - JSON parsing and serialization
- `random.thorn` - Random number generation
- `crypto.thorn` - Basic cryptographic operations

**Testing Framework:**
- `test.thorn` - Unit testing framework
- Integration with existing examples
- Performance benchmarking utilities
- Test coverage reporting

### Phase 3: Advanced Features (Week 3)
**Network and I/O:**
- `http.thorn` - HTTP client and server
- `net.thorn` - Network operations
- `database.thorn` - Database connectivity
- `compression.thorn` - Data compression

**Concurrency:**
- `concurrent.thorn` - Threading and async operations
- Integration with VM execution model
- Performance optimization for concurrent workloads

### Phase 4: Specialized Modules (Week 4)
**Media Processing:**
- `image.thorn` - Image manipulation
- `audio.thorn` - Audio processing
- `csv.thorn` - CSV file processing

**Development Tools:**
- `config.thorn` - Configuration management
- `validation.thorn` - Data validation
- `template.thorn` - Template processing

### Implementation Priority

**High Priority (Essential):**
- File I/O, JSON, Math, String, Collections
- Testing framework for validation
- HTTP client for web development
- Basic system operations

**Medium Priority (Important):**
- System operations, Random, Crypto
- Database connectivity, Compression
- Template processing, Configuration
- Advanced string utilities

**Low Priority (Nice to Have):**
- Image/Audio processing
- Advanced concurrency features
- Specialized utilities
- Performance optimization tools

This architecture provides a comprehensive foundation for ThornLang's standard library while maintaining the language's performance characteristics and design principles. The mixed approach allows for optimal performance where needed while keeping the majority of the API accessible and maintainable in pure Thorn code.