#!/bin/bash
# Builds delivery/RELEASE-MANIFEST.md: archive identities, current source hashes and historical acceptance evidence.
# Checksums alone prove file integrity, not that these bytes are the bytes that passed acceptance.
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH
REQ=/Users/karlholmqvist/LastwallLabs/Troy/requests/2026-09-20-tak-apk-container
OUT=$REQ/delivery/RELEASE-MANIFEST.md

h() { shasum -a 256 "$1" | awk '{print $1}'; }

{
printf '# Release manifest, TAK Server OIDC container\n\n'
printf 'Generated %s from branch `lastwall/oidc-phase1`, source commit `%s`, container commit `%s`.\n\n' \
  "$(date -u +%FT%TZ)" \
  "$(git -C .. rev-parse --short c0851d5)" "$(git -C .. rev-parse --short HEAD)"

printf '## Images (linux/amd64)\n\n'
# Read identities and sizes from the delivered OCI archive, not mutable engine tags or .Size.
python3 - "$REQ/delivery/takserver-oidc.oci.tar" <<'PYARCHIVE'
import gzip, hashlib, json, sys, tarfile
with tarfile.open(sys.argv[1], "r:*") as archive:
    def blob(digest):
        algorithm, value = digest.split(":", 1)
        if algorithm != "sha256":
            raise ValueError("unsupported digest algorithm")
        data = archive.extractfile(f"blobs/sha256/{value}").read()
        if hashlib.sha256(data).hexdigest() != value:
            raise ValueError("archive blob digest mismatch")
        return data
    def runtime_manifest(descriptor):
        doc = json.loads(blob(descriptor["digest"]))
        if "layers" in doc:
            return doc
        candidates = [d for d in doc["manifests"] if d.get("platform", {}).get("os") == "linux"
                      and d.get("platform", {}).get("architecture") == "amd64"]
        if len(candidates) != 1:
            raise ValueError("expected exactly one linux/amd64 image manifest")
        return runtime_manifest(candidates[0])
    index = json.load(archive.extractfile("index.json"))
    print("| image | index digest | compressed layer blobs | uncompressed layer tar data |")
    print("|---|---|---|---|")
    for descriptor in index["manifests"]:
        annotations = descriptor.get("annotations", {})
        name = annotations.get("io.containerd.image.name", annotations.get("org.opencontainers.image.ref.name", "unnamed"))
        image_index = json.loads(blob(descriptor["digest"]))
        if "manifests" not in image_index:
            raise ValueError("top-level image descriptor is not an index; do not label a manifest digest as index digest")
        manifest = runtime_manifest(descriptor)
        compressed = uncompressed = 0
        for layer in manifest["layers"]:
            data = blob(layer["digest"])
            compressed += len(data)
            if "gzip" in layer["mediaType"]:
                uncompressed += len(gzip.decompress(data))
            elif layer["mediaType"].endswith(".tar"):
                uncompressed += len(data)
            else:
                raise ValueError("unsupported layer compression: " + layer["mediaType"])
        print(f"| `{name}` | `{descriptor['digest']}` | {compressed:,} B | {uncompressed:,} B |")
PYARCHIVE
printf '\nSizes above are measured from the archive blobs and decompressed tar streams, including tar metadata\n'
printf 'and overwritten layer entries; they are not merged-rootfs sizes. Local engine storage is not measured here.\n'
printf 'Historical hostile-review-2 runtime totals: 390,366,742 B compressed; 561,752,576 B uncompressed.\n\n'

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
printf 'These hashes identify the package files at the commit named above; the images in the archive were built from that\n'
printf 'tree (init/qa at 11:36Z, runtime/schemamanager at 06:47Z, unchanged since), and the release evidence below ran on them.\n\n'
for f in Dockerfile Dockerfile.dockerignore compose.yaml .env.example RUN.md DESIGN-NOTES.md \
         init/init.sh init/CoreConfig.template.xml init/TAKIgniteConfig.xml init/UserAuthenticationFile.xml \
         health/Health.java qa/lab-up.sh qa/run-e2e-container.sh qa/keycloak-realm-container.sh \
         qa/revocation-proof.sh qa/hardening-inspect.sh qa/release-manifest.sh qa/check-maintenance.sh; do
  [ -f "$f" ] && printf -- '- `%s` sha256 `%s`\n' "$f" "$(h "$f")"
done

printf '\n## Release acceptance evidence, bound to the delivered images\n\n'
printf 'The four images above were built at 2026-09-20 11:36Z and saved into the archive at 11:51Z; nothing was rebuilt in\n'
printf 'between. The following runs executed on exactly those image ids (docker compose used the local tags, and the\n'
printf 'ids are recorded in qa/container/13-hardening-inspect-final.txt and 04b-image-size-after-fixes.txt):\n\n'
for f in 11-acceptance-cold-start-crl-on.txt 12-revocation-assertive.txt 13-hardening-inspect-final.txt 14-effective-coreconfig-redacted.xml 10-check-maintenance.txt; do
  p="$REQ/qa/container/$f"; [ -f "$p" ] && printf -- '- `qa/container/%s` sha256 `%s`\n' "$f" "$(shasum -a 256 "$p" | cut -c1-64)"
done
printf '\n11: docker compose down -v, fresh start, CRL_ENABLED=true, TOTAL pass=29 fail=0. 12: RC=0, both certificates 200 before\n'
printf 'revocation and rejected after on 8443, fresh correlated rejections on 8089. 14: the effective CoreConfig.xml of that\n'
printf 'stack with secrets redacted.\n'
printf '\n## Historical evidence (earlier image bytes, kept for the record)\n\n'
printf 'qa/container/09-* ran on an init image (83c5cd9f) that predates the CRL-default flip; 03-*/06-*/07-* predate the\n'
printf 'second fix round. They are not acceptance of this release.\n'

} > "$OUT"
echo "wrote $OUT"
