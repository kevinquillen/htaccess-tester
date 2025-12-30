# Htaccess Tester

A JetBrains IDE plugin that tests `.htaccess` rewrite rules with instant offline evaluation.

![.htaccess tester screenshot in action](screenshot.png)

## Scope

This plugin provides an integrated way to test Apache `.htaccess` rewrite rules directly from your IDE. It evaluates your rules locally and displays the results, including:

- The final output URL after rewrites
- A step-by-step trace showing which rules matched
- Per-rule status (met, valid, reached)

### Features

- Test `.htaccess` rules against any URL
- Offline evaluation - no internet required
- Support for custom server variables
- Read rules directly from open `.htaccess` files in the editor
- Save and reload test cases per project
- Filter and analyze rule evaluation results

## Usage

### Opening the Tool Window

- **Via Menu**: Go to **Tools** → **Htaccess Tester** → **Open Tool Window**
- **Via Tool Window Bar**: Click the "Htaccess Tester" tab at the bottom of your IDE

### Testing Rules

1. Enter the **URL** you want to test (e.g., `https://example.com/old-page`)
2. Enter your `.htaccess` **Rules** in the text area, or check "Use current .htaccess file" to read from an open editor
3. Optionally add **Server Variables** using the table (e.g., `HTTPS=on`, `HTTP_HOST=example.com`)
4. Click **Test**

Note: `RewriteEngine On` is assumed by default. You only need to include it if you want to explicitly test with `RewriteEngine Off`.

### Understanding Results

- **Result URL**: Shows the final URL after all rewrites are applied
- **Trace Table**: Shows each rule with its status and response
- **Filter**: Use the dropdown to show only failed, met, or reached rules
- **Copy Summary**: Copies a text summary to your clipboard

### Saving Test Cases

1. Enter your URL and rules
2. Click **Save**
3. Enter a name for your test case
4. Load saved cases from the dropdown

### Editor Integration

Right-click any `.htaccess` file in the editor and select **Test with Htaccess Tester** to automatically load its contents.

## Requirements

- **JetBrains IDE**: Compatible with IntelliJ IDEA 2024.1+, PhpStorm, WebStorm, and other JetBrains IDEs

## Troubleshooting

### Rules not evaluating as expected
- Check that server variables are set correctly
- Some Apache directives may not be supported (file system tests like `-f`, `-d` always return false in offline mode)

### Plugin not loading
Ensure you're using a compatible JetBrains IDE version (2024.1 or later).

## License

MIT License - see [LICENSE](LICENSE) for details.
