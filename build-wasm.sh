#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PNG_GLITCH_DIR="${PNG_GLITCH_DIR:-$SCRIPT_DIR/third_party/png-glitch}"
WASM_TOOLS_BIN="${WASM_TOOLS_BIN:-wasm-tools}"
if ! command -v "$WASM_TOOLS_BIN" >/dev/null 2>&1 && [[ -x "$HOME/.cargo/bin/wasm-tools" ]]; then
  WASM_TOOLS_BIN="$HOME/.cargo/bin/wasm-tools"
fi

if ! command -v "$WASM_TOOLS_BIN" >/dev/null 2>&1; then
  echo "error: wasm-tools is required (set WASM_TOOLS_BIN or install wasm-tools)" >&2
  exit 1
fi

cargo build --manifest-path "$PNG_GLITCH_DIR/Cargo.toml" \
  -p png-glitch-component \
  --target wasm32-unknown-unknown \
  --release

mkdir -p "$SCRIPT_DIR/app/src/main/wasm"
CORE_WASM="$PNG_GLITCH_DIR/target/wasm32-unknown-unknown/release/png_glitch_component.wasm"
OUT_WASM="$SCRIPT_DIR/app/src/main/wasm/png-glitch-component.wasm"
"$WASM_TOOLS_BIN" component new "$CORE_WASM" -o "$OUT_WASM"
