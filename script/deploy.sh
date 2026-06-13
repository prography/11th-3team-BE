#!/usr/bin/env bash
set -euo pipefail

: "${IMAGE_NAME:?IMAGE_NAME is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"

SECRETS_ID="${SECRETS_ID:-}"
CONTAINER_NAME="${CONTAINER_NAME:-prography-backend}"
HOST_PORT="${HOST_PORT:-8080}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
DOCKER_NETWORK="${DOCKER_NETWORK:-}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
GHCR_USERNAME="${GHCR_USERNAME:-}"
GHCR_TOKEN="${GHCR_TOKEN:-}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://localhost:${HOST_PORT}/actuator/health}"
HEALTHCHECK_TIMEOUT="${HEALTHCHECK_TIMEOUT:-180}"
HEALTHCHECK_INTERVAL="${HEALTHCHECK_INTERVAL:-5}"
AWSLOGS_ENABLED="${AWSLOGS_ENABLED:-true}"
AWSLOGS_REGION="${AWSLOGS_REGION:-ap-northeast-2}"
AWSLOGS_GROUP="${AWSLOGS_GROUP:-/prography/backend}"
AWSLOGS_STREAM_PREFIX="${AWSLOGS_STREAM_PREFIX:-prography-backend}"
AWSLOGS_CREATE_GROUP="${AWSLOGS_CREATE_GROUP:-true}"
TARGET_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
TEMP_ENV=""
TEMP_SECRET_JSON=""

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not installed."
  exit 1
fi

cleanup_temp_env() {
  if [ -n "${TEMP_ENV}" ] && [ -f "${TEMP_ENV}" ]; then
    rm -f "${TEMP_ENV}"
  fi
  if [ -n "${TEMP_SECRET_JSON}" ] && [ -f "${TEMP_SECRET_JSON}" ]; then
    rm -f "${TEMP_SECRET_JSON}"
  fi
}
trap cleanup_temp_env EXIT

if [ -n "${SECRETS_ID}" ]; then
  if ! command -v aws >/dev/null 2>&1; then
    echo "[deploy] aws CLI not found, cannot fetch secrets."
    exit 1
  fi
  if ! command -v jq >/dev/null 2>&1; then
    echo "[deploy] jq not found, cannot parse secrets."
    exit 1
  fi
  echo "[deploy] Fetching secrets from AWS Secrets Manager: ${SECRETS_ID}"
  TEMP_ENV="$(mktemp)"
  TEMP_SECRET_JSON="$(mktemp)"
  aws secretsmanager get-secret-value \
    --secret-id "${SECRETS_ID}" \
    --query "SecretString" \
    --output text > "${TEMP_SECRET_JSON}"
  jq -r '
    to_entries[]
    | select((.key | test("^(GHCR_USERNAME|GHCR_TOKEN)$")) | not)
    | "\(.key)=\(.value)"
  ' "${TEMP_SECRET_JSON}" > "${TEMP_ENV}"
  if [ -z "${GHCR_USERNAME}" ]; then
    GHCR_USERNAME="$(jq -r ".GHCR_USERNAME // empty" "${TEMP_SECRET_JSON}")"
  fi
  if [ -z "${GHCR_TOKEN}" ]; then
    GHCR_TOKEN="$(jq -r ".GHCR_TOKEN // empty" "${TEMP_SECRET_JSON}")"
  fi
  echo "[deploy] Secrets loaded."
else
  echo "[deploy] SECRETS_ID not set, proceeding without secrets."
fi

get_current_image() {
  sudo docker inspect -f '{{.Config.Image}}' "${CONTAINER_NAME}" 2>/dev/null || true
}

remove_container_if_exists() {
  if sudo docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
    echo "[deploy] Removing existing container: ${CONTAINER_NAME}"
    sudo docker rm -f "${CONTAINER_NAME}" >/dev/null
  fi
}

print_container_diagnostics() {
  if ! sudo docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
    echo "[deploy] Container ${CONTAINER_NAME} does not exist."
    return 0
  fi

  local status exit_code log_driver
  status="$(sudo docker inspect -f '{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null || echo unknown)"
  exit_code="$(sudo docker inspect -f '{{.State.ExitCode}}' "${CONTAINER_NAME}" 2>/dev/null || echo unknown)"
  log_driver="$(sudo docker inspect -f '{{.HostConfig.LogConfig.Type}}' "${CONTAINER_NAME}" 2>/dev/null || echo unknown)"

  echo "[deploy] Container status=${status}, exit_code=${exit_code}, log_driver=${log_driver}"

  if [ "${log_driver}" = "awslogs" ]; then
    echo "[deploy] awslogs driver in use — app logs are in CloudWatch (${AWSLOGS_GROUP}), not docker logs."
    echo "[deploy] If the container exited immediately, check EC2 IAM role for logs:CreateLogGroup/logs:PutLogEvents on ${AWSLOGS_GROUP}."
  else
    sudo docker logs --tail 100 "${CONTAINER_NAME}" 2>/dev/null || true
  fi
}

wait_for_container_running() {
  local max_tries=6
  local status=""

  for i in $(seq 1 "${max_tries}"); do
    status="$(sudo docker inspect -f '{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null || echo missing)"
    if [ "${status}" = "running" ]; then
      return 0
    fi
    if [ "${status}" = "exited" ] || [ "${status}" = "dead" ]; then
      return 1
    fi
    sleep 1
  done

  echo "[deploy] Container did not reach running state (last status=${status})."
  return 1
}

run_container_once() {
  local image_ref="$1"
  local use_awslogs="$2"
  local -a docker_run_args

  docker_run_args=(
    run
    -d
    --name "${CONTAINER_NAME}"
    --restart unless-stopped
    -p "${HOST_PORT}:${CONTAINER_PORT}"
    -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
  )

  if [ -n "${TEMP_ENV}" ] && [ -f "${TEMP_ENV}" ]; then
    docker_run_args+=(--env-file "${TEMP_ENV}")
  fi

  if [ -n "${DOCKER_NETWORK}" ]; then
    docker_run_args+=(--network "${DOCKER_NETWORK}")
  fi

  if [ "${use_awslogs}" = "true" ]; then
    docker_run_args+=(
      --log-driver awslogs
      --log-opt "awslogs-region=${AWSLOGS_REGION}"
      --log-opt "awslogs-group=${AWSLOGS_GROUP}"
      --log-opt "awslogs-stream-prefix=${AWSLOGS_STREAM_PREFIX}"
      --log-opt "awslogs-create-group=${AWSLOGS_CREATE_GROUP}"
    )
    echo "[deploy] CloudWatch Logs: ${AWSLOGS_GROUP} (${AWSLOGS_REGION})"
  fi

  docker_run_args+=("${image_ref}")

  echo "[deploy] Run container ${CONTAINER_NAME} from ${image_ref}"
  sudo docker "${docker_run_args[@]}"
}

run_container() {
  local image_ref="$1"

  if [ "${AWSLOGS_ENABLED}" = "true" ]; then
    if run_container_once "${image_ref}" "true" && wait_for_container_running; then
      return 0
    fi

    echo "[deploy] Container failed to stay running with awslogs driver."
    print_container_diagnostics
    remove_container_if_exists

    echo "[deploy] Retrying without awslogs driver to avoid blocking deployment."
    echo "[deploy] Add CloudWatch Logs IAM permissions to the EC2 instance role, then redeploy."
    run_container_once "${image_ref}" "false"
    wait_for_container_running
    return $?
  fi

  run_container_once "${image_ref}" "false"
  wait_for_container_running
}

wait_for_health() {
  if [ -z "${HEALTHCHECK_URL}" ]; then
    echo "[deploy] HEALTHCHECK_URL is empty, skipping health check."
    return 0
  fi

  if ! command -v curl >/dev/null 2>&1; then
    echo "[deploy] curl not found, skipping health check."
    return 0
  fi

  local max_tries=$(( HEALTHCHECK_TIMEOUT / HEALTHCHECK_INTERVAL ))
  echo "[deploy] Waiting for health check: ${HEALTHCHECK_URL} (max ${max_tries} tries)"

  for i in $(seq 1 "${max_tries}"); do
    if curl -fsS "${HEALTHCHECK_URL}" >/dev/null 2>&1; then
      echo "[deploy] Health check passed."
      return 0
    fi
    echo "[deploy] [$i/${max_tries}] Waiting..."
    sleep "${HEALTHCHECK_INTERVAL}"
  done

  echo "[deploy] Health check timed out after ${HEALTHCHECK_TIMEOUT}s."
  return 1
}

rollback() {
  local rollback_image="$1"

  if [ -z "${rollback_image}" ] || [ "${rollback_image}" = "${TARGET_IMAGE}" ]; then
    echo "[deploy] No rollback target available."
    return 1
  fi

  echo "[deploy] Rolling back to ${rollback_image}"
  remove_container_if_exists
  run_container "${rollback_image}"

  if ! wait_for_health; then
    echo "[deploy] Rollback health check also failed."
    return 1
  fi

  echo "[deploy] Rollback succeeded."
}

if [ -n "${GHCR_USERNAME}" ] && [ -n "${GHCR_TOKEN}" ]; then
  echo "[deploy] Login to ghcr.io as ${GHCR_USERNAME}"
  echo "${GHCR_TOKEN}" | sudo docker login ghcr.io -u "${GHCR_USERNAME}" --password-stdin
else
  echo "[deploy] GHCR credentials not provided, proceeding without docker login"
fi

PREV_IMAGE="$(get_current_image || true)"
if [ -n "${PREV_IMAGE}" ]; then
  echo "[deploy] Previous image detected: ${PREV_IMAGE}"
else
  echo "[deploy] No previous image detected (first deploy or container missing)"
fi

echo "[deploy] Pull image: ${TARGET_IMAGE}"
sudo docker pull "${TARGET_IMAGE}"

remove_container_if_exists

if ! run_container "${TARGET_IMAGE}"; then
  echo "[deploy] Deploy failed."
  rollback "${PREV_IMAGE}" || true
  exit 1
fi

if ! wait_for_health; then
  echo "[deploy] Health check failed."
  print_container_diagnostics
  rollback "${PREV_IMAGE}" || true
  exit 1
fi

echo "[deploy] Cleanup previous image"
if [ -n "${PREV_IMAGE}" ] && [ "${PREV_IMAGE}" != "${TARGET_IMAGE}" ]; then
  sudo docker rmi "${PREV_IMAGE}" >/dev/null 2>&1 || true
fi
sudo docker image prune -f >/dev/null 2>&1 || true

echo "[deploy] Done"
