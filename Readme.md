![Avatar](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/thumbnail.png)
# FREE6

## About

A lightweight and scalable level-tracking Discord bot, inspired by [k9](https://github.com/Aurailus/k9) and [MEE6](https://mee6.xyz). Completely free.

Made by Lars Mueller aka LMD or appguru(eu). Code licensed under the terms of the GPLv3 (GNU Public License Version 3). Support is available on the [Discord Server](https://discord.gg/ysP74by).

## Screenshots

![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/about.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/help.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/error.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/leaderboard.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/rank.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/import.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/reset_confirmation.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/reset_performed.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/role_set.png)
![Screenshot](https://raw.githubusercontent.com/appgurueu/FREE6/master/media/screenshots/role_list.png)

## Development

FREE6 is an Apache NetBeans Gradle project using JDA, Jedis, Jackson and Logback.
Used Java version is 11, but it should also be compatible with Java 8.

## Hosting

FREE6 requires a running Redis server. Run FREE6 using `java -jar <path_to_jar> <path_to_conf>`. You might have to `cd` into the JAR's directory.
The JAR can be found in `build/libs/FREE6.jar`. Make sure you have Java 11 installed. It is, however, recommended to start & build FREE6 yourself using Gradle & Apache NetBeans.
For an example conf file, see `/conf/example.conf`. Note that it is recommended to start Redis yourself and that you should take additional security measures.

## Help

All commands need to be prefixed with the command starter (`>>` by default)

### `about`

Usage : `about` (informational command)

Displays an informational embed (linking this Readme).

### `help`

Usage : `help` (informational command)

Displays a helpful embed (linking this Readme).

### `import`

Usage : `import [url/id]`, *staff-only (`MANAGE_SERVER` permission)*

Imports XP from a MEE6 leaderboard URL or guild ID, defaults to the current guild's ID. Irreversible.

### `reset`

Usage : `reset`, *staff-only (`MANAGE_SERVER` permission)*

Needs to first be executed without arguments, and then a second time with the number supplied by the bot to confirm.

Resets the entire leaderboard. Irreversible.

### `role`

Alias : `roles`, `reward`, `rewards`, `rolereward`, `rolerewards`, `role-reward`, `role-rewards`

Usage : `role [level] [mention/id/name]`, *setting is staff-only (`MANAGE_SERVER` permission)*

Set/get a role reward for a level. A member can only have one rewarded role at a time. 

If nothing is given, it displays all rewards.

If only a `level` is given, it gets the current reward. 

If setting a reward and `name` is not unique, you have to use the `mention` or `id`.

### `rank`

Aliases : `stats`, `status`

Usage : `rank [mention]` / `rank [id]` - hint : you can get user IDs by enabling developer options and right-clicking on users

Displays somebody's rank - leaderboard position, role, level, and XP. 

Interactive - can be refreshed using "🔄" or replaced by corresponding leaderboard page using "🏆".

If no `mention` or `id` is given, it defaults to you.

### `leaderboard`

Alias : `lb`

Usage : `leaderboard [page]`

Displays the desired interactive leaderboard page modulo the number of pages using an embed.

Leaderboards can be refreshed or scrolled using reactions ("⬅", "🔄" and "➡").

If no `page` is given, it defaults to the first page