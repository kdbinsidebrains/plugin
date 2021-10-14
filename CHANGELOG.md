<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KdbInsideBrains Changelog

## [0.13.0]

### Added

- Auto-Reconnection option has been added: when a query is executed and connection's been lost, it's auto-restoring
  first and notification is raised is it can't be restored. Enabled by default.
- 'Reconnect Instance' options has been added in connection lost notification. The notification is moved to Warning from
  Error.

### Fixed

- Linux 'Run Configuration' fixed (thanks to zvenczel-kx)
- Latest IDE version is supported now (2021.2.2). Many compatibility issues have been fixed as well.

## [0.12.2]

### Added

- Local variables added into completion list

## [0.12.1]

### Fixed

- Quick Documentation format has been fixed
- Missed keyword docs have been added

## [0.12.0]

### Added

- Queries logging functionality has been added: each finished query is logged into .kdbinb folder into daily files (can
  be disabled in settings).

### Fixed

- Copy/Paste issue in the console has been fixed: market is not copied anymore.

## [0.11.0]

### Added

- Documentations for all keywords and system (.Q, .h, .j and .z namespaces) functions have been added. Press Ctrl+Q on a
  function.
- Dynamic documentation based on comments with qdoc-tags
  supporting: https://code.kx.com/developer/libraries/documentation-generator/#qdoc-tags. Press Ctrl+Q on a variable.
- TypeCast to hh, mm, ss has been fixed. Lower and upper (parse from a string) casting is supported now.

### Fixed

- Issue with files renaming/moving (files refactoring) has been fixed.
- Global/Local variables inside a lamba defined in a namespace are detected correctly now.
- Variable references work inside a table definition

## [0.10.1]

### Added

- Arguments for .z, .j, .h and commands have been described in the completion

### Changed

- Duplicate connection from console tab has been fixed (https://github.com/kdbinsidebrains/plugin/issues/3)
- Namespace declaration issue fixed (https://github.com/kdbinsidebrains/plugin/issues/2)

## [0.10.0]

### Added

- Lambda parameters info has been added (Ctrl+P)]
- Arguments for keywords and .Q namespace have been described in completion

### Changed

- Correct variable type is shown in the completion popup
- Lambda parameters are shown in the completion popup

## [0.9.0]

### Added

- StructureView for Q files is ready
- A toolbar to a table result view has been added

## [0.8.0]

### Added

- Variable references have been updated and works fine inside global/local and query contexts.
- MixinLambdaVarDeclaration added - minix local and global scope of a variable is shown as an error with quick fix (
  changing scope into initial one).

### Changed

- Inline comment parsing has been finished and appropriate code doesn't blink anymore
- Psi tree elements slightly redesigned to reduce complexity
- Export rows/columns fixed
- Nulls are exported into Excel instead of wrong Q values
- Export re-uses current TableResultView formatting options (string escaping and so on) instead of default one.

### Removed

- QInplaceRenameHandler removed from the project

## [0.7.12]

### Added

- system "l " added to import pattern

### Changed

- UnresolvedImport moved from an Annotation to an Inspection
- UnusedLocalVariable moved from an Annotation to an Inspection
- QImportReferenceProvider optimized
- QVariableReferenceProvider - much better but shows global variables in local scope
- The plugin description has been updated

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