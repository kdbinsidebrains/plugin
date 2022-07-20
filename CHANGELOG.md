<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# KdbInsideBrains Changelog

## [1.7.0]

### Added

- Symbol references introduced
- Renaming variables/symbols by namespaces. For example, in variable: .this.is.my.variable, each token can be renamed
  separately.
- Context naming convention checks added: leading do, one level depth and separate spaces count

## [1.6.1]

### Fixed

- Items are auto-expanded at search in InstanceTree
- Max decimal precisions is 16 digits now and size is limited by the spinner in the settings
- Export to Excel and CSV ignores symbol and string wrapping options and export only raw data
- Nulls in Excel exported as empty value
- Copy/Paste takes into account symbol and string wrapping options

## [1.6.0]

### Added

- CredentialsPlugin functionality redesigned:
  - plugins copied into Idea System folder (current approach will be auto-migrated)
  - duplicate plugins are not allowed and adding new version replaces exist one
  - plugion version and description added (plugins must be recomplited)

## [1.5.0]

### Added

- Credentials plugin structure has been improved and redesigned a bit but backward compatible: plugin version has been
  added with "undefined" default value for now.

### Fixed

- Global and Scope credential changes show appropriate error if something is wrong
- Credentials plugin completely destroyed when removed and releases all resources

## [1.4.0]

### Added

- Idea 2022.2 supporting added
- Show public and private assignments in the StrictureView
- Show current element in the NavigationBar, base on the StructureView

## [1.3.1]

### Fixed

- Only console settings are restored: #36
- Not all iterators shown in console result: #37

## [1.3.0]

### Added

- Ability to split Console and TableResults views in Instance Console View: vertically or horizontally. You can set
  default split type in configuration (by default - no split) or set required for each Instance Console View.

### Fixed

- Tooltip for a TableResult has been removed - was added for debug and contains irrelevant information
- Closing last Instance Console View doesn't close Kdb Console tool window

## [1.2.0]

### Added

- Quick definition of an unknown function (quickfix) has been added

## [1.1.2]

### Fixed

- A function's arguments/parameters suggestion has been fixed

## [1.1.1]

### Fixed

- Key columns shown wrong after moving around in TableResult

## [1.1.0]

### Added

- New 'Execute Global Assignment' action has been added: by pressing 'Ctrl + Shift + Enter' or from context menu you
  can send whole global definition for current context into an instance.

  For example, if you have a function definition then 'Execute Global Assignment' will execute the definition wherever
  the cursor is inside the function. It also works for global multiline expressions.

### Fixed

- Long query overlays query time and rows count in a table result view. The query truncated to available size now.

## [1.0.1]

### Fixed

- Freezes at cascading parentheses parsing has been fixed

## [1.0.0]

### Added

- Code formatting has been added. **Warning: it's not recommended to reformat all code at once**
- Type suggestions added for a column definition

## [0.24.0]

### Fixed

- Idea 2022.1 integration fixed (search in a TableView)

## [0.23.1]

### Fixed

- a string shown in a TableView
- chart fails if there is no time/float columns

## [0.23.0]

### Fixed

- Hovering for striped tables is not shown.
- TableView background is not correct if 'stripe table' is disabled for keyed tables.
- Disabling 'wrap string' option also disables 'Stripe table' option.
- Key column symbol is not shown for keyed table but dictionary.

## [0.22.0]

### Added

- a list shown in a TableView (can be disabled in options. Enabled by default.)

### Changed

- a dict in a TableView can be disabled in options

## [0.21.1]

### Fixed

- a string inside a string parsing fixed

## [0.21.0]

### Added

- Dark icons set added with IntelliJ Idea colors
- Charting is ready now:
  - New Magnet/Snap tool has been added with ability to snap to line, vertex or disable the tool (default)
  - Values Tool shows only series values and no range values anymore
  - Measure Tool has context menu with remove/clear ability

## [0.20.0]

### Added

- If a TableResult has more than 200K rows*cols - a 300ms timeout for filtering update has been added after the search
  field change. That allows to write the full search text before do the real search work to reduce freezes for huge
  table result. Can be disabled in the search toolbar for the current search session.

### Changed

- Console behaviours changed:
  - Closing a console tab also closes the connection as well now
  - Changing active console tab changes active connection, so it's the same as select active connection from the
    toolbar

## [0.19.0]

### Fixed

- Table Result search's been fixed and can process strings now.

### Added

- EPA 2021.1 supporting added
- Table Result supports search by words and regex with or without case match
- Right side toolbar in charts view has been added instead of context menu
- Charting tools have been introduced:
  - **Crosshair tool** - to show current values for the mouse position
  - **Measure tool** - draw measuring rectangles on the chart with values diff: left mouse click to start, move, left
    mouse click to finish. Esc to cancel current drawing.
  - **Points collector** - save any chart clicked values into the points collector table with ability to export or
    send
    into another KDB instance.

## [0.18.1]

### Fixed

- resolved #20: QDocs doesn't work for variables

## [0.18.0]

### Added

- Charting configuration has been redesigned
- Candlestick chart has been added
- Line charts have been updated:
  - Spline, Steps, Bar, Area, Diff and Scatter line charts have been added
  - Ability to change line width and show shapes added
  - Transparent colors and axes orders added

## [0.17.1]

### Fixed

- KdbInstances color selection doesn't work in 2021.3.x versions
- Connection state notifications are not shown after the plugin update without restart
- Line Chart can't draw not double numbers

## [0.17.0]

### Added

- Line Charting has been added to the Table View

### Changed

- Icon colors changed for dark theme
- Restart is not required anymore for the plugin

## [0.16.0]

### Changed

- Excel exporting format changed to xlsx (Excel 2007) instead of xls (Excel 95)
  - 1,048,576 rows instead of 65,536 and 16,384 cols instead of 256
  - Streams worksheet is used to reduce memory footprint

- Export actions run as a background task to reduce UI freeze

## [0.15.0]

### Changed

- Supporting of Idea 2021.3 (build 213.*) has been added
- GrammaKit upgraded to 2021.2.1, IdeaPlugin to 1.3.0, Changelog to 1.3.1

## [0.14.0]

### Changed

- Grammar has been fully redesigned and performance has been significantly improved for nested expressions
- StructureView has been updated as well: a variable type is show based on assignment expression
- Files importing has been redesigned and improved. The 'system l' construction supports files refactoring now.
- Supporting version upgraded to 2021.2.3

### Fixed

- Auto-Reconnection logic executes the query, not just does reconnection
- auto-completion has been disabled for parameter names

### Added

- Symbols indexing has been added but not injected in dependency logic yet
- Auto-popup has been added for importing files for both, '\l ' and 'system l' constructions

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