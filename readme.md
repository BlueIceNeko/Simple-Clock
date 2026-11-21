# Simple Clock

A fully customizable full-screen clock.

Mostly written by DeepSeek V4.1 Flash.

---

This offline full-screen clock can be fully customized by `./config.json`
or other JSON file specified by `--config`.
Invalid layout components will be ignored.

`text` layout component supports
`{time}` (with `timeFormat`),
`{date}` (with `dateFormat`),
`{week}` (with `weekDef`)

It also has a bundled default config which will be read if the external config is absent.
- `default_config.json` is the slim default and `--generate-config` writes it to `./config.json`.
- `full_config.json` is the full default and `--generate-full-config` writes it to `./config.json`.

ESC to exit, F5 to reload the config, F8 to minimize, F11 to toggle full-screen mode
