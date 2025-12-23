# Htaccess Tester API Contract

This document describes the API contract for the htaccess.madewithlove.com service, discovered by inspecting the [madewithlove/htaccess-api-client](https://github.com/madewithlove/htaccess-api-client) PHP client source code.

## Base URL

```
https://htaccess.madewithlove.com/api
```

## Endpoints

### Test Rules

Tests `.htaccess` rewrite rules against a given URL.

**Endpoint:** `POST /api` (no trailing slash - the server returns 405 with a trailing slash)

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "url": "https://example.com/original-path",
  "htaccess": "RewriteEngine On\nRewriteRule ^old$ /new [R=301,L]",
  "serverVariables": {
    "HTTP_HOST": "example.com",
    "HTTPS": "on"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `url` | string | Yes | The URL to test against the rules |
| `htaccess` | string | Yes | The `.htaccess` file content |
| `serverVariables` | object | No | Key-value pairs for server variables |

**Response (Success - 200):**
```json
{
  "outputUrl": "https://example.com/new",
  "outputStatusCode": 301,
  "lines": [
    {
      "line": "RewriteEngine On",
      "message": "The rewrite engine is now enabled",
      "isMet": true,
      "isValid": true,
      "wasReached": true,
      "isSupported": true
    },
    {
      "line": "RewriteRule ^old$ /new [R=301,L]",
      "message": "The rule was met and redirects to /new with status 301",
      "isMet": true,
      "isValid": true,
      "wasReached": true,
      "isSupported": true
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `outputUrl` | string | The final URL after applying all rules |
| `outputStatusCode` | int \| null | HTTP status code if a redirect occurs |
| `lines` | array | Per-line evaluation results |

**ResultLine Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `line` | string | The original rule line |
| `message` | string | Human-readable explanation of what happened |
| `isMet` | boolean | Whether the rule's condition was satisfied |
| `isValid` | boolean | Whether the rule syntax is valid |
| `wasReached` | boolean | Whether evaluation reached this line |
| `isSupported` | boolean | Whether this directive is supported by the tester |

---

### Share Test Case

Creates a shareable link for a test configuration.

**Endpoint:** `POST /share`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "url": "https://example.com/test",
  "htaccess": "RewriteRule ^test$ /result [L]",
  "serverVariables": {}
}
```

**Response (Success - 200):**
```json
{
  "shareUrl": "https://htaccess.madewithlove.com/?share=abc123-uuid"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `shareUrl` | string | The URL to share this test configuration |

---

### Retrieve Shared Test Case

Retrieves a previously shared test configuration.

**Endpoint:** `GET /share?share={uuid}`

**Response:** Returns the saved test configuration (structure TBD based on actual usage).

---

## Error Responses

### Validation Error (400)
```json
{
  "error": "Validation failed",
  "details": "URL is required"
}
```

### Rate Limit (429)
```json
{
  "error": "Rate limit exceeded",
  "details": "Please wait before making another request"
}
```

### Server Error (5xx)
```json
{
  "error": "Internal server error",
  "details": "An unexpected error occurred"
}
```

---

## Example cURL Requests

### Test Request
```bash
curl -X POST https://htaccess.madewithlove.com/api \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com/old-page",
    "htaccess": "RewriteEngine On\nRewriteRule ^old-page$ /new-page [R=301,L]",
    "serverVariables": {}
  }'
```

### Share Request
```bash
curl -X POST https://htaccess.madewithlove.com/api/share \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com/test",
    "htaccess": "RewriteRule ^test$ /result [L]",
    "serverVariables": {}
  }'
```

---

## Notes

- The API does not require authentication
- Server variables are optional but useful for testing conditional rules
- The `isSupported` field indicates whether the tester can evaluate a particular directive
- Empty `serverVariables` should be sent as an empty object `{}`, not omitted
