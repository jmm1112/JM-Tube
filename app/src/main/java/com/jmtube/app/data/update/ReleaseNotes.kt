package com.jmtube.app.data.update

private const val REPO_URL = "https://github.com/jmm1112/JM-Tube"
private const val GITHUB_URL = "https://github.com"
private const val RELEASE_DATE_LABEL = "**release date"

/** Release notes split the way the update page shows them: an intro, then titled sections of items. */
data class ReleaseNotes(
    val intro: List<NoteText>,
    val sections: List<NoteSection>,
)

data class NoteSection(
    val title: String,
    val items: List<NoteText>,
)

data class NoteText(
    val spans: List<NoteSpan>,
)

sealed interface NoteSpan {
    data class Plain(
        val text: String,
    ) : NoteSpan

    data class Strong(
        val text: String,
    ) : NoteSpan

    data class Link(
        val text: String,
        val url: String,
    ) : NoteSpan
}

private val InlinePattern =
    Regex(
        """\*\*(.+?)\*\*""" +
            """|\[([^\]]+)]\((https?://[^)\s]+)\)""" +
            """|(?<![\w/&])#(\d+)\b""" +
            """|(?<![\w/])@([A-Za-z0-9][A-Za-z0-9-]{0,38})""" +
            """|(https?://[^\s)]+)""",
    )
private val BulletPattern = Regex("""^[-*+]\s+""")
private val HeadingPattern = Regex("""^(#{1,6})\s+""")
private val RulePattern = Regex("""^([-*_])\1{2,}$""")

/**
 * Reads the Markdown GitHub releases are written in: the `#` title and release date lines
 * are dropped, `##` headings open sections, bullets and paragraphs become items, and bold text,
 * links, `#123` issues and `@user` mentions keep their meaning.
 *
 * Items are aggregated so that at most one "New", one "Modify", and one "Fixed" section are produced.
 */
fun parseReleaseNotes(markdown: String): ReleaseNotes {
    val newItems = mutableListOf<NoteText>()
    val modifyItems = mutableListOf<NoteText>()
    val fixedItems = mutableListOf<NoteText>()

    var currentTitle = "New"
    var currentItems = mutableListOf<NoteText>()

    val ignoredKeywords = listOf(
        "license",
        "contact",
        "download",
        "install",
        "package",
        "about",
        "developer profile",
        "overview",
        "verification",
    )

    fun flushSection() {
        if (currentItems.isNotEmpty()) {
            val lowerTitle = currentTitle.lowercase()
            if (!ignoredKeywords.any { lowerTitle.contains(it) }) {
                val sectionCategory = when {
                    lowerTitle == "modify" || lowerTitle == "modified" || lowerTitle.startsWith("modify") || lowerTitle.startsWith("modified") -> "Modify"
                    lowerTitle == "fixed" || lowerTitle == "fix" || lowerTitle.startsWith("fixed") || lowerTitle.startsWith("fix") -> "Fixed"
                    lowerTitle.contains("fix") || lowerTitle.contains("bug") || lowerTitle.contains("repair") || lowerTitle.contains("issue") -> "Fixed"
                    lowerTitle.contains("modify") || lowerTitle.contains("modified") || lowerTitle.contains("improve") || lowerTitle.contains("improved") ||
                        lowerTitle.contains("refine") || lowerTitle.contains("ui") || lowerTitle.contains("design") || lowerTitle.contains("change") ||
                        lowerTitle.contains("update") || lowerTitle.contains("perf") || lowerTitle.contains("optimi") -> "Modify"
                    else -> "New"
                }

                currentItems.forEach { item ->
                    val plain = item.spans.joinToString("") {
                        when (it) {
                            is NoteSpan.Plain -> it.text
                            is NoteSpan.Strong -> it.text
                            is NoteSpan.Link -> it.text
                        }
                    }.trim().lowercase()

                    when {
                        plain.startsWith("[fix") || plain.startsWith("fix:") || plain.startsWith("fixed:") || plain.startsWith("bug:") -> {
                            fixedItems += item
                        }
                        plain.startsWith("[modify") || plain.startsWith("modify:") || plain.startsWith("modified:") ||
                            plain.startsWith("[improve") || plain.startsWith("improve:") || plain.startsWith("improved:") -> {
                            modifyItems += item
                        }
                        plain.startsWith("[new") || plain.startsWith("new:") || plain.startsWith("feature:") || plain.startsWith("add:") || plain.startsWith("added:") -> {
                            newItems += item
                        }
                        sectionCategory == "Fixed" -> {
                            if (plain.contains("optimi") || plain.contains("perf") || plain.contains("speed") || plain.contains("memory") || plain.contains("battery")) {
                                modifyItems += item
                            } else {
                                fixedItems += item
                            }
                        }
                        sectionCategory == "Modify" -> {
                            if (plain.contains("fix") || plain.contains("bug") || plain.contains("repair") || plain.contains("resolved")) {
                                fixedItems += item
                            } else {
                                modifyItems += item
                            }
                        }
                        else -> {
                            newItems += item
                        }
                    }
                }
            }
        }
        currentItems = mutableListOf()
    }

    markdown.replace("\r\n", "\n").lineSequence().map { it.trim() }.forEach { line ->
        val heading = HeadingPattern.find(line)
        when {
            line.isEmpty() || RulePattern.matches(line) -> Unit
            line.lowercase().startsWith(RELEASE_DATE_LABEL) -> Unit
            heading != null && heading.groupValues[1].length == 1 -> Unit
            heading != null -> {
                flushSection()
                currentTitle = plainText(line.removeRange(heading.range))
            }
            BulletPattern.containsMatchIn(line) -> {
                val itemText = line.replaceFirst(BulletPattern, "").trim()
                if (itemText.isNotBlank()) {
                    currentItems += parseInline(itemText)
                }
            }
        }
    }
    flushSection()

    val sections = mutableListOf<NoteSection>()
    if (newItems.isNotEmpty()) {
        sections += NoteSection("New", newItems)
    }
    if (modifyItems.isNotEmpty()) {
        sections += NoteSection("Modify", modifyItems)
    }
    if (fixedItems.isNotEmpty()) {
        sections += NoteSection("Fixed", fixedItems)
    }

    return ReleaseNotes(intro = emptyList(), sections = sections)
}

internal fun parseInline(text: String): NoteText {
    val spans = mutableListOf<NoteSpan>()
    var cursor = 0
    InlinePattern.findAll(text).forEach { match ->
        if (match.range.first > cursor) spans += NoteSpan.Plain(text.substring(cursor, match.range.first))
        val (bold, label, url, issue, user, bare) = match.destructured
        spans +=
            when {
                bold.isNotEmpty() -> NoteSpan.Strong(bold)
                label.isNotEmpty() -> NoteSpan.Link(label, url)
                issue.isNotEmpty() -> NoteSpan.Link("#$issue", "$REPO_URL/issues/$issue")
                user.isNotEmpty() -> NoteSpan.Link("@$user", "$GITHUB_URL/$user")
                else -> NoteSpan.Link(bare, bare)
            }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) spans += NoteSpan.Plain(text.substring(cursor))
    return NoteText(spans)
}

private fun plainText(markdown: String): String =
    parseInline(markdown).spans.joinToString("") { span ->
        when (span) {
            is NoteSpan.Plain -> span.text
            is NoteSpan.Strong -> span.text
            is NoteSpan.Link -> span.text
        }
    }
