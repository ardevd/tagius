package net.ardevd.tagius.core.ui

import java.util.regex.Pattern

// Regex for tags (Start with #, followed by non-whitespace characters)
val tagRegexPattern = Pattern.compile("#\\S+")