package eu.kanade.tachiyomi.util

import org.nekomanga.BuildConfig

private const val GITHUB_REPO: String = "nekomangaorg/neko"

const val REPO_URL = "https://github.com/$GITHUB_REPO"

/** Release notes for the installed version. */
const val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/${BuildConfig.VERSION_NAME}"

const val LATEST_COMMIT_URL = "https://github.com/$GITHUB_REPO/commits/main"
