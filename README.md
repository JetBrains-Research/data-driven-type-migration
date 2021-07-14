# Data-Driven Type Migration

[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)

An IntelliJ IDEA plugin that adjusts the current approach to `Type Migration`
refactoring for Java using inferred type change rules, which were gathered from popular open-source Java repositories.
In general, it helps to automate the process of updating the data-flow dependent references of a program element, which
type has been changed.

The plugin is compatible with **IntelliJ IDEA 2021.1** and can be built from sources.

## Installation

At first, clone this repository and open the root folder.

**Build the plugin from sources and go:**

- Run `./gradlew :plugin:buildPlugin`
- Check out `./plugin/build/distributions/plugin-*.zip`
- Install the plugin in your **IntelliJ IDEA 2020.1** via `File` - `Settings` - `Plugins` - `Install Plugin from Disk...`

**Quick IDE launch for evaluation:**

- Run `./gradlew :plugin:runIde`

## Overview

There are several types of [Code Intentions](https://plugins.jetbrains.com/docs/intellij/code-intentions.html)
provided by the plugin:

### Proactive Type Change Intention

This is a usual code intention that can be invoked from the `Show Context Actions`
item in the popup menu, when you click on the `Type Element` in your Java code. Then you can choose any of the
suggested `Type Migration Rules` from the dropdown list, and the plugin will try to update the data-flow dependent
references of the selected `Type Element`:

<img src="assets/img/proactive.gif" alt="Proactive Intention Example" width="600">

If the plugin does not succeed in migrating some references, it will show the
`Failed Type Changes` tool window. Moreover, for some of them it can suggest another type conversion rules using another
type of code intention —
**Recovering Type Change Intention**. But this one could also change the type of the overall expression or statement in
your code, so be careful when applying them.

<img src="assets/img/recovering.jpg" alt="Recovering Intention Example" width="900">

### Reactive Type Change Intention*

**Experimental feature*

This intention (and corresponding refactoring) is suggested when the user changes some `Type Element` in the Java code
manually. After the single type change is performed, you can put the caret on the element that was just changed, and
open the `Show Context Actions`
menu. If such a type change is supported by the plugin, it will offer you the appropriate type migration rule.

<img src="assets/img/reactive.gif" alt="Reactive Intention Example" width="600">

You can also click on the icon that appears on the gutter, and run the type migration from there:

<img src="assets/img/gutter.gif" alt="Gutter Icon Example" width="700">

*Note: the reactive intention for the particular type element in the code turns off by timeout (10 sec by default).*

### Settings

You can setup the appropriate `Search Scope` for type migration or `Reactive Intention Disabling Timeout`
in the menu of the plugin: `File` - `Settings` - `Tools` - `Data-Driven Type Migration`.

<img src="assets/img/settings.png" alt="Gutter Icon Example" width="400">

### Currently supported Type Change Patterns*:

**Examples are provided with [comby](https://comby.dev/) template syntax*

| Source Type | Target Type |
| --- | --- |
|`java.io.File` | `java.nio.file.Path` |
| `String` | `java.util.regex.Pattern` |
| `String` | `java.nio.charset.Charset` |
| `String` | `java.net.URI` |
| `String` | `java.util.UUID` |
| `String` | `java.nio.file.Path` |
| `java.util.Date` | `java.time.Instant` |
| `java.util.Date` | `java.time.LocalDate` |
| `int` | `long` |
| `java.util.List<:[type]>` | `java.util.Set<:[type]>` |
| `java.util.function.Function<Double, Double>` | `java.util.function.DoubleUnaryOperator` |
| `java.util.function.Function<:[type], Boolean>` | `java.util.function.Predicate<:[type]>` |
| `java.util.function.Supplier<Integer>` | `java.util.function.IntSupplier` |
| `java.util.function.Function<Integer, Integer>` | `java.util.function.IntUnaryOperator` |
| `java.util.concurrent.Callable<:[type]>` | `java.util.function.Supplier<:[type]>` |
| `:[type]` | `java.util.Optional<:[type]>` |
