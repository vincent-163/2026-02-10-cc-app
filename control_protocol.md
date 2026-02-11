# Claude Code Stream-JSON Control Protocol

How permissions are requested and granted/denied when using `claude` CLI with `--input-format stream-json --output-format stream-json`.

## Two modes

### Without `--permission-prompt-tool-name stdio`

CLI auto-denies permission requests. Emits a `type: "user"` message with an error tool_result:

```json
{
  "type": "user",
  "message": {
    "role": "user",
    "content": [{
      "type": "tool_result",
      "content": "Claude requested permissions to write to /path/file.txt, but you haven't granted it yet.",
      "is_error": true,
      "tool_use_id": "tooluse_xxx"
    }]
  }
}
```

Final `result` message includes `permission_denials` array. No way for client to approve.

### With `--permission-prompt-tool-name stdio`

Enables the control protocol. CLI sends `control_request` on stdout, expects `control_response` on stdin.

## Control Protocol Messages

### Permission request (CLI → client, stdout)

```json
{
  "type": "control_request",
  "request_id": "uuid",
  "request": {
    "subtype": "can_use_tool",
    "tool_name": "Write",
    "input": {
      "file_path": "/path/file.txt",
      "content": "hello world\n"
    },
    "permission_suggestions": [],
    "blocked_path": "/path/file.txt"
  }
}
```

### Allow response (client → CLI, stdin)

```json
{
  "type": "control_response",
  "response": {
    "subtype": "success",
    "request_id": "uuid",
    "response": {
      "behavior": "allow",
      "updatedInput": { "file_path": "/path/file.txt", "content": "hello world\n" }
    }
  }
}
```

`updatedInput` can modify the tool input (e.g. redirect file path). Pass original input to allow as-is.

Optional `updatedPermissions` array to add permanent permission rules:
```json
"updatedPermissions": [{ "tool": "Write", "path": "/path/*", "permission": "allow" }]
```

### Deny response (client → CLI, stdin)

```json
{
  "type": "control_response",
  "response": {
    "subtype": "success",
    "request_id": "uuid",
    "response": {
      "behavior": "deny",
      "message": "User denied permission"
    }
  }
}
```

Optional `"interrupt": true` to also stop the current task.

### Initialize (client → CLI, stdin; required at session start)

```json
{
  "type": "control_request",
  "request_id": "uuid",
  "request": {
    "subtype": "initialize",
    "hooks": null
  }
}
```

CLI responds with `control_response` confirming success. Must be sent before the first user message.

### Set permission mode (client → CLI, stdin)

```json
{
  "type": "control_request",
  "request_id": "uuid",
  "request": {
    "subtype": "set_permission_mode",
    "mode": "acceptEdits"
  }
}
```

Modes: `default`, `acceptEdits`, `bypassPermissions`, `plan`.

### Interrupt (client → CLI, stdin)

```json
{
  "type": "control_request",
  "request_id": "uuid",
  "request": {
    "subtype": "interrupt"
  }
}
```

### Generic response format (CLI → client, stdout)

Success:
```json
{
  "type": "control_response",
  "response": {
    "subtype": "success",
    "request_id": "uuid",
    "response": {}
  }
}
```

Error:
```json
{
  "type": "control_response",
  "response": {
    "subtype": "error",
    "request_id": "uuid",
    "error": "error message"
  }
}
```

## User messages (client → CLI, stdin)

```json
{
  "type": "user",
  "session_id": "",
  "message": { "role": "user", "content": "your prompt here" },
  "parent_tool_use_id": null
}
```

## Source

Derived from Agent SDK Python source (`claude-agent-sdk-python`), specifically:
- `src/claude_agent_sdk/_internal/query.py` — control protocol handling
- `src/claude_agent_sdk/_internal/client.py` — sets `permission_prompt_tool_name="stdio"` when `can_use_tool` callback is provided
- `src/claude_agent_sdk/types.py` — `SDKControlRequest`, `SDKControlResponse`, `SDKControlPermissionRequest` type definitions
