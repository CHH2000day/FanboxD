# FanboxD

A tool that downloads fanbox

## Usage

- Get config.example.json,rename it to config.json and place it to the same directory with this program. You can also
  use `--config <config-file>` to specific a config file.
- Edit config.json,explanation of each field as follows:
    - `fanboxSessionId` Fill in your fanbox session id.You can get it in your browser's cookies.
    - `downloadDir` Where you want to store downloaded contents
    - `interval` Interval between two update queries.Only work when running as daemon.
    - `downloadFanbox` Whether to download all posts from the creators you're supporting on startup.
    - `asDaemon` Whether to monitor fanbox update after all download tasks are done.

- Fire up!