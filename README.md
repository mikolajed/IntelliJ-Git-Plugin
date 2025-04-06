# IntelliJ-Git-Plugin

![Build](https://github.com/mikolajed/IntelliJ-Git-Plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

A simple IntelliJ plugin to rename the most recent Git commit with a multi-line input dialog.

<!-- Plugin description -->
Rename your latest Git commit directly from IntelliJ IDEA. Features a multi-line input dialog and validation to prevent empty commit messages—prompting again if you try to sneak one through. Built for developers who want a quick, reliable way to tweak commit messages without leaving the IDE.

This section is extracted by Gradle into `plugin.xml` during the build process. Do not remove `<!-- ... -->` sections.
<!-- Plugin description end -->

## Features
- Rename the most recent Git commit via `git commit --amend`.
- Multi-line commit message support.
- Prevents empty messages with a warning and re-prompt.
- Skips amend if the new message matches the current one.
- Error handling for failed amend attempts.

## Installation

- **Using the IDE Built-in Plugin System**:
- **Manually**: download the [latest release](https://github.com/mikolajed/IntelliJ-Git-Plugin/releases/latest) and install using <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
- **Git**: clone the repo and follow developer guide for instruction to build and run.

## Developer Guide
1. **Clone the Repo**:
   ```bash
   git clone https://github.com/mikolajed/IntelliJ-Git-Plugin.git
   cd IntelliJ-Git-Plugin
   ```

2. **Open in IntelliJ IDEA:**  
- Tested on IntelliJ IDEA 2024.3.5 (Ultimate Edition).
- <kbd>File > Open</kbd> > Select the cloned folder.

3. **Configure Project Structure:**
- <kbd>File > Project Structure</kbd>
- Under SDKs, click <kbd>+</kbd> > <kbd>IntelliJ Platform Plugin SDK</kbd> > Choose your IDEA install dir.
- Add git4idea to the classpath:  
- Select the SDK > <kbd>Classpath</kbd> tab > <kbd>+</kbd> > Navigate to <IDEA_DIR>/plugins/git4idea/lib/git4idea.jar > <kbd>OK</kbd>.
- Apply and close.

  | New project                                | Add Git4Idea Classpath                      |
  |--------------------------------------------|-------------------------------------------|
  | ![Configure Project Structure](./img/sdk.png) | ![Git4Idea Classpath](./img/git4idea.png) |


4. **Gradle Sync:**  
- <kbd>File > Sync Project with Gradle Files</kbd> or click the Gradle elephant icon.
- Wait for dependencies (JUnit 5, Mockito) to resolve.

5. **Run the Plugin:**
- In IDEA: <kbd>Run > Run 'Plugin'</kbd> (or use the green play button).
- Or via Gradle:  
```bash
./gradlew runIde
```

6. **Test It:**
In the sandbox IDEA instance:  
   - <kbd>File > New > Project</kbd> > Create a simple project.

     | Plugin location                           | Rename current commiit                 |
     |--------------------------------------------|-------------------------------------------|
     | ![Click on git menu](./img/gitmenu.png) | ![Rename current commit](./img/newmsg.png) |

- Init Git: git init, add a file, and commit (git commit -m "Initial commit").
- Main toolbar > <kbd>Git</kbd> > <kbd>Rename Current Commit</kbd> > Change the message.

7. **Build for Distribution:**
```bash
./gradlew buildPlugin
```