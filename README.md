# WatchWolf - ServersManager [![CodeFactor](https://www.codefactor.io/repository/github/miranda1000/watchwolf-serversmanager/badge/dev)](https://www.codefactor.io/repository/github/miranda1000/watchwolf-serversmanager/overview/dev)

Provides Minecraft servers on demand for the [WatchWolf](https://watchwolf.dev/) framework. It
listens on TCP **8000**, and for every *start server* petition it assembles a server folder (plugins,
world, config files, and a JAR only for legacy), launches it inside its own Docker container, streams the console
back to the requester, and frees everything once the server stops.

`dev.watchwolf:watchwolf-servers-manager` · **Java 17** · Docker

## GHCR image release

Publishing a GitHub Release with a tag such as `v0.4.1` runs
`.github/workflows/release-image.yml` and pushes
`ghcr.io/sarhatabaot/watchwolf-servers-manager:v0.4.1`. The image builds the manager from
this repository and includes the WatchWolf-Server plugin JAR from the **published** Server
release. The workflow pins that plugin version with `WATCHWOLF_SERVER_VERSION`; update it when
releasing a newer plugin. Publish the plugin release first, then this manager release.

The release image has the plugin in `/servers/usual-plugins` and starts with an empty
`server-types` directory for the default itzg runtime. It does not use the older
`ci/release/build.sh` download path. The Compose deployment is in the WatchWolf repository's
`compose.release.yaml`. It still needs Docker daemon access to create ephemeral Minecraft
containers. Tag `v<version>` must match the Maven project version or the image build fails.

## How it works

The runtime provider is selected by `WATCHWOLF_MINECRAFT_RUNTIME`. The launch script passes
this host variable into ServersManager; an unset or empty value selects `itzg`. Set it to
`legacy` for the explicit fallback. The legacy provider requires a prebuilt server JAR and
selects an `eclipse-temurin` Java image from the requested Minecraft version. The `itzg`
provider rejects other types and versions with a provisioning error; it does not silently
replace a Spigot request with Paper.

| Provider | Server type and versions | Java image variant | Validation |
| --- | --- | --- | --- |
| itzg | Paper 1.19 | `java17` | Docker startup, WatchWolf plugin socket, Tester player-list request |
| itzg | Paper 1.20.2 | `java17` | Docker startup, WatchWolf plugin socket, Tester player-list request |
| itzg | Paper 1.20.6 | `java21` | Docker startup, WatchWolf plugin socket, Tester player-list request |
| legacy | Existing JAR-backed types and versions | WW-Core version mapping | Paper 1.20.6 parity workflow on Windows Docker Desktop |

The Tester fixtures also request Spigot and older Paper versions, including 1.8.8, 1.12.2,
and 1.15. Those remain on the explicit legacy provider and need matching local JARs. They are
not part of the tested itzg matrix. Linux Docker Engine has not been validated here.

For the itzg provider, `WATCHWOLF_ITZG_IMAGE` defaults to `itzg/minecraft-server` and
`WATCHWOLF_ITZG_TAG` defaults to `auto`, selecting the image variants in the table. Set these variables in the host environment before
running `ci/release/run.sh`; the script passes them into the manager. A variable explicitly set
inside the manager container takes precedence over the code default. The image tag selects the
container runtime and is independent of the requested Minecraft version. The manager pulls a
missing image, then the image downloads the requested Paper build on first start. Pin the tag
or image deliberately before release. Every isolated instance downloads its own Paper files;
there is no shared Minecraft JAR cache yet. An explicit tag overrides the per-version choice;
validate that override against the requested Paper version. Floating Java variant tags should
be pinned to a release tag or digest before a release that requires reproducibility.

Each itzg instance gets its own named Docker volume mounted at `/data`. ServersManager copies
the prepared world, config and plugin files into that volume through Docker's API. The
WatchWolf-Server plugin must be present or provisioning fails. The volume is removed after
the server exits; instance logs under `logs/` remain. The manager still needs the Docker
socket, and instances retain the existing bridge network and consecutive published port pair.
The helper copies files into the mounted volume and sets ownership to UID/GID 1000, matching
the itzg runtime settings. This storage path avoids nested Docker bind mounts on Windows Docker
Desktop. The version matrix and a legacy Paper 1.20.6 parity request passed on Windows Docker
Desktop with Linux containers on 2026-10-02. Linux Docker Engine has not been tested here.
The itzg provider assumes one ServersManager process per Docker daemon when reclaiming
labelled containers and `watchwolf-itzg-*` volumes after a manager restart.

The following diagram describes the legacy fallback path:

```
Tester ──"start Spigot 1.19 with these plugins"──▶ ServersManager :8000
                                                          │
                                     ServerRequirements    │  builds tmp/<id>/ :
                                                          │    server.jar, eula.txt,
                                                          │    server.properties, bukkit.yml,
                                                          │    plugins/, world/
                                                          ▼
                                     DockerizedServerInstantiator
                                                          │  docker run eclipse-temurin:<jdk>
                                                          ▼
                            ┌──────────── Minecraft server container ────────────┐
                            │  :25565 → host :N      (players connect here)      │
                            │  :25566 → host :N+1    (WatchWolf-Server socket)   │
                            └────────────────────────────────────────────────────┘
                                                          │
Tester ◀───"started, at <ip>:N"───── console scraped for "Done (…)! For help, type help"
```

Servers take a **consecutive pair** of ports starting at **8001**. The JDK image is chosen from
the Minecraft version (Java 8 below 1.17, 16 for 1.17, 17 up to 1.20.4, 21 from 1.20.5).

## Dependencies

- [Docker](https://www.docker.com/get-started/)
- The JDK images the servers run on:
  ```bash
  docker pull eclipse-temurin:8-jdk
  docker pull eclipse-temurin:16-jdk
  docker pull eclipse-temurin:17-jdk
  docker pull eclipse-temurin:21-jdk
  ```
- A [WatchWolf-Core release](https://github.com/watch-wolf/WatchWolf-Core/releases) matching the
  version in `pom.xml`; place the `.jar` inside `lib/`

## Compile

```bash
./ci/debug/build.sh --preclean
```

`--preclean` matters: the WatchWolf-Core jar in `lib/` is installed into your local Maven
repository during the **clean** phase, so skipping it will keep using whatever was there before.

The debug build expects exactly one `watchwolf-server-<version>.jar` in `ci/debug/`.
It compiles and assembles `ServersManager.jar`, prepares the plugin, and builds the
`servers-manager` Docker image.

For a build against the published releases instead of local jars, use `./ci/release/build.sh` —
it downloads the latest ServersManager and WatchWolf-Server releases and builds the image.
Create `ci/release/server-types/` and `ci/release/usual-plugins/` first (normally done by setup).

Both scripts can be called from any working directory. Artifact preparation runs in a
temporary container using commands embedded in each `build.sh`. Debug uses the Maven image
directly; release uses Ubuntu and installs its download and parsing tools during the run.
Only the final runtime image is built. Maven, wget, jq, and GNU grep/sort run inside Docker,
so the host only needs Docker and standard shell utilities. The debug build reuses `$HOME/.m2` for Maven
dependencies. Generated files belong to the invoking user, and a failed preparation stops
the build before the runtime image is built. The runtime Dockerfiles remain separate from
the build tooling.

## Run

```bash
./ci/release/run.sh     # or ./ci/debug/run.sh
```

This is what the [WatchWolf setup script](https://github.com/watch-wolf/WatchWolf) invokes. It
exports `MACHINE_IP`, `PUBLIC_IP`, `PARENT_PWD` and `SERVER_PATH_SHIFT` and runs
`docker run` with the `servers-manager` image.

The container bind-mounts `/var/run/docker.sock`, so the servers it starts are **siblings** on the
host rather than nested containers — that is why the paths it passes around have to be host paths.

## Data folders

All of these live in `ci/release/` (or `ci/debug/`), and are gitignored:

| Folder | Contents |
| --- | --- |
| `server-types/<Type>/<version>.jar` | The available server softwares, e.g. `Spigot/1.16.5.jar`, `Paper/1.20.jar`. Any folder name is a valid server type — `CustomSpigot/1.20.4.jar` works too. See [`server-types/README.md`](ci/release/server-types/README.md). |
| `usual-plugins/<Name>-<pluginVer>-<minMc>-<maxMc>.jar` | Plugins kept on the machine, e.g. `WorldGuard-7.0.8-1.19-LATEST.jar`. Spaces become `_`. **At least one WatchWolf-Server jar must be here.** See [`usual-plugins/README.md`](ci/release/usual-plugins/README.md). |
| `tmp/<id>/` | One scratch folder per running server; removed when it stops |
| `logs/<id>/` | `info.txt` (type, version, IP, timestamp) and `latest.log`, kept after the server dies |

The setup script populates `usual-plugins/`. With `WATCHWOLF_MINECRAFT_RUNTIME=itzg`, it leaves
`server-types/` empty; with `legacy`, it prepares JARs there. `src/tools/` holds those legacy
Spigot/Paper build scripts.

## Test

```bash
./ci/debug/tests.sh --unit
./ci/debug/tests.sh --integration
./ci/debug/tests.sh --unit --tests 'ServerRequirementsShould'
./ci/debug/validator.sh          # checks the test naming conventions
```

Reports land in `target/site` (HTML summary), `target/surefire-reports` (unit) and
`target/failsafe-reports` (integration). Unit tests must be named `*Should`; integration tests
must be named `IT*` and declare `@Timeout`. Integration tests genuinely start containers, so they
need Docker and the data folders above.

## Related

- [WatchWolf](https://github.com/watch-wolf/WatchWolf) — the protocol specification and setup script
- [WatchWolf-Core](https://github.com/watch-wolf/WatchWolf-Core) — shared entities and the RPC runtime
- [WatchWolf-Server](https://github.com/miranda1000/WatchWolf-Server) — the plugin injected into every server it starts
- [WatchWolf-Tester](https://github.com/miranda1000/WatchWolf-Tester) — its client
