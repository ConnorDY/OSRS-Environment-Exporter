# OSRS Environment Exporter

Tool for exporting Old School RuneScape environments so that they can be used in 3D modeling programs like Blender.

![Screenshot of the application](./docs/screenshot.png)

## Downloading

Please visit the [Releases](https://github.com/ConnorDY/OSRS-Environment-Exporter/releases) page to download the application.

We recommend using the latest release unless you are having issues with it.

## Configuration

| Config                     | Type      | Default Value    | Description                                                                                                                                                                                                  |
|----------------------------|-----------|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `alpha-mode`               | `Enum`    | `ORDERED_DITHER` | The type of alpha blending to use. One of `BLEND`, `CLIP`, `HASH`, `ORDERED_DITHER`, `IGNORE`.                                                                                                               |
| `anti-aliasing`            | `Enum`    | `MSAA_16`        | The multisampling depth to use. One of `DISABLED`, `MSAA_2`, `MSAA_4`, `MSAA_8`, `MSAA_16`.                                                                                                                  |
| `check-for-updates`        | `Boolean` | `true`           | If enabled, will check for newer versions of the application after the cache chooser screen.                                                                                                                 |
| `debug`                    | `Boolean` | `false`          | If enabled, the cache chooser will load the most recently used cache automatically, and a small suite of debug options will be accessible from the toolbar.                                                  |
| `fps-cap`                  | `Int`     | `0`              | Caps the frame rate (FPS) to the provided value. A value of `0` means no cap.                                                                                                                                |
| `initial-radius`           | `Int`     | `1`              | The initial radius to load.                                                                                                                                                                                  |
| `initial-region-id`        | `Int`     | `15256`          | The initial region to load.                                                                                                                                                                                  |
| `last-cache-dir`           | `String`  | Empty            | The last cache that was used.                                                                                                                                                                                |
| `last-checked-for-updates` | `Long`    | `0`              | The last time an update check was performed, in unix timestamp format. Used to limit the number of requests made to GitHub's servers.                                                                        |
| `mouse-warping`            | `Boolean` | Varies           | If enabled, the mouse will warp from one edge to the other if it leaves the window while the camera is being dragged. Disabled by default on MacOS due to permissions requirements, enabled everywhere else. |
| `priority-renderer`        | `Enum`    | Varies           | The face-sorting renderer to use. May not actually sort faces. One of `GLSL`, `CPU_NAIVE`. Set to `CPU_NAIVE` on MacOS due to missing compute shader support, and `GLSL` everywhere else.                    |
| `sample-shading`           | `Boolean` | `false`          | If enabled, tells OpenGL to shade sub-samples in MSAA (OpenGL 4.0+).                                                                                                                                         |

## Development

### Build

```bash
./gradlew build
```

### Run

```bash
./run
```

### Lint

```bash
./gradlew ktlintCheck # check linting
```

```bash
./gradlew ktlintFormat # apply automated linting fixes
```

## Credits

Original idea by [Trillion](https://twitter.com/TrillionStudios).

Based on [@tpetrychyn](https://github.com/tpetrychyn)'s [OSRS Map Editor](https://github.com/tpetrychyn/osrs-map-editor).

Using changes from [@partyvaper](https://github.com/partyvaper)'s [fork](https://github.com/partyvaper/osrs-map-editor).

## Donate

If you would like to financially support the primary developers ([@ScoreUnder](https://github.com/ScoreUnder) & [@ConnorDY](https://github.com/ConnorDY)) of the OSRS Environment Exporter, click the button below: [^1]

[![Donate](./docs/donate-button.png)](https://www.paypal.com/donate/?business=DVHHXKWFYZUJL&no_recurring=0&item_name=Donations+will+go+directly+to+the+primary+developers+%28score+and+wiz%29+of+the+OSRS+Environment+Exporter.&currency_code=USD)

[^1]: Donating will not guarantee that more time is spent developing this application.
