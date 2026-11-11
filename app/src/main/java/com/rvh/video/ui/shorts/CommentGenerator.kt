package com.rvh.video.ui.shorts

import kotlin.random.Random

/** One decorative local-preview comment. It is never persisted or synced. */
data class PlaceholderComment(
    val username: String,
    val avatarLetter: Char,
    val text: String,
)

/**
 * Generates a fixed-looking local preview set for the Shorts comment sheet.
 * These are deliberately presentation data, not social metrics or published comments.
 */
object CommentGenerator {

    private val usernames = listOf(
        "Sara P.", "Mike L.", "Jordan K.", "Alex G.", "Nina R.", "Tom W.", "Priya S.", "Leo M."
    )

    private val commentPool = listOf(
        "Great view!",
        "Love the editing!",
        "The framing is clean",
        "This looks great",
        "This is amazing",
        "Wait where is this",
        "Okay but the transition tho",
        "Been waiting for this one",
        "The quality on this is insane",
        "No because how",
    )

    fun forVideo(videoUri: String, count: Int = 5): List<PlaceholderComment> {
        val random = Random(videoUri.hashCode().toLong())
        return List(count) {
            val username = usernames.random(random)
            PlaceholderComment(
                username = username,
                avatarLetter = username.first(),
                text = commentPool.random(random),
            )
        }
    }
}
