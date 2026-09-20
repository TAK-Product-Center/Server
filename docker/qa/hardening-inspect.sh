#!/bin/bash
# Effective runtime hardening evidence, read from the running containers rather than from the compose file.
set -uo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH

printf '# Effective hardening, %s\n' "$(date -u +%FT%TZ)"
printf '# docker inspect on the running clean-start stack. Compose intent is in compose.yaml; this is what the\n'
printf '# engine actually applied.\n\n'

for svc in takserver-config takserver-messaging takserver-api schemamanager takinit takconfig db idp qa; do
	cid=$(docker compose ps -aq "$svc" 2>/dev/null | head -1)
	[ -n "$cid" ] || { printf '## %s: no container\n\n' "$svc"; continue; }
	printf '## %s\n' "$svc"
	docker inspect "$cid" --format '   image        : {{.Config.Image}}
   state        : {{.State.Status}}
   User         : {{if .Config.User}}{{.Config.User}}{{else}}(image default){{end}}
   ReadonlyRootfs: {{.HostConfig.ReadonlyRootfs}}
   CapDrop      : {{.HostConfig.CapDrop}}
   CapAdd       : {{.HostConfig.CapAdd}}
   SecurityOpt  : {{.HostConfig.SecurityOpt}}
   Privileged   : {{.HostConfig.Privileged}}
   PidMode      : {{.HostConfig.PidMode}}
   NetworkMode  : {{.HostConfig.NetworkMode}}
   Tmpfs        : {{.HostConfig.Tmpfs}}
   Memory       : {{.HostConfig.Memory}}
   PortBindings : {{.HostConfig.PortBindings}}'
	printf '   Mounts       :\n'
	docker inspect "$cid" --format '{{range .Mounts}}     {{.Type}} {{.Name}}{{.Source}} -> {{.Destination}} rw={{.RW}}
{{end}}'
	if [ "$(docker inspect "$cid" --format '{{.State.Status}}')" = running ]; then
		printf '   effective id : '
		docker exec "$cid" id 2>/dev/null || docker exec "$cid" java -XshowSettings:properties -version 2>&1 | grep -m1 'user.name' || echo '(no shell and no id in this image)'
	fi
	printf '\n'
done

printf '## uid seen by the distroless TAK processes (no shell, so the JVM reports it)\n'
cid=$(docker compose ps -aq takserver-api | head -1)
[ -n "$cid" ] && docker inspect "$cid" --format '   Config.User = {{.Config.User}}' 
docker compose exec -T takserver-api java -XshowSettings:properties -version 2>&1 | grep -E 'user.name|user.home|java.io.tmpdir' | sed 's/^/   /'

printf '\n## the runtime image really has no shell\n'
docker run --rm --entrypoint /bin/sh takserver-oidc:5.7-RELEASE-14 -c 'echo reached a shell' 2>&1 | sed 's/^/   /'
docker run --rm --entrypoint /bin/bash takserver-oidc:5.7-RELEASE-14 -c 'echo reached a shell' 2>&1 | sed 's/^/   /'
