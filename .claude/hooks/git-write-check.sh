#!/bin/bash
# PreToolUse hook for Write/Edit/NotebookEdit.
# Allow when the target file lives inside a git working tree, otherwise ask.

input=$(cat)
file_path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // .tool_input.notebook_path // empty')

if [ -z "$file_path" ]; then
  exit 0
fi

# Walk up to the nearest existing ancestor directory (handles new-file paths).
dir=$(dirname "$file_path")
while [ ! -d "$dir" ] && [ "$dir" != "/" ] && [ "$dir" != "." ]; do
  dir=$(dirname "$dir")
done

if (cd "$dir" 2>/dev/null && git rev-parse --is-inside-work-tree >/dev/null 2>&1); then
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow","permissionDecisionReason":"file is inside a git working tree"}}'
else
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask","permissionDecisionReason":"file is not inside any git repository — confirm before writing"}}'
fi
