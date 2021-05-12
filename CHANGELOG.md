<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KdbInsideBrains Changelog

## [Unreleased]

## [0.7.10]

### Changed

- Grammar has been redesigned to remove lags and issues. Complexity of q.flex was reduced and partly moved to q.bnf
- All KdbInstance related staff (toolwindow, services) are DumbAware now.
- Gradle plugin has been updated to get changelogs from this file and release with publishPlugin

### Removed

- Dicts processing were removed from flex/bnf - too complex and takes ages for parsing

## [0.6.2] - DOESN'T WORK WITH BIG FILES

### Added

- Initial public version of the plugin