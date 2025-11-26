package net.ardevd.tagius.core.utils

import kotlin.random.Random

object KeyGenerator {
    private const val CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    /**
     * Generate a random key string for a timetagger record
     * timetagger itself uses keys with a length of 8.
     * Tagius uses a length of 12 by default for more randomness.
     */
    fun generateKey(length: Int = 12): String {
        return (1..length)
            .map { Random.nextInt(0, CHAR_POOL.length) }
            .map(CHAR_POOL::get)
            .joinToString("")
    }
}