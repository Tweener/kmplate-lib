# Kmplate

Kmplate-lib is a template to easily create a new Kotlin Multiplaform library with Maven Central publishing configuration.

### 1. Create a webhook post messages on Slack

1. For Github Actions to post messages on Slack, you must create a new webhook URL by using the [Incoming Webhook](https://slack.com/apps/A0F7XDUAZ-incoming-webhooks) app.
2. Create a new [Github Actions secret](https://docs.github.com/en/actions/security-guides/encrypted-secrets) with name `SLACK_WEBHOOK_URL`, and copy paste the webhook created in the previous step as
   value of this secret.

### 2. Configure the Slack bot to post on Slack

We will configure 2 Slack bots to post message on Slack: one bot to check for outdated dependencies, and one bot for the build status.
To configure these 2 Slack bots, we need to create 3 [Github Actions variables](https://docs.github.com/en/actions/learn-github-actions/variables):

1. `SLACK_GITHUB_ACTIONS_CHANNEL_NAME`: the name of the Slack channel where Github Actions will post messages (ie. `myproject_build_status`).
2. `SLACK_GITHUB_ACTIONS_DEPENDENCY_UPDATES_ICON_URL`: the icon URL to be used as a profile picture for the "Dependency Updates" Slack bot.
3. `SLACK_GITHUB_ACTIONS_ICON_URL`: the icon URL to be used as a profile picture for the "Github Actions CI" Slack bot.

### 3. Rename package name to your own

1. Open `buildSrc/src/main/kotlin/Dependencies.kt` and rename the following things:
    1. _Line 21_: Change `MyProject` object name to your own project name,
    2. _Line 22_: Change `com.tweener.changehere` package name to your own package name.
    3. _Line 29_: Change all the properties within the `object Maven` block to your own Maven Central publishing configuration.
2. Rename module `changehere` to the name of your library. This is the name that will be shown when published to Maven Central.
3. Open `settings.gradle.kts` and change `MyProjectName` on line 17 by your own project name.
4. Open `changehere/build.gradle.kts` (or `yourlibraryname/build.gradle.kts` if you renamed the module on step 2) and change `changehere` on line 65 with your own iOS framework name.
5. Rename packages name (`import` and `package`) in all existing files:
    1. Click on `Edit` > `Find` > `Replace in files`,
    2. In the first input field, type `com.tweener.changehere`,
    3. In the second input field, type your own package name,
    4. Click on `Replace all` button.
6. Replace `com/tweener/changehere` by your own directory path in the following directories:
    1. `changehere/src/commonMain/kotlin/com/tweener/changehere`
    2. `changehere/src/androidMain/kotlin/com/tweener/changehere`
    3. `changehere/src/iosMain/kotlin/com/tweener/changehere`

### 4. Rename Github Actions names

1. Open `.github/workflows/buildDebug.xml` and replace `Kplate` on lines 1, 42 and 54 by your own name.
