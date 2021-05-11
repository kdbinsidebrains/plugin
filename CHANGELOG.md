<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KdbInsideBrains Changelog

## [Unreleased]

### Changed

- Grammar has been redesigned to remove lags and issues. Complexity of q.flex was reduced and partly moved to q.bnf
- All KdbInstance related staff (toolwindow, services) are DumbAware now.

### Removed

- Dicts processing were removed from flex/bnf - too complex and takes ages for parsing

## [0.6.2]

### Added

- Initial public version of the plugin