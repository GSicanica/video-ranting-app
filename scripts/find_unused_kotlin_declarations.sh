#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v rg >/dev/null 2>&1; then
  echo "error: ripgrep (rg) is required" >&2
  exit 1
fi

tracked_kt_files="$(mktemp)"
trap 'rm -f "$tracked_kt_files"' EXIT

git ls-files '*.kt' | while IFS= read -r file; do
  [[ -f "$file" ]] && printf '%s\n' "$file"
done > "$tracked_kt_files"

echo "Unused Kotlin declaration candidates"
echo "===================================="
echo
echo "A symbol is reported when its name is found only in the declaring file."
echo "Review before deleting: reflection, serialization, Android manifests, and DI can hide usage."
echo

while IFS= read -r file; do
  while IFS=: read -r line kind name; do
    [[ -z "${name:-}" ]] && continue

    external_refs="$(
      rg --fixed-strings --word-regexp --line-number --no-heading \
        --files-with-matches "$name" $(<"$tracked_kt_files") \
        | grep -Fvx "$file" \
        || true
    )"
    external_ref_count="$(
      printf '%s\n' "$external_refs" \
        | sed '/^$/d' \
        | wc -l \
        | tr -d ' '
    )"

    if [[ "$external_ref_count" == "0" ]]; then
      printf '%s:%s %s %s\n' "$file" "$line" "$kind" "$name"
    fi
  done < <(
    perl -ne '
      if (/^\s*(?:public\s+|internal\s+|private\s+)?(?:(?:data|sealed|abstract|open|value)\s+)*(class|object|interface|enum\s+class)\s+([A-Za-z_][A-Za-z0-9_]*)/) {
        print "$.:$1:$2\n";
      } elsif (/^(?:public\s+|internal\s+|private\s+)?(val|var|fun)\s+([A-Za-z_][A-Za-z0-9_]*)/) {
        print "$.:$1:$2\n";
      }
    ' "$file"
  )
done < "$tracked_kt_files"
