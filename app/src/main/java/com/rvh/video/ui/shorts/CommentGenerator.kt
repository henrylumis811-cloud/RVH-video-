package com.rvh.video.ui.shorts

import kotlin.random.Random

/** One decorative comment. Not persisted — regenerated deterministically from the video's URI each time. */
data class PlaceholderComment(
    val username: String,
    val avatarLetter: Char,
    val text: String,
    val likes: Int,
    val minutesAgo: Int,
)

/**
 * Generates a fixed-looking set of comments per video, per the decision to
 * drop real TikTok comment ingestion entirely: comments are now a styling
 * layer, not data. Seeding off the video's URI hash means the same short
 * shows the same comments every time it's opened, rather than looking
 * broken/random on repeat views — while still varying naturally between
 * different videos.
 */
object CommentGenerator {

    private val usernames = listOf(
        "Sara P.", "Mike L.", "Jordan K.", "Alex G.", "Nina R.", "Tom W.", "Priya S.", "Leo M."
    )

    private val commentPool = listOf(
        "Great view!",
        "Love the editing!",
        "Goo up are the need!",
        "Naming tolaen sams watr commentas lol",
        "This is amazing",
        "Wait where is this",
        "Okay but the transition tho",
        "Been waiting for this one",
        "The quality on this is insane",
        "No because how",
    )

    fun forVideo(videoUri: String, count: Int = 5): List<PlaceholderComment> {
        val seed = videoUri.hashCode().toLong()
        val random = Random(seed)

        return List(count) {
            PlaceholderComment(
                username = usernames.random(random),
                avatarLetter = usernames.random(random).first(),
                text = commentPool.random(random),
                likes = random.nextInt(1, 8000),
                minutesAgo = random.nextInt(1, 600),
            )
        }
    }

    /** Total shown next to the comment icon in the action rail — derived from the same seed, so it matches the sheet. */
    fun totalCountForVideo(videoUri: String): Int {
        val random = Random(videoUri.hashCode().toLong())
        return random.nextInt(50, 900)
    }

    private val captionPool = listOf(
        "A quick hike on the trail",
        "Golden hour hits different",
        "Not the best day but we made it work",
        "Chasing the view",
        "One of those days",
        "POV: you finally made it",
    )

    /** Seeded so the caption stays fixed per video, same rationale as comments — no re-render surprises on repeat views. */
    fun captionForVideo(videoUri: String): String {
        val random = Random(videoUri.hashCode().toLong() + 1) // +1 offsets the seed from comments/count so they don't all pick the same list index pattern
        return captionPool.random(random)
    }
}
