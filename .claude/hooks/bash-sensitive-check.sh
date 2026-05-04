#!/bin/bash
# PreToolUse hook for Bash. Force ASK when the command pipes data into a shell
# (curl|sh, wget|bash, etc.) — common supply-chain attack vector.

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // empty')

if [ -z "$cmd" ]; then
  exit 0
fi

# Match `| sh`, `| bash`, `| zsh`, `| ksh`, `| fish` (with optional flags / -)
if printf '%s' "$cmd" | grep -qE '\|[[:space:]]*(sudo[[:space:]]+)?(/[^ |]*/)?(sh|bash|zsh|ksh|fish)([[:space:]]|$|;|\|)'; then
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask","permissionDecisionReason":"command pipes output into a shell — supply-chain risk, please confirm"}}'
fi

exit 0
