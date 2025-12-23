# Sample Files

This directory contains sample files for demonstrating and testing the Htaccess Tester plugin.

## sample.htaccess

A sample `.htaccess` file with common rewrite patterns. Open this file in your IDE and use the plugin to test each rule.

### Test Scenarios

| Test URL | Expected Result | Description |
|----------|-----------------|-------------|
| `https://example.com/old-page` | Redirect 301 to `/new-page` | Simple redirect |
| `https://example.com/blog/123` | Rewrite to `/index.php?post=123` | Blog post URL pattern |
| `http://example.com/secure` | Redirect 301 to HTTPS | Force HTTPS |
| `https://example.com/api/users/42` | Rewrite to `/api.php?resource=users&id=42` | RESTful API pattern |
| `https://example.com/path/` | Redirect 301 to `/path` | Remove trailing slash |
| `https://example.com/any-page` | Rewrite to `/index.php?route=any-page` | Front controller |

### Server Variables

For the HTTPS redirect test, set:
- `HTTPS` = `off`

For other tests, you can set:
- `HTTP_HOST` = `example.com`
- `REQUEST_FILENAME` = (leave empty or set to a non-existent path)

## Using in the Plugin

1. Open `sample.htaccess` in your IDE
2. Right-click and select "Test with Htaccess Tester"
3. Enter one of the test URLs above
4. Configure server variables as needed
5. Click "Test" to see the results

## Saved Test Cases

Test cases are automatically saved per-project in `.idea/htaccess-tester.xml`. Use the Save button in the plugin to create reusable test cases for your project.
