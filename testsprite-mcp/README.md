# TestSprite MCP Server

A comprehensive Model Context Protocol (MCP) server for testing and validation. TestSprite provides AI assistants with powerful testing capabilities including test generation, execution, validation, and reporting.

## Features

### 🔧 Tools

- **Generate Test Cases**: Create comprehensive test suites for functions, APIs, and components
- **Execute Tests**: Run test suites with configurable parallel execution and timeouts
- **Generate Reports**: Create detailed test reports in multiple formats (JSON, HTML, Markdown)

### 📚 Resources

- **Test Templates**: Ready-to-use test templates for different frameworks (Jest, Cypress, etc.)

### 💡 Prompts

- **Test Planning Assistant**: Get help creating comprehensive test plans

## Installation

```bash
npm install
npm run build
```

## Usage

### As a Local Development Server

```bash
# Start the server via stdio transport
npm start

# Or test with MCP Inspector
npm run test
```

### As a Published Package

You can use this server through npx:

```bash
npx @testsprite/testsprite-mcp@latest
```

## Configuration

### Environment Variables

- `API_KEY`: Your TestSprite API key (optional, defaults to 'demo-key')

## Integration Examples

### Claude Desktop

Add to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "TestSprite": {
      "command": "npx",
      "args": ["@testsprite/testsprite-mcp@latest"],
      "env": {
        "API_KEY": "your-api-key"
      }
    }
  }
}
```

### VS Code with GitHub Copilot

Add to your `.vscode/mcp.json`:

```json
{
  "servers": {
    "TestSprite": {
      "type": "stdio",
      "command": "npx",
      "args": ["@testsprite/testsprite-mcp@latest"],
      "env": {
        "API_KEY": "your-api-key"
      }
    }
  }
}
```

## Example Workflows

### 1. Generate and Execute Tests

```typescript
// Use the generate_test_cases tool
{
  "target": "calculateTax",
  "testType": "unit",
  "coverage": "comprehensive",
  "framework": "jest"
}

// Then execute with execute_tests tool
{
  "suiteId": "generated-suite-id",
  "parallel": true,
  "timeout": 5000
}
```

### 2. Generate Test Report

```typescript
{
  "suiteId": "test-suite-id",
  "format": "markdown"
}
```

## API Reference

### Tools

#### `generate_test_cases`

Generates comprehensive test cases for testing targets.

**Parameters:**

- `target` (string): Function, API, or component to test
- `testType` ('unit' | 'integration' | 'e2e' | 'api'): Type of tests
- `coverage` ('basic' | 'comprehensive' | 'edge-cases'): Coverage level
- `framework` (string, optional): Testing framework

#### `execute_tests`

Executes a test suite with configurable options.

**Parameters:**

- `suiteId` (string): Test suite identifier
- `parallel` (boolean): Enable parallel execution
- `timeout` (number): Timeout per test in milliseconds

#### `generate_report`

Creates comprehensive test reports.

**Parameters:**

- `suiteId` (string): Test suite identifier
- `format` ('json' | 'html' | 'markdown'): Report format

### Resources

#### `test-templates`

Access to test templates for different frameworks and scenarios.

### Prompts

#### `test-planning`

Assists with creating comprehensive test plans.

**Parameters:**

- `project` (string): Project or feature to test
- `scope` (string, optional): Testing scope

## Development

```bash
# Development mode with auto-reload
npm run dev

# Build
npm run build

# Test the built server
npm run test
```

## License

MIT License
