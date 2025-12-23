# Htaccess Tester

A JetBrains IDE plugin that tests `.htaccess` rewrite rules against a remote evaluation service.

## Scope

This plugin provides an integrated way to test Apache `.htaccess` rewrite rules directly from your IDE. It sends your rules and a test URL to the [htaccess.madewithlove.com](https://htaccess.madewithlove.com) API and displays the results, including:

- The final output URL after rewrites
- A step-by-step trace showing which rules matched
- Per-rule status (met, valid, reached)

### Features

- Test `.htaccess` rules against any URL
- Support for custom server variables
- Read rules directly from open `.htaccess` files in the editor
- Share test cases via URL
- Save and reload test cases per project

## Non-Goals

- **Local evaluation**: This plugin does not interpret `.htaccess` rules locally. All evaluation happens via a remote service.
- **Full Apache emulation**: The remote evaluator covers common rewrite scenarios but may not support every Apache module or directive.
- **Offline usage**: An internet connection is required.

## Requirements

- **Internet access**: This plugin requires connectivity to the remote evaluator at `htaccess.madewithlove.com`.
- **JetBrains IDE**: Compatible with IntelliJ IDEA, PhpStorm, WebStorm, and other JetBrains IDEs.

## Privacy Notice

When you use this plugin, the following data is sent to the remote evaluation service:

- The URL you want to test
- Your `.htaccess` rules
- Any custom server variables you configure

No data is stored locally beyond your saved test cases and plugin settings.

## Installation

*Coming soon - will be available via JetBrains Marketplace*

## License

MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgments

This plugin uses the [htaccess.madewithlove.com](https://htaccess.madewithlove.com) API for rule evaluation.
