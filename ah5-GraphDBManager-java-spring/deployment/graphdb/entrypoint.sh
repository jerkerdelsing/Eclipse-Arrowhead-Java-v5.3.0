#!/bin/bash
# GraphDB for GraphDBManager: starts GraphDB, then (idempotently) creates the historian repository, sets the admin
# password, creates or updates GraphDBManager's own user (read/write on that repository only) and turns security on.
# Passwords come only from files (GRAPHDB_ADMIN_PASSWORD_FILE, GRAPHDB_USER_PASSWORD_FILE) and are never logged.
set -eu

REPOSITORY=${GRAPHDB_REPOSITORY:-graphdbmanager}
USER_NAME=${GRAPHDB_USER:-graphdbmanager}
ADMIN_PASSWORD=$(cat "$GRAPHDB_ADMIN_PASSWORD_FILE")
USER_PASSWORD=$(cat "$GRAPHDB_USER_PASSWORD_FILE")
BASE=http://127.0.0.1:7200

/opt/graphdb/dist/bin/graphdb -Dgraphdb.home="${GRAPHDB_HOME:-/opt/graphdb/home}" -Dgraphdb.distribution=docker "$@" &
GRAPHDB_PID=$!
trap 'kill -TERM "$GRAPHDB_PID" 2>/dev/null; wait "$GRAPHDB_PID"' TERM INT

# the image has neither jq nor python: passwords are embedded in JSON as they are, so only safe characters are allowed
for value in "$ADMIN_PASSWORD" "$USER_PASSWORD"; do
  case "$value" in
    ''|*[!A-Za-z0-9._-]*) echo "GraphDB passwords must be non-empty and use only A-Z a-z 0-9 . _ -" >&2; exit 1 ;;
  esac
done

until [ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/rest/security")" = 200 ]; do
  kill -0 "$GRAPHDB_PID" 2>/dev/null || exit 1
  sleep 2
done

# Admin credentials: the secret once security is on; on first start (security off) every request is allowed and the
# admin password is set below. After a rotation of the admin secret, reset it in GraphDB first (see README).
# Credentials go to curl through a netrc file and request bodies through stdin, never as process arguments
NETRC=$(mktemp)
trap 'rm -f "$NETRC"; kill -TERM "$GRAPHDB_PID" 2>/dev/null; wait "$GRAPHDB_PID"' TERM INT
printf 'machine 127.0.0.1 login admin password %s\n' "$ADMIN_PASSWORD" > "$NETRC"
if [ "$(curl -s -o /dev/null -w '%{http_code}' --netrc-file "$NETRC" "$BASE/rest/security/users/admin")" != 200 ]; then
  if [ "$(curl -s "$BASE/rest/security")" = true ]; then
    echo "GraphDB security is on and the admin secret is not accepted; refusing to continue" >&2
    kill -TERM "$GRAPHDB_PID"; wait "$GRAPHDB_PID"; exit 1
  fi
fi
api() { curl -fsS --netrc-file "$NETRC" "$@"; }
exists() { curl -fs -o /dev/null --netrc-file "$NETRC" "$1"; }
app_settings='"appSettings":{"DEFAULT_INFERENCE":false,"DEFAULT_SAMEAS":false,"DEFAULT_VIS_GRAPH_SCHEMA":false,"EXECUTE_COUNT":false}'

if ! exists "$BASE/rest/repositories/$REPOSITORY"; then
  api -o /dev/null -X POST -F "config=@/opt/graphdbmanager/repository-config.ttl" "$BASE/rest/repositories"
  echo "GraphDB repository $REPOSITORY created"
fi

admin_body="{\"password\":\"$ADMIN_PASSWORD\",\"grantedAuthorities\":[\"ROLE_ADMIN\"],$app_settings}"
api -o /dev/null -X PUT -H 'Content-Type: application/json' -d @- <<<"$admin_body" "$BASE/rest/security/users/admin"

user_body="{\"password\":\"$USER_PASSWORD\",\"grantedAuthorities\":[\"ROLE_USER\",\"READ_REPO_$REPOSITORY\",\"WRITE_REPO_$REPOSITORY\"],$app_settings}"
if exists "$BASE/rest/security/users/$USER_NAME"; then
  api -o /dev/null -X PUT -H 'Content-Type: application/json' -d @- <<<"$user_body" "$BASE/rest/security/users/$USER_NAME"
else
  api -o /dev/null -X POST -H 'Content-Type: application/json' -d @- <<<"$user_body" "$BASE/rest/security/users/$USER_NAME"
fi

if [ "$(curl -s "$BASE/rest/security")" != true ]; then
  api -o /dev/null -X POST -H 'Content-Type: application/json' -d true "$BASE/rest/security"
fi
# no anonymous access
api -o /dev/null -X POST -H 'Content-Type: application/json' -d '{"enabled": false}' "$BASE/rest/security/free-access"
rm -f "$NETRC"
unset ADMIN_PASSWORD USER_PASSWORD admin_body user_body
touch /tmp/graphdb-ready
echo "GraphDB ready: repository $REPOSITORY, user $USER_NAME, security on"
wait "$GRAPHDB_PID"
