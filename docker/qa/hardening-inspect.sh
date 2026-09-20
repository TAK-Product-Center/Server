#!/bin/bash
# Engine-configuration evidence and new diagnostic-process observations; not existing process credentials.
set -uo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH

printf '# Engine hardening configuration, %s\n' "$(date -u +%FT%TZ)"
printf '# docker inspect on the available stack. Compose intent is in compose.yaml; this records the\n'
printf '# engine configuration, not a full audit of existing processes.\n\n'

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
		printf '   new exec id  : '
		docker exec "$cid" id 2>/dev/null || docker exec "$cid" java -XshowSettings:properties -version 2>&1 | grep -m1 'user.name' || echo '(no shell and no id in this image)'
	fi
	printf '\n'
done

printf '## identity properties of a newly executed diagnostic JVM (not the serving JVM)\n'
cid=$(docker compose ps -aq takserver-api | head -1)
[ -n "$cid" ] && docker inspect "$cid" --format '   Config.User = {{.Config.User}}' 
docker compose exec -T takserver-api java -XshowSettings:properties -version 2>&1 | grep -E 'user.name|user.home|java.io.tmpdir' | sed 's/^/   /'

printf '\n## the runtime image really has no shell\n'
docker run --rm --entrypoint /bin/sh takserver-oidc:5.7-RELEASE-14 -c 'echo reached a shell' 2>&1 | sed 's/^/   /'
docker run --rm --entrypoint /bin/bash takserver-oidc:5.7-RELEASE-14 -c 'echo reached a shell' 2>&1 | sed 's/^/   /'
