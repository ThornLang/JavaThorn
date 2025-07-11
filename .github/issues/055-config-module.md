# Implement Configuration Management Module for ThornLang Standard Library

## Overview
Implement a configuration management module (`config.thorn`) for parsing configuration files and handling environment variables in ThornLang. This module will be implemented in pure Thorn to provide flexible configuration management across different file formats.

## Technical Requirements

### Configuration Parsing Functions
- [ ] `parseConfig(configString)` - Parse generic configuration string
- [ ] `parseJSON(jsonString)` - Parse JSON configuration
- [ ] `parseYAML(yamlString)` - Parse YAML configuration
- [ ] `parseINI(iniString)` - Parse INI configuration format
- [ ] `parseTOML(tomlString)` - Parse TOML configuration
- [ ] `parseProperties(propString)` - Parse Java properties format
- [ ] `parseEnv(envString)` - Parse .env file format

### File Operations
- [ ] `loadConfigFile(filename)` - Load configuration from file
- [ ] `saveConfig(config, filename)` - Save configuration to file
- [ ] `watchConfig(filename, callback)` - Watch for configuration changes
- [ ] `reloadConfig(filename)` - Reload configuration from file

### Configuration Management
- [ ] `getConfigValue(config, key, defaultValue)` - Get value with default
- [ ] `setConfigValue(config, key, value)` - Set configuration value
- [ ] `hasConfigValue(config, key)` - Check if key exists
- [ ] `deleteConfigValue(config, key)` - Remove configuration key
- [ ] `mergeConfigs(config1, config2)` - Deep merge configurations
- [ ] `flattenConfig(config)` - Flatten nested configuration
- [ ] `unflattenConfig(flatConfig)` - Unflatten to nested structure

### Environment Integration
- [ ] `loadEnv()` - Load environment variables
- [ ] `expandVars(config)` - Expand environment variables in config
- [ ] `substituteVars(template, vars)` - Variable substitution
- [ ] `getEnvOrDefault(key, defaultValue)` - Get env var with default

### Schema and Validation
- [ ] `validateConfig(config, schema)` - Validate against schema
- [ ] `applyDefaults(config, defaults)` - Apply default values
- [ ] `typeCoerce(config, schema)` - Coerce types based on schema
- [ ] `validateRequired(config, required)` - Check required fields

### Advanced Features
- [ ] `interpolateConfig(config)` - Interpolate values within config
- [ ] `encryptSensitive(config, keys)` - Encrypt sensitive values
- [ ] `decryptSensitive(config, keys)` - Decrypt sensitive values
- [ ] `diffConfigs(config1, config2)` - Show configuration differences

## Implementation Details

### Pure Thorn Implementation
```thorn
# Example implementation structure
export $ loadConfigFile(filename) {
    let content = readFile(filename);
    let extension = getFileExtension(filename);
    
    if (extension == "json") {
        return parseJSON(content);
    } else if (extension == "yaml" || extension == "yml") {
        return parseYAML(content);
    } else if (extension == "ini") {
        return parseINI(content);
    } else if (extension == "toml") {
        return parseTOML(content);
    } else if (extension == "env") {
        return parseEnv(content);
    }
    
    throw Error("Unsupported configuration format: " + extension);
}

export $ mergeConfigs(config1, config2) {
    let result = {};
    
    # Deep merge logic
    for (let key in config1) {
        if (isObject(config1[key]) && isObject(config2[key])) {
            result[key] = mergeConfigs(config1[key], config2[key]);
        } else {
            result[key] = config1[key];
        }
    }
    
    # Add keys only in config2
    for (let key in config2) {
        if (!(key in result)) {
            result[key] = config2[key];
        }
    }
    
    return result;
}
```

## Testing Requirements
- [ ] Unit tests for all parser functions
- [ ] Integration tests with real configuration files
- [ ] Edge case testing (malformed configs, circular references)
- [ ] Performance benchmarks for large configurations
- [ ] File watching and reload testing

## Documentation Requirements
- [ ] API documentation for all functions
- [ ] Configuration format guides
- [ ] Schema definition examples
- [ ] Best practices for configuration management
- [ ] Migration guide from other config systems

## References

### Python Standard Library Equivalents
- `configparser` - INI file parsing
- `json` - JSON parsing
- `os.environ` - Environment variables
- Third-party: `pyyaml`, `toml`, `python-dotenv`
- `argparse` - Command-line configuration

### Rust Standard Library Equivalents
- `serde` - Serialization framework for config parsing
- `toml` crate - TOML parsing
- `serde_json` - JSON parsing
- `serde_yaml` - YAML parsing
- `config` crate - Layered configuration management
- `dotenv` crate - Environment variable loading

## Acceptance Criteria
- [ ] Support for JSON, YAML, INI, TOML, and .env formats
- [ ] Environment variable expansion
- [ ] Configuration merging and inheritance
- [ ] Schema validation support
- [ ] File watching capabilities
- [ ] Comprehensive error handling
- [ ] Full test coverage

## Priority
Medium - Essential for application configuration management

## Labels
- stdlib
- configuration
- pure-thorn
- file-formats