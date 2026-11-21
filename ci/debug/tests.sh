#!/bin/bash
set -o pipefail

unit=0
integration=0
test_match=""

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --unit) unit=1 ;;
        --integration) integration=1 ;;
        --tests) test_match="$2"; shift ;;
        *) echo "[e] Unknown parameter passed: $1" >&2; exit 1 ;;
    esac
    shift
done

if [[ $integration -eq 0 && $unit -eq 0 ]]; then
    echo "[e] You must specify at least one type of test to run!"
    exit 1
fi

script_path=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
base_path=$(dirname "$(dirname "$script_path")")
local_gradle_cache="$HOME/.gradle"
mkdir -p "$local_gradle_cache"

wsl_mode() { echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; }
get_ip() {
    if wsl_mode; then
        echo "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias Ethernet).IPAddress" |
            powershell.exe 2>/dev/null | tail -n2 | head -n1
    else
        hostname -I | awk '{print $1}'
    fi
}

common_args=(
    --rm -it
    --volume "$base_path:/compile"
    --volume "$local_gradle_cache:/gradle-cache"
    --env GRADLE_USER_HOME=/gradle-cache
    --env "WSL_MODE=$(wsl_mode; echo $?)"
    --env "MACHINE_IP=$(get_ip)"
    --env "PUBLIC_IP=$(curl -fsS ifconfig.me)"
    --env "PARENT_PWD=$base_path"
    --env SERVER_PATH_SHIFT=./ci/debug
)

docker run "${common_args[@]}" gradle:9.7.1-jdk17 \
    gradle --no-daemon --project-dir /compile clean

filter_args=()
if [[ -n "$test_match" ]]; then
    echo "[v] Running filtered tests: $test_match"
    filter_args=(--tests "$test_match")
fi

result=0
if [[ $unit -eq 1 ]]; then
    unit_results="$base_path/build/test-results/test"
    mkdir -p "$unit_results"
    docker run "${common_args[@]}" gradle:9.7.1-jdk17 \
        gradle --no-daemon --project-dir /compile test "${filter_args[@]}" \
        2>&1 | tee "$unit_results/docker-log.txt"
    result=$?
fi

if [[ $integration -eq 1 ]]; then
    integration_results="$base_path/build/test-results/integrationTest"
    mkdir -p "$integration_results"
    docker run "${common_args[@]}" --volume /var/run/docker.sock:/var/run/docker.sock \
        gradle:9.7.1-jdk17 gradle --no-daemon --project-dir /compile \
        integrationTest "${filter_args[@]}" 2>&1 | tee "$integration_results/docker-log.txt"
    integration_result=$?
    if [[ $integration_result -ne 0 ]]; then result=$integration_result; fi
fi

exit "$result"
