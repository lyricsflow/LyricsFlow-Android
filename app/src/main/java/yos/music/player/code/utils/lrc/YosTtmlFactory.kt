package com.lyricsflow.player.code.utils.lrc

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max

/**
 * Full Apple Music / AMLL / LyricsFlow TTML Parser
 * Ported 1:1 from `main/js/ttml-parser.js` (`parseTTMLToLyrics`).
 */
class YosTtmlFactory {
    fun formatTtmlEntries(ttmlText: String): YosParsedLyricData? {
        if (!isTtmlText(ttmlText)) {
            return null
        }

        return runCatching {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeatureIfSupported("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
                setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val document = documentBuilderFactory.newDocumentBuilder().parse(
                InputSource(StringReader(ttmlText))
            )
            val rootElement = document.documentElement ?: return null
            if (rootElement.localTagName() != "tt") {
                return null
            }

            val bodyElement = rootElement.descendantsByLocalName("body").firstOrNull() ?: return null
            val subtitleByKey = rootElement.parseMetadataTextByKey("translations")
            val transliterationByKey = rootElement.parseMetadataTextByKey("transliterations")
            val songwriters = rootElement.parseSongwriters()
            val (headPrimaryAgent, oppositeAgents) = rootElement.parseAgentsInfo()
            val bodyAgent = bodyElement.readAttribute("agent") ?: bodyElement.readAttribute("ttm:agent")

            val lyricEntries = mutableListOf<List<Pair<Float, String>>>()
            val endTimes = mutableListOf<Float>()
            val rawLineAgents = mutableListOf<String?>()
            val otherSideForLines = mutableListOf<Boolean>()
            val transliterations = mutableListOf<String?>()
            val subtitles = mutableListOf<String?>()
            val syllablesForLines = mutableListOf<List<AmlSyllable>>()
            val backgroundLines = mutableListOf<AmlBackgroundLine?>()
            val sections = mutableMapOf<Int, String>()

            val divElements = bodyElement.directChildElementsByLocalName("div")
            val pWithSectionList = mutableListOf<Triple<Element, String?, String?>>()

            if (divElements.isNotEmpty()) {
                divElements.forEach { div ->
                    val songPart = div.readAttribute("songPart")
                        ?: div.readAttribute("itunes:songPart")
                    val divAgent = div.readAttribute("agent")
                        ?: div.readAttribute("ttm:agent")
                        ?: bodyAgent
                    val pList = div.descendantsByLocalName("p")
                    pList.forEachIndexed { idx, p ->
                        pWithSectionList.add(Triple(p, if (idx == 0) songPart else null, divAgent))
                    }
                }
            } else {
                bodyElement.descendantsByLocalName("p").forEach { p ->
                    pWithSectionList.add(Triple(p, null, bodyAgent))
                }
            }

            pWithSectionList.forEach { (lineElement, sectionName, inheritedAgent) ->
                val rawLineBegin = lineElement.readTimeAttribute("begin")
                val parsedLine = lineElement.parseRichParagraph(rawLineBegin ?: 0f)
                val leadSyllables = parsedLine.leadSyllables
                val lineBegin = rawLineBegin
                    ?: leadSyllables.firstOrNull()?.startTimeMs
                    ?: return@forEach

                val lineEnd = lineElement.readTimeAttribute("end")
                    ?: leadSyllables.lastOrNull()?.endTimeMs
                    ?: (lineBegin + 3000f)

                if (leadSyllables.isEmpty()) {
                    val plainText = lineElement.extractDirectLeadText().normalizeMetadataText()
                    if (plainText.isBlank()) {
                        return@forEach
                    }
                    val splitSyllables = splitSingleTextIntoWordSyllables(plainText, lineBegin, lineEnd)
                    val linePairs = buildLegacyLinePairs(lineBegin, splitSyllables)
                    val lyricKey = lineElement.readAttribute("key")
                    val lineIndex = lyricEntries.size
                    if (!sectionName.isNullOrBlank()) {
                        sections[lineIndex] = sectionName
                    }
                    val agentId = (lineElement.readAttribute("agent")
                        ?: lineElement.readAttribute("ttm:agent")
                        ?: inheritedAgent)?.trim()
                    lyricEntries.add(linePairs)
                    endTimes.add(lineEnd.coerceAtLeast(lineBegin))
                    rawLineAgents.add(agentId)
                    transliterations.add(
                        parsedLine.inlineRomanization
                            ?: lyricKey?.let { transliterationByKey[it] }
                    )
                    subtitles.add(
                        parsedLine.inlineTranslation
                            ?: lyricKey?.let { subtitleByKey[it] }
                    )
                    syllablesForLines.add(splitSyllables)
                    backgroundLines.add(parsedLine.backgroundLine)
                    return@forEach
                }

                val normalizedSyllables = if (leadSyllables.size == 1 && leadSyllables[0].text.trim().contains(' ')) {
                    splitSingleTextIntoWordSyllables(
                        leadSyllables[0].text.trim(),
                        leadSyllables[0].startTimeMs,
                        leadSyllables[0].endTimeMs
                    )
                } else {
                    leadSyllables
                }

                val linePairs = buildLegacyLinePairs(lineBegin, normalizedSyllables)
                val lyricKey = lineElement.readAttribute("key")
                val lineIndex = lyricEntries.size
                if (!sectionName.isNullOrBlank()) {
                    sections[lineIndex] = sectionName
                }
                val agentId = (lineElement.readAttribute("agent")
                    ?: lineElement.readAttribute("ttm:agent")
                    ?: inheritedAgent)?.trim()

                lyricEntries.add(linePairs)
                endTimes.add(lineEnd.coerceAtLeast(lineBegin))
                rawLineAgents.add(agentId)
                transliterations.add(
                    parsedLine.inlineRomanization
                        ?: lyricKey?.let { transliterationByKey[it] }
                )
                subtitles.add(
                    parsedLine.inlineTranslation
                        ?: lyricKey?.let { subtitleByKey[it] }
                )
                syllablesForLines.add(normalizedSyllables)
                backgroundLines.add(parsedLine.backgroundLine)
            }

            if (lyricEntries.isEmpty()) {
                return null
            }

            // Resolve Duet vs Non-Duet (`isOpposite` / `otherSideForLines`) ported from `main/js/ttml-parser.js` & `lyrics-applyer.js`
            val distinctLineAgents = rawLineAgents.mapNotNull { it?.lowercase() }.distinct()
            val primaryAgentLower = when {
                distinctLineAgents.contains("v1") -> "v1"
                headPrimaryAgent != null && distinctLineAgents.contains(headPrimaryAgent.lowercase()) -> headPrimaryAgent.lowercase()
                else -> distinctLineAgents.firstOrNull() ?: headPrimaryAgent?.lowercase()
            }
            val hasMultipleAgents = distinctLineAgents.size >= 2

            rawLineAgents.forEach { rawAgent ->
                val agentLower = rawAgent?.lowercase()
                val isOpposite = if (agentLower == null) {
                    false
                } else if (hasMultipleAgents && primaryAgentLower != null) {
                    agentLower != primaryAgentLower
                } else {
                    (agentLower == "v2" || agentLower == "v2000" || oppositeAgents.contains(agentLower)) &&
                        distinctLineAgents.size > 1
                }
                otherSideForLines.add(isOpposite)
            }

            // Guard: if every line was marked opposite (solo track with a single non-v1 agent), reset to false
            if (otherSideForLines.all { it }) {
                for (idx in otherSideForLines.indices) {
                    otherSideForLines[idx] = false
                }
            }

            YosParsedLyricData(
                entries = lyricEntries,
                endTimes = endTimes,
                otherSideForLines = otherSideForLines,
                transliterations = transliterations,
                subtitles = subtitles,
                isTtml = true,
                syllablesForLines = syllablesForLines,
                backgroundLines = backgroundLines,
                songwriters = songwriters,
                sections = sections
            )
        }.getOrNull()
    }

    companion object {
        fun isTtmlText(text: String): Boolean {
            val trimmedText = text.trimStart()
            return trimmedText.startsWith("<") && Regex("<\\s*tt(\\s|>|$)", RegexOption.IGNORE_CASE).containsMatchIn(trimmedText)
        }
    }

    private data class ParsedParagraphResult(
        val leadSyllables: List<AmlSyllable>,
        val backgroundLine: AmlBackgroundLine?,
        val inlineTranslation: String?,
        val inlineRomanization: String?
    )

    private fun buildLegacyLinePairs(
        lineBegin: Float,
        syllables: List<AmlSyllable>
    ): List<Pair<Float, String>> {
        val pairs = mutableListOf<Pair<Float, String>>()
        pairs.add(lineBegin to "")
        syllables.forEachIndexed { index, syl ->
            val suffix = if (!syl.isPartOfWord && index < syllables.lastIndex && !syl.text.endsWith(" ")) {
                " "
            } else {
                ""
            }
            pairs.add(syl.endTimeMs to (syl.text + suffix))
        }
        pairs.add(lineBegin to "")
        return pairs
    }

    private fun splitSingleTextIntoWordSyllables(
        text: String,
        startMs: Float,
        endMs: Float
    ): List<AmlSyllable> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        if (words.size == 1) {
            return listOf(
                AmlSyllable(
                    text = words[0],
                    startTimeMs = startMs,
                    endTimeMs = max(endMs, startMs + 100f),
                    isPartOfWord = false
                )
            )
        }
        val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
        val totalDur = max(endMs - startMs, 200f)
        var cursor = startMs
        return words.mapIndexed { idx, word ->
            val wordDur = totalDur * (word.length.toFloat() / totalChars.toFloat())
            val wordStart = cursor
            val wordEnd = if (idx == words.lastIndex) max(endMs, wordStart + 50f) else (wordStart + wordDur)
            cursor = wordEnd
            AmlSyllable(
                text = word,
                startTimeMs = wordStart,
                endTimeMs = wordEnd,
                isPartOfWord = false
            )
        }
    }

    private fun Element.parseRichParagraph(defaultBeginMs: Float): ParsedParagraphResult {
        val leadSyllables = mutableListOf<AmlSyllable>()
        val bgSyllables = mutableListOf<AmlSyllable>()
        var bgTranslatedText: String? = null
        var bgRomanizedText: String? = null
        var inlineTranslation: String? = null
        var inlineRomanization: String? = null

        var hasSpaceBeforeNextSpan = true
        val nodes = childNodes

        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    val raw = node.nodeValue.orEmpty()
                    if (raw.any { it.isWhitespace() }) {
                        hasSpaceBeforeNextSpan = true
                    }
                }

                Node.ELEMENT_NODE -> {
                    val el = node as Element
                    if (el.localTagName() == "span") {
                        val role = el.readAttribute("role")?.lowercase()
                        when (role) {
                            "x-bg" -> {
                                val extractedBg = el.extractBgSpan(defaultBeginMs)
                                bgSyllables.addAll(extractedBg.first)
                                if (extractedBg.second != null) bgTranslatedText = extractedBg.second
                                if (extractedBg.third != null) bgRomanizedText = extractedBg.third
                                hasSpaceBeforeNextSpan = true
                            }
                            "x-translation" -> {
                                val t = el.textContent.normalizeMetadataText()
                                if (t.isNotBlank()) inlineTranslation = t
                            }
                            "x-roman" -> {
                                val r = el.textContent.normalizeMetadataText()
                                if (r.isNotBlank()) inlineRomanization = r
                            }
                            else -> {
                                val sylBegin = el.readTimeAttribute("begin")
                                    ?: leadSyllables.lastOrNull()?.endTimeMs
                                    ?: defaultBeginMs
                                val sylEnd = el.readTimeAttribute("end")
                                    ?: (sylBegin + 250f)
                                val rawText = el.textContent.replace(Regex("[\\r\\n]+"), " ")
                                val trimmedLeading = rawText.trimStart()
                                val hadLeadingSpace = rawText.length != trimmedLeading.length
                                val cleanText = trimmedLeading.trimEnd()
                                val hadTrailingSpace = trimmedLeading.length != cleanText.length

                                if (leadSyllables.isNotEmpty() && !hasSpaceBeforeNextSpan && !hadLeadingSpace) {
                                    val prevIndex = leadSyllables.lastIndex
                                    leadSyllables[prevIndex] = leadSyllables[prevIndex].copy(isPartOfWord = true)
                                }

                                if (cleanText.isNotEmpty()) {
                                    leadSyllables.add(
                                        AmlSyllable(
                                            text = cleanText,
                                            startTimeMs = sylBegin,
                                            endTimeMs = max(sylEnd, sylBegin),
                                            isPartOfWord = false
                                        )
                                    )
                                }
                                hasSpaceBeforeNextSpan = hadTrailingSpace
                            }
                        }
                    }
                }
            }
        }

        // If no explicit x-bg span was present, check for parenthesized background vocals in leadSyllables
        if (bgSyllables.isEmpty() && leadSyllables.size > 1) {
            val separated = separateParenthesizedBgVocals(leadSyllables)
            if (separated.second.isNotEmpty() && separated.first.isNotEmpty()) {
                leadSyllables.clear()
                leadSyllables.addAll(separated.first)
                bgSyllables.addAll(separated.second)
            }
        }

        val bgLine = if (bgSyllables.isNotEmpty()) {
            val bgStart = bgSyllables.first().startTimeMs
            val bgEnd = bgSyllables.last().endTimeMs
            val leadStart = leadSyllables.firstOrNull()?.startTimeMs ?: defaultBeginMs
            AmlBackgroundLine(
                startTimeMs = bgStart,
                endTimeMs = max(bgEnd, bgStart + 200f),
                syllables = bgSyllables,
                placeAbove = bgStart < leadStart - 50f,
                translatedText = bgTranslatedText,
                romanizedText = bgRomanizedText
            )
        } else {
            null
        }

        return ParsedParagraphResult(
            leadSyllables = leadSyllables,
            backgroundLine = bgLine,
            inlineTranslation = inlineTranslation,
            inlineRomanization = inlineRomanization
        )
    }

    private fun Element.extractBgSpan(
        defaultBeginMs: Float
    ): Triple<List<AmlSyllable>, String?, String?> {
        val syllables = mutableListOf<AmlSyllable>()
        var translation: String? = null
        var romanization: String? = null
        var hasSpaceBefore = true

        val spanBegin = readTimeAttribute("begin") ?: defaultBeginMs
        val spanEnd = readTimeAttribute("end") ?: (spanBegin + 1500f)

        val children = childNodes
        var foundNestedTimedSpan = false

        for (i in 0 until children.length) {
            val node = children.item(i)
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    if (node.nodeValue.orEmpty().any { it.isWhitespace() }) {
                        hasSpaceBefore = true
                    }
                }
                Node.ELEMENT_NODE -> {
                    val childEl = node as Element
                    if (childEl.localTagName() == "span") {
                        val role = childEl.readAttribute("role")?.lowercase()
                        when (role) {
                            "x-translation" -> {
                                val t = childEl.textContent.normalizeMetadataText().trim('(', ')')
                                if (t.isNotBlank()) translation = t
                            }
                            "x-roman" -> {
                                val r = childEl.textContent.normalizeMetadataText().trim('(', ')')
                                if (r.isNotBlank()) romanization = r
                            }
                            else -> {
                                foundNestedTimedSpan = true
                                val b = childEl.readTimeAttribute("begin")
                                    ?: syllables.lastOrNull()?.endTimeMs
                                    ?: spanBegin
                                val e = childEl.readTimeAttribute("end") ?: spanEnd
                                val raw = childEl.textContent.replace(Regex("[\\r\\n]+"), " ")
                                val trimmedStart = raw.trimStart()
                                val hadLeadSpace = raw.length != trimmedStart.length
                                val cleaned = trimmedStart.trimEnd()
                                val hadTrailSpace = trimmedStart.length != cleaned.length

                                if (syllables.isNotEmpty() && !hasSpaceBefore && !hadLeadSpace) {
                                    val lastIdx = syllables.lastIndex
                                    syllables[lastIdx] = syllables[lastIdx].copy(isPartOfWord = true)
                                }

                                if (cleaned.isNotEmpty()) {
                                    syllables.add(
                                        AmlSyllable(
                                            text = cleaned,
                                            startTimeMs = b,
                                            endTimeMs = max(e, b),
                                            isPartOfWord = false
                                        )
                                    )
                                }
                                hasSpaceBefore = hadTrailSpace
                            }
                        }
                    }
                }
            }
        }

        if (!foundNestedTimedSpan) {
            val directText = extractDirectLeadText().normalizeMetadataText().trim('(', ')')
            if (directText.isNotBlank()) {
                syllables.addAll(splitSingleTextIntoWordSyllables(directText, spanBegin, spanEnd))
            }
        }

        // Strip outer parentheses on first/last background syllable just like ttml-parser.js
        if (syllables.isNotEmpty()) {
            val first = syllables.first()
            val strippedFirst = first.text.removePrefix("(").trimStart()
            if (strippedFirst != first.text) {
                if (strippedFirst.isEmpty()) {
                    syllables.removeAt(0)
                } else {
                    syllables[0] = first.copy(text = strippedFirst)
                }
            }
        }
        if (syllables.isNotEmpty()) {
            val lastIdx = syllables.lastIndex
            val last = syllables[lastIdx]
            val strippedLast = last.text.removeSuffix(")").trimEnd()
            if (strippedLast != last.text) {
                if (strippedLast.isEmpty()) {
                    syllables.removeAt(lastIdx)
                } else {
                    syllables[lastIdx] = last.copy(text = strippedLast)
                }
            }
        }

        return Triple(syllables, translation, romanization)
    }

    private fun separateParenthesizedBgVocals(
        input: List<AmlSyllable>
    ): Pair<List<AmlSyllable>, List<AmlSyllable>> {
        val lead = mutableListOf<AmlSyllable>()
        val bg = mutableListOf<AmlSyllable>()
        var inParens = false

        for (syl in input) {
            val t = syl.text
            if (!inParens && t.startsWith("(")) {
                inParens = true
                val cleaned = t.removePrefix("(").removeSuffix(")").trim()
                if (cleaned.isNotEmpty()) {
                    bg.add(syl.copy(text = cleaned, isPartOfWord = if (t.endsWith(")")) false else syl.isPartOfWord))
                }
                if (t.endsWith(")")) {
                    inParens = false
                }
            } else if (inParens) {
                val endsParen = t.endsWith(")")
                val cleaned = t.removeSuffix(")").trim()
                if (cleaned.isNotEmpty()) {
                    bg.add(syl.copy(text = cleaned, isPartOfWord = if (endsParen) false else syl.isPartOfWord))
                }
                if (endsParen) {
                    inParens = false
                }
            } else {
                lead.add(syl)
            }
        }
        return lead to bg
    }

    private fun Element.extractDirectLeadText(): String {
        val sb = StringBuilder()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
                sb.append(node.nodeValue)
            } else if (node.nodeType == Node.ELEMENT_NODE) {
                val el = node as Element
                val role = el.readAttribute("role")?.lowercase()
                if (role != "x-bg" && role != "x-translation" && role != "x-roman") {
                    sb.append(el.textContent)
                }
            }
        }
        return sb.toString()
    }

    private fun Element.parseSongwriters(): List<String> {
        val writers = mutableListOf<String>()
        descendantsByLocalName("songwriter").forEach { el ->
            val name = el.textContent.normalizeMetadataText()
            if (name.isNotBlank() && !writers.contains(name)) {
                writers.add(name)
            }
        }
        return writers
    }

    private fun Element.parseAgentsInfo(): Pair<String?, Set<String>> {
        val agents = mutableListOf<String>()
        val opposite = mutableSetOf<String>()
        var primaryAgent: String? = null
        descendantsByLocalName("agent").forEach { el ->
            val id = (el.readAttribute("id") ?: el.readAttribute("xml:id"))?.trim()?.lowercase() ?: return@forEach
            val type = el.readAttribute("type")?.lowercase()
            val role = el.readAttribute("role")?.lowercase()
            if (id == "v1" || role == "lead") {
                if (primaryAgent == null) primaryAgent = id
            }
            if (type == "person" || type == "character" || type == null) {
                if (primaryAgent == null) primaryAgent = id
                agents.add(id)
            } else if (type == "group" || type == "other") {
                opposite.add(id)
            }
        }
        if (agents.size >= 2) {
            val lead = primaryAgent ?: agents.first()
            agents.filter { it != lead }.forEach { opposite.add(it) }
        }
        return primaryAgent to opposite
    }

    private fun DocumentBuilderFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
        runCatching {
            setFeature(feature, enabled)
        }
    }

    private fun Element.parseMetadataTextByKey(containerLocalName: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        descendantsByLocalName(containerLocalName).forEach { containerElement ->
            containerElement.descendantsByLocalName("text").forEach { textElement ->
                val textKey = textElement.readAttribute("for") ?: return@forEach
                val text = textElement.textContent.normalizeMetadataText()
                if (text.isNotBlank() && !result.containsKey(textKey)) {
                    result[textKey] = text
                }
            }
        }

        return result
    }

    private fun Element.directChildElementsByLocalName(localName: String): List<Element> {
        val list = mutableListOf<Element>()
        val children = childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) {
                val el = n as Element
                if (el.localTagName() == localName) {
                    list.add(el)
                }
            }
        }
        return list
    }

    private fun Element.descendantsByLocalName(localName: String): List<Element> {
        val result = mutableListOf<Element>()

        fun visit(node: Node) {
            val childNodes = node.childNodes
            for (childIndex in 0 until childNodes.length) {
                val childNode = childNodes.item(childIndex)
                if (childNode.nodeType == Node.ELEMENT_NODE) {
                    val childElement = childNode as Element
                    if (childElement.localTagName() == localName) {
                        result.add(childElement)
                    }
                    visit(childElement)
                }
            }
        }

        visit(this)
        return result
    }

    private fun Element.readTimeAttribute(localName: String): Float? {
        return readAttribute(localName)?.parseTtmlTime()
    }

    private fun Element.readAttribute(localName: String): String? {
        if (hasAttribute(localName)) {
            return getAttribute(localName).takeIf { it.isNotBlank() }
        }

        for (attributeIndex in 0 until attributes.length) {
            val attribute = attributes.item(attributeIndex)
            if (attribute.localName == localName || attribute.nodeName.substringAfter(':') == localName) {
                return attribute.nodeValue.takeIf { it.isNotBlank() }
            }
        }

        return null
    }

    private fun Element.localTagName(): String {
        return localName ?: tagName.substringAfter(':')
    }

    private fun String.parseTtmlTime(): Float? {
        val raw = trim()
        if (raw.isBlank()) return null
        if (raw.endsWith("ms", ignoreCase = true)) {
            return raw.substring(0, raw.length - 2).trim().toFloatOrNull()
        }
        val normalizedTime = raw.removeSuffix("s").removeSuffix("S")
        if (normalizedTime.isBlank()) {
            return null
        }

        val timeParts = normalizedTime.split(':')
        val seconds = when (timeParts.size) {
            1 -> timeParts[0].toFloatOrNull()
            2 -> {
                val minutes = timeParts[0].toFloatOrNull() ?: return null
                val secs = timeParts[1].toFloatOrNull() ?: return null
                minutes * 60f + secs
            }
            3 -> {
                val hours = timeParts[0].toFloatOrNull() ?: return null
                val minutes = timeParts[1].toFloatOrNull() ?: return null
                val secs = timeParts[2].toFloatOrNull() ?: return null
                hours * 3600f + minutes * 60f + secs
            }
            else -> return null
        } ?: return null

        return seconds * 1000f
    }

    private fun String.normalizeMetadataText(): String {
        return replace(Regex("\\s+"), " ").trim()
    }
}
