# Data-Driven Type Migration

[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)
![pipeline status](https://github.com/JetBrains-Research/data-driven-type-migration/actions/workflows/build.yml/badge.svg)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/MIT)

An IntelliJ IDEA plugin that adapts the current approach of `Type Migration` refactoring for Java to use custom
structural-replace templates that express the adaptations required to perform the type change. We inferred these type
change rules by mining 250 popular open-source Java repositories. In general, it helps to automate the process of
updating the data-flow dependent references of a program element, which type has been changed.

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
suggested `Type Migration Rules` from the dropdown list, select the search scope, and the plugin will try to update the data-flow dependent
references of the selected `Type Element`:

<img src="assets/img/proactive.gif" alt="Proactive Intention Example" width="600">

If the plugin does not succeed in migrating some references, it will show the
`Failed Type Changes` tool window. Moreover, for some of them it can suggest another type conversion rules using another
type of code intention ???
**Recovering Type Change Intention**. But this one could also change the type of the overall expression or statement in
your code, so be careful when applying them.

<img src="assets/img/recovering.jpeg" alt="Recovering Intention Example" width="900">

### Reactive Type Change Intention*

**Experimental feature*

This intention (and corresponding refactoring) is suggested when the user changes some `Type Element` in the Java code
manually. After the single type change is performed, you can click on the icon that appears on the gutter, and run the type migration from there:

<img src="assets/img/gutter.gif" alt="Gutter Icon Example" width="700">

*Note: the reactive intention for the particular type element in the code turns off by timeout (10 sec by default).*

### Settings

You can configure the default `Search Scope` for type migration or `Reactive Intention Disabling Timeout`
in the menu of the plugin: `File` - `Settings` - `Tools` - `Data-Driven Type Migration`.

Also, you can always edit any of type change patterns or rewrite rules manually in the Settings menu tab:

<img src="assets/img/settings.png" alt="Gutter Icon Example" width="400">

### Inspections

The plugin will also try to recommend you some type changes to apply in the form
of [Code Inspection](https://www.jetbrains.com/help/idea/code-inspection.html). Generally, these suggestions will have
a `WARNING` level and are based on the recommendations from Effective Java, eliminating the misuses of Java 8 
functional interfaces and unnecessary boxing.

<img src="assets/img/inspection.gif" alt="Gutter Icon Example" width="600">

**Note: the full list of supported type change patterns can be found [here](https://type-change.github.io/patterns.html).**