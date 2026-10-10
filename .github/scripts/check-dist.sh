#!/bin/bash
# Fails if a committed dist/ bundle differs from the one just built by `./gradlew bundle`.
# Kotlin/JS writes the interfaces of a class in an order that changes from one build to the next, and the order
# has no effect, so the interface lists are sorted before comparing.
set -e
cd "$(git rev-parse --show-toplevel)"

normalize() {
    perl -pe 's/\[([^\[\]]*)\]\);$/"[" . join(", ", sort split(m|,\s*|, $1)) . "]);"/e'
}

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

stale=0
for file in $(git ls-files -- '*/dist/*'); do
    git show "HEAD:$file" | normalize > "$tmp/committed"
    normalize < "$file" > "$tmp/built"
    if ! diff -u "$tmp/committed" "$tmp/built" > "$tmp/diff"; then
        echo "::error file=$file::$file is stale"
        head -50 "$tmp/diff"
        stale=1
    fi
done

untracked="$(git ls-files --others --exclude-standard -- '*/dist/*')"
if [ -n "$untracked" ]; then
    echo "::error::Untracked bundle files: $untracked"
    stale=1
fi

if [ "$stale" = 1 ]; then
    echo "::error::dist/ is stale: run ./gradlew bundle and commit the result"
fi
exit $stale
