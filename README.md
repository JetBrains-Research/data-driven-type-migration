# Data-Driven Type Migration

[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)
![pipeline status](https://github.com/JetBrains-Research/data-driven-type-migration/actions/workflows/build.yml/badge.svg)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/MIT)

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
- Install the plugin in your **IntelliJ IDEA 2021.1** via `File` - `Settings` - `Plugins`
  - `Install Plugin from Disk...`

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

### Inspections

The plugin will also try to recommend you some type changes to apply in the form
of [Code Inspection](https://www.jetbrains.com/help/idea/code-inspection.html). Generally, these suggestions will have
a `WARNING` level, but just to attract your attention during plugin evaluation. Such hints are mostly of a
recommendation and training nature.

<img src="assets/img/inspection.gif" alt="Gutter Icon Example" width="600">

### Currently supported Type Change Patterns*:

**Examples are provided with [comby](https://comby.dev/) template syntax*

| Source Type | Target Type |
| --- | --- |
| `java.io.File` | `java.nio.file.Path` |
| `java.util.List<:[type]>` | `java.util.Set<:[type]>` |
| `java.util.List<:[type]>` | `java.util.Deque<:[type]>` |
| `String` | `java.util.regex.Pattern` |
| `String` | `java.nio.charset.Charset` |
| `String` | `java.net.URL` |
| `String` | `java.util.UUID` |
| `String` | `java.net.URI` |
| `String` | `java.nio.file.Path` |
| `java.util.concurrent.Callable<:[type]>` | `java.util.function.Supplier<:[type]>` |
| `java.util.Map<String, String>` | `java.util.Properties` |
| `:[type]` | `java.util.Optional<:[type]>` |
| `java.util.Optional<:[type]>` | `:[type]` |
| `java.util.Optional<Integer>` | `java.util.OptionalInt` |
| `java.util.Optional<Long>` | `java.util.OptionalLong` |
| `java.util.Optional<Double>` | `java.util.OptionalDouble` |
| `:[type]` | `java.util.List<:[type]>` |
| `int` | `long` |
| `long` | `java.math.BigInteger` |
| `long` | `java.time.Duration` |
| `java.util.Date` | `java.time.Instant` |
| `java.util.Date` | `java.time.LocalDate` |
| `java.util.function.Function<Integer, Integer>` | `java.util.function.IntUnaryOperator` |
| `java.util.function.Function<Double, Integer>` | `java.util.function.DoubleToIntFunction` |
| `java.util.function.Function<Long, Integer>` | `java.util.function.LongToIntFunction` |
| `java.util.function.Function<:[type], Integer>` | `java.util.function.ToIntFunction` |
| `java.util.function.BiFunction<Integer, Integer, Integer>` | `java.util.function.IntBinaryOperator` |
| `java.util.function.BiFunction<:[type], :[type2], Integer>` | `java.util.function.ToIntBiFunction` |
| `java.util.function.BiFunction<:[type], :[type2], Long>` | `java.util.function.ToLongBiFunction` |
| `java.util.function.BiFunction<:[type], :[type2], Double>` | `java.util.function.ToDoubleBiFunction` |
| `java.util.function.Function<Long, Long>` | `java.util.function.LongUnaryOperator` |
| `java.util.function.Function<Integer, Long>` | `java.util.function.IntToLongFunction` |
| `java.util.function.Function<Double, Long>` | `java.util.function.DoubleToLongFunction` |
| `java.util.function.Function<:[type], Long>` | `java.util.function.ToLongFunction` |
| `java.util.function.BiFunction<Long, Long, Long>` | `java.util.function.LongBinaryOperator` |
| `java.util.function.BiFunction<Double, Double, Double>` | `java.util.function.DoubleBinaryOperator` |
| `java.util.function.Function<Double, Double>` | `java.util.function.DoubleUnaryOperator` |
| `java.util.function.Function<Integer, Double>` | `java.util.function.IntToDoubleFunction` |
| `java.util.function.Function<Long, Double>` | `java.util.function.LongToDoubleFunction` |
| `java.util.function.Function<:[type], Double>` | `java.util.function.ToDoubleFunction` |
| `java.util.function.Function<Long, Boolean>` | `java.util.function.LongPredicate` |
| `java.util.function.Function<Integer, Boolean>` | `java.util.function.IntPredicate` |
| `java.util.function.Function<Double, Boolean>` | `java.util.function.DoublePredicate` |
| `java.util.function.Function<:[type], Boolean>` | `java.util.function.Predicate<:[type]>` |
| `java.util.function.Supplier<Integer>` | `java.util.function.IntSupplier` |
| `java.util.function.Supplier<Long>` | `java.util.function.LongSupplier` |
| `java.util.function.Supplier<Double>` | `java.util.function.DoubleSupplier` |
| `java.util.function.Supplier<Boolean>` | `java.util.function.BooleanSupplier` |
