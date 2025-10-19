#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
echo "[testsprite] Root: $ROOT_DIR"

run_backend() {
	echo "[testsprite] Running backend-api suite"
	(cd "$ROOT_DIR/backend" && npm run test:api)
}

run_frontend() {
	echo "[testsprite] Running frontend-e2e suite"
	(cd "$ROOT_DIR/admin-dashboard" && npm run test:e2e)
}

run_mobile() {
	echo "[testsprite] Running mobile-smoke suite"
	"$ROOT_DIR/scripts/run-mobile-smoke.sh"
}

STATUS=0
run_backend || STATUS=$?
run_frontend || STATUS=$?
run_mobile || STATUS=$?

exit $STATUS
