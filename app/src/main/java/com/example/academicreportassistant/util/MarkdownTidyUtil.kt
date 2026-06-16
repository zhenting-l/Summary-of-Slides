package com.lzt.summaryofslides.util

object MarkdownTidyUtil {
    private val noUnderscoreCommandPattern =
        Regex(
            """\\(mathbb|mathcal|mathrm|mathbf|mathsf|mathtt|operatorname|sqrt|text|begin|end|frac|bar|hat|tilde|vec|overline)_\{([^{}]+)\}""",
        )
    private val noUnderscoreCommandSingleTokenPattern =
        Regex(
            """\\(mathbb|mathcal|mathrm|mathbf|mathsf|mathtt|operatorname|sqrt|text|begin|end|bar|hat|tilde|vec|overline)_([A-Za-z]+)""",
        )

    fun tidy(raw: String): String {
        var md = unescapeStrayJsonEscapes(raw)
        md = md.replace("\r\n", "\n").trim()
        md = stripMathCodeFences(md)
        md = ensureDoubleDollarStandalone(md)
        md = md.replace("\\\\(", "\$")
            .replace("\\\\)", "\$")
            .replace("\\\\[", "\$\$")
            .replace("\\\\]", "\$\$")
        md = md.replace(Regex("\\$\\\\\\s*\\n")) { "${'$'}\n" }
        md = normalizeBlockMathDelimiters(md)
        md = fixInlineMathNewlines(md)
        md = fixLatexInMathSegments(md)
        md = removeMarkdownLineEndBackslashes(md)
        md = md.replace(Regex("\n{3,}"), "\n\n").trim()
        return md + "\n"
    }

    private fun unescapeStrayJsonEscapes(s: String): String {
        if (s.isEmpty()) return s
        if (s.indexOf('\\') < 0) return s
        var out = s
        out = out.replace("\\n", "\n")
        out = out.replace("\\\\", "\u0000")
        out = out.replace("\u0000", "\\")
        return out
    }

    private fun stripMathCodeFences(md: String): String {
        return md.replace(Regex("```(?:latex|math)\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)) { m ->
            m.groupValues.getOrNull(1).orEmpty().trim()
        }
    }

    private fun ensureDoubleDollarStandalone(md: String): String {
        val lines = md.replace("\r\n", "\n").split('\n')
        val out = StringBuilder(md.length + 32)
        var inCodeFence = false
        for (line0 in lines) {
            val line = line0
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence
                out.append(line).append('\n')
                continue
            }
            if (!inCodeFence && line.contains("$$") && line.trim() != "$$") {
                out.append(line.replace("$$", "\n$$\n")).append('\n')
                continue
            }
            out.append(line).append('\n')
        }
        return out.toString().trimEnd()
    }

    private fun normalizeBlockMathDelimiters(md: String): String {
        return md.replace(Regex("\\$\\$([^\\n]*?)\\$\\$")) { m ->
            val inner = m.groupValues.getOrNull(1).orEmpty().trim()
            if (inner.isBlank()) m.value
            else "$$\n$inner\n$$"
        }
    }

    private fun fixInlineMathNewlines(md: String): String {
        val out = StringBuilder(md.length)
        var i = 0
        var inInline = false
        var inBlock = false
        while (i < md.length) {
            val c = md[i]
            if (c == '$') {
                val isDouble = i + 1 < md.length && md[i + 1] == '$'
                if (isDouble) {
                    inBlock = !inBlock
                    out.append("$$")
                    i += 2
                    continue
                }
                if (!inBlock) inInline = !inInline
                out.append('$')
                i += 1
                continue
            }
            if (c == '\n' && inInline && !inBlock) {
                out.append(' ')
                i += 1
                continue
            }
            out.append(c)
            i += 1
        }
        return out.toString()
    }

    private fun fixLatexInMathSegments(md: String): String {
        val out = StringBuilder(md.length)
        var i = 0
        var inInline = false
        var inBlock = false
        var segmentStart = -1
        while (i < md.length) {
            val c = md[i]
            if (c == '$') {
                val isDouble = i + 1 < md.length && md[i + 1] == '$'
                if (isDouble) {
                    if (!inInline && !inBlock) {
                        inBlock = true
                        segmentStart = i + 2
                        out.append("$$")
                        i += 2
                        continue
                    }
                    if (inBlock) {
                        val rawSeg = md.substring(segmentStart.coerceAtLeast(0), i)
                        out.append(fixLatexSegment(rawSeg))
                        out.append("$$")
                        inBlock = false
                        segmentStart = -1
                        i += 2
                        continue
                    }
                    out.append("$$")
                    i += 2
                    continue
                }
                if (inBlock) {
                    i += 1
                    continue
                }
                if (!inBlock && !inInline) {
                    inInline = true
                    segmentStart = i + 1
                    out.append('$')
                    i += 1
                    continue
                }
                if (inInline) {
                    val rawSeg = md.substring(segmentStart.coerceAtLeast(0), i)
                    out.append(fixLatexSegment(rawSeg))
                    out.append('$')
                    inInline = false
                    segmentStart = -1
                    i += 1
                    continue
                }
                out.append('$')
                i += 1
                continue
            }
            if (!inInline && !inBlock) out.append(c)
            i += 1
        }
        if (inInline || inBlock) {
            val rawSeg = md.substring(segmentStart.coerceAtLeast(0))
            out.append(fixLatexSegment(rawSeg))
        }
        return out.toString()
    }

    private fun fixLatexSegment(raw: String): String {
        return runCatching {
            val normalized = normalizeSafeLatex(raw)
            val targeted = repairTargetedLatexMistakes(normalized)
            val candidate = when {
                targeted == normalized -> normalized
                shouldPreferTargetedRepair(normalized, targeted) -> targeted
                else -> normalized
            }
            candidate
        }.getOrDefault(raw)
    }

    private fun removeMarkdownLineEndBackslashes(md: String): String {
        val lines = md.replace("\r\n", "\n").split('\n')
        val out = StringBuilder(md.length)
        var inCodeFence = false
        var inMathBlock = false
        for (line0 in lines) {
            val line = line0
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence
                out.append(line).append('\n')
                continue
            }
            if (!inCodeFence) {
                val t = line.trim()
                if (t.startsWith("$$")) {
                    inMathBlock = !inMathBlock
                    out.append(line).append('\n')
                    continue
                }
            }
            if (!inCodeFence && !inMathBlock) {
                val t = line.trim()
                if (t == "\\") {
                    out.append('\n')
                    continue
                }
                if (line.endsWith("\\") && !line.endsWith("\\\\")) {
                    out.append(line.dropLast(1)).append('\n')
                    continue
                }
            }
            out.append(line).append('\n')
        }
        return out.toString().trimEnd()
    }

    private fun normalizeSafeLatex(raw: String): String {
        var s = raw
        s = s.replace('，', ',').replace('。', '.').replace('：', ':').replace('；', ';')
        s = s.replace('（', '(').replace('）', ')').replace('【', '[').replace('】', ']')
        s = s.replace('−', '-')
        s = s.replace(Regex("""\\\s*abla\b"""), "\\\\nabla")
        s = s.replace(Regex("""\babla\b"""), "\\\\nabla")
        s = s.replace(Regex("""\beq\s+0\b"""), "\\\\neq 0")
        val leftCount = Regex("""\\left\b""").findAll(s).count()
        val rightCount = Regex("""\\right\b""").findAll(s).count()
        if (leftCount != rightCount) {
            s = s.replace("\\left", "").replace("\\right", "")
        }
        s = s.replace("\\left{", "\\left\\{")
        s = s.replace("\\right}", "\\right\\}")
        return s
    }

    private fun repairTargetedLatexMistakes(raw: String): String {
        var s = raw
        s = s.replace("\\arg\\min{", "\\arg\\min_{")
        s = s.replace(Regex("""\\(bar|hat|tilde|vec|overline)\{([a-zA-Z])\}([a-zA-Z0-9])\b""")) { m ->
            val cmd = m.groupValues[1]
            val sym = m.groupValues[2]
            val sub = m.groupValues[3]
            "\\\\$cmd{$sym}_{$sub}"
        }
        s = s.replace(Regex("""\\hat\{([^}]+)\}\{([^}]+)\}""")) { m ->
            val a = m.groupValues[1]
            val idx = m.groupValues[2]
            "\\hat{$a}_{$idx}"
        }
        s = s.replace(Regex("""D\{\\text\{KL\}\}"""), "D_{\\\\text{KL}}")
        s = s.replace(Regex("""D\{\\mathrm\{KL\}\}"""), "D_{\\\\mathrm{KL}}")
        s = s.replace(Regex("""\\pi\{(\\theta[^}]*)\}""")) { m ->
            "\\\\pi_{" + m.groupValues[1] + "}"
        }
        s = s.replace(Regex("""\\hat\{A\}\{i,t\}"""), "\\\\hat{A}_{i,t}")
        s = s.replace(Regex("""\\hat\{A\}\{i, t\}"""), "\\\\hat{A}_{i,t}")
        s = s.replace(Regex("""\Q\}{\E([a-zA-Z]+\s*=\s*[^}]+)\}""")) { m ->
            "\\\\}_{" + m.groupValues[1] + "}"
        }
        s = s.replace(noUnderscoreCommandPattern) { m ->
            val command = m.groupValues[1]
            val arg = m.groupValues[2]
            when (command) {
                "frac" -> "\\frac{$arg}"
                else -> "\\$command{$arg}"
            }
        }
        s = s.replace(Regex("""\\frac_\{([^{}]+)\}\{([^{}]+)\}""")) { m ->
            val numerator = m.groupValues[1]
            val denominator = m.groupValues[2]
            "\\frac{$numerator}{$denominator}"
        }
        s = s.replace(noUnderscoreCommandSingleTokenPattern) { m ->
            val command = m.groupValues[1]
            val arg = m.groupValues[2]
            "\\$command{$arg}"
        }
        return s
    }

    private fun shouldPreferTargetedRepair(original: String, repaired: String): Boolean {
        if (original == repaired) return false
        if (repaired.isBlank()) return false
        val originalSuspicious = countSuspiciousPatterns(original)
        val repairedSuspicious = countSuspiciousPatterns(repaired)
        if (repairedSuspicious < originalSuspicious) return true
        if (repairedSuspicious > originalSuspicious) return false
        return false
    }

    private fun countSuspiciousPatterns(text: String): Int {
        var count = 0
        count += noUnderscoreCommandPattern.findAll(text).count()
        count += noUnderscoreCommandSingleTokenPattern.findAll(text).count()
        count += Regex("""\\frac_\{""").findAll(text).count()
        count += Regex("""\\(mathbb|mathcal|mathrm|mathbf|mathsf|mathtt|operatorname|sqrt|text|begin|end|frac|bar|hat|tilde|vec|overline)\s+[A-Za-z]""").findAll(text).count()
        return count
    }
}
