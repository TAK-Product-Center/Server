#!/bin/bash
# Builds delivery/RELEASE-MANIFEST.md: image identity, configuration identity, and the acceptance run they are
# bound to. Checksums alone prove file integrity, not that these bytes are the bytes that passed acceptance.
set -uo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH
REQ=/Users/karlholmqvist/LastwallLabs/Troy/requests/2026-09-20-tak-apk-container
OUT=$REQ/delivery/RELEASE-MANIFEST.md
TAG=5.7-RELEASE-14

h() { shasum -a 256 "$1" | awk '{print $1}'; }

{
printf '# Release manifest, TAK Server OIDC container\n\n'
printf 'Generated %s from branch `lastwall/oidc-phase1`, source commit `%s`, container commit `%s`.\n\n' \
  "$(date -u +%FT%TZ)" \
  "$(git -C .. rev-parse --short c0851d5)" "$(git -C .. rev-parse --short HEAD)"

printf '## Images (linux/amd64)\n\n'
printf '| image | config digest | content size | unpacked layers | local storage |\n|---|---|---|---|---|\n'
for i in takserver-oidc takserver-oidc-schemamanager takserver-oidc-init takserver-oidc-qa; do
  id=$(docker image inspect "$i:$TAG" --format '{{.Id}}' 2>/dev/null)
  unpacked=$(docker image inspect "$i:$TAG" --format '{{.Size}}' 2>/dev/null)
  line=$(docker image ls --format '{{.Repository}}\t{{.Size}}\t{{.VirtualSize}}' 2>/dev/null | awk -F'\t' -v r="$i" '$1==r{print;exit}')
  # docker 29 prints IMAGE as repo:tag in column 1, DISK USAGE in column 3, CONTENT SIZE in column 4
  content=$(docker image ls | awk -v r="$i:$TAG" '$1==r{print $4}')
  disk=$(docker image ls | awk -v r="$i:$TAG" '$1==r{print $3}')
  printf '| `%s:%s` | `%s` | %s | %s bytes | %s |\n' "$i" "$TAG" "$id" "${content:-n/a}" "${unpacked:-n/a}" "${disk:-n/a}"
done

printf '\nThose are three different quantities: content size is the sum of the compressed layer blobs, unpacked\n'
printf 'layers is what the filesystem holds after extraction, local storage is what this engine reports for the\n'
printf 'image including shared base layers counted once per image.\n\n'

printf '## Base images, pinned by digest\n\n'
grep -E '^ARG (JDK|RUNTIME|ALPINE)_IMAGE=' Dockerfile | sed 's/^ARG /- `/; s/$/`/'
grep -nE 'image: (postgis|quay.io/keycloak)' compose.yaml | sed 's/^[0-9]*: *image: /- `/; s/$/`/'

printf '\n## Delivered artifact\n\n'
printf -- '- `takserver-oidc.oci.tar` sha256 `%s`, %s bytes, contains the four images above\n' \
  "$(h "$REQ/delivery/takserver-oidc.oci.tar")" "$(stat -f%z "$REQ/delivery/takserver-oidc.oci.tar")"
for f in takserver-oidc.cyclonedx.json takserver-oidc-schemamanager.cyclonedx.json takserver-oidc-init.cyclonedx.json takserver-oidc-qa.cyclonedx.json; do
  [ -f "$REQ/delivery/$f" ] && printf -- '- `%s` sha256 `%s`\n' "$f" "$(h "$REQ/delivery/$f")"
done

printf '\n## Configuration and documentation identity\n\n'
printf 'These are the files the acceptance run below actually used. A different compose file or a different\n'
printf 'initializer is a different release.\n\n'
for f in Dockerfile Dockerfile.dockerignore compose.yaml .env.example RUN.md DESIGN-NOTES.md \
         init/init.sh init/CoreConfig.template.xml init/TAKIgniteConfig.xml init/UserAuthenticationFile.xml \
         health/Health.java qa/lab-up.sh qa/run-e2e-container.sh qa/keycloak-realm-container.sh \
         qa/revocation-proof.sh qa/hardening-inspect.sh; do
  [ -f "$f" ] && printf -- '- `%s` sha256 `%s`\n' "$f" "$(h "$f")"
done

printf '\n## Acceptance this release is bound to\n\n'
for f in 09-acceptance-clean-start.txt 06-revocation.txt 07-hardening-inspect.txt; do
  p=$REQ/qa/container/$f
  if [ -f "$p" ]; then
    printf -- '- `qa/container/%s` sha256 `%s`\n' "$f" "$(h "$p")"
    grep -E '^TOTAL|^=== .*AFTER|reached a shell' "$p" 2>/dev/null | sed 's/^/      /' | head -6
  else
    printf -- '- `qa/container/%s` MISSING\n' "$f"
  fi
done
printf '\nThe acceptance run was a cold start: `qa/lab-up.sh --clean`, which begins with `docker compose down -v`\n'
printf 'and therefore a new certificate authority, a new realm, a new client secret and an empty database.\n'
} > "$OUT"
echo "wrote $OUT"
