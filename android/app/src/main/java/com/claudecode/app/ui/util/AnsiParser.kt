package com.claudecode.app.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object AnsiParser {

    private val ANSI_REGEX = Regex("\u001B\\[([0-9;]*)m")

    private val STANDARD_COLORS = arrayOf(
        Color(0xFF000000), // 0 Black
        Color(0xFFCC0000), // 1 Red
        Color(0xFF00CC00), // 2 Green
        Color(0xFFCCCC00), // 3 Yellow
        Color(0xFF0000CC), // 4 Blue
        Color(0xFFCC00CC), // 5 Magenta
        Color(0xFF00CCCC), // 6 Cyan
        Color(0xFFCCCCCC), // 7 White
    )

    private val BRIGHT_COLORS = arrayOf(
        Color(0xFF555555), // 0 Bright Black
        Color(0xFFFF5555), // 1 Bright Red
        Color(0xFF55FF55), // 2 Bright Green
        Color(0xFFFFFF55), // 3 Bright Yellow
        Color(0xFF5555FF), // 4 Bright Blue
        Color(0xFFFF55FF), // 5 Bright Magenta
        Color(0xFF55FFFF), // 6 Bright Cyan
        Color(0xFFFFFFFF), // 7 Bright White
    )

    fun parse(text: String, defaultColor: Color = Color(0xFFE6EDF3)): AnnotatedString {
        return buildAnnotatedString {
            var currentFg: Color = defaultColor
            var currentBg: Color? = null
            var bold = false
            var italic = false
            var underline = false
            var dim = false
            var strikethrough = false

            var lastEnd = 0
            val matches = ANSI_REGEX.findAll(text)

            for (match in matches) {
                // Append text before this escape sequence
                if (match.range.first > lastEnd) {
                    val segment = text.substring(lastEnd, match.range.first)
                    val style = buildStyle(currentFg, currentBg, bold, italic, underline, dim, strikethrough)
                    pushStyle(style)
                    append(segment)
                    pop()
                }
                lastEnd = match.range.last + 1

                // Parse SGR codes
                val codes = match.groupValues[1]
                    .split(";")
                    .filter { it.isNotEmpty() }
                    .map { it.toIntOrNull() ?: 0 }

                if (codes.isEmpty()) {
                    // ESC[m is same as ESC[0m
                    currentFg = defaultColor
                    currentBg = null
                    bold = false
                    italic = false
                    underline = false
                    dim = false
                    strikethrough = false
                    continue
                }

                var i = 0
                while (i < codes.size) {
                    when (codes[i]) {
                        0 -> {
                            currentFg = defaultColor
                            currentBg = null
                            bold = false
                            italic = false
                            underline = false
                            dim = false
                            strikethrough = false
                        }
                        1 -> bold = true
                        2 -> dim = true
                        3 -> italic = true
                        4 -> underline = true
                        9 -> strikethrough = true
                        22 -> { bold = false; dim = false }
                        23 -> italic = false
                        24 -> underline = false
                        29 -> strikethrough = false
                        in 30..37 -> currentFg = STANDARD_COLORS[codes[i] - 30]
                        38 -> {
                            // Extended foreground
                            if (i + 1 < codes.size && codes[i + 1] == 5 && i + 2 < codes.size) {
                                currentFg = get256Color(codes[i + 2])
                                i += 2
                            }
                        }
                        39 -> currentFg = defaultColor
                        in 40..47 -> currentBg = STANDARD_COLORS[codes[i] - 40]
                        48 -> {
                            // Extended background
                            if (i + 1 < codes.size && codes[i + 1] == 5 && i + 2 < codes.size) {
                                currentBg = get256Color(codes[i + 2])
                                i += 2
                            }
                        }
                        49 -> currentBg = null
                        in 90..97 -> currentFg = BRIGHT_COLORS[codes[i] - 90]
                        in 100..107 -> currentBg = BRIGHT_COLORS[codes[i] - 100]
                    }
                    i++
                }
            }

            // Append remaining text
            if (lastEnd < text.length) {
                val segment = text.substring(lastEnd)
                val style = buildStyle(currentFg, currentBg, bold, italic, underline, dim, strikethrough)
                pushStyle(style)
                append(segment)
                pop()
            }
        }
    }

    private fun buildStyle(
        fg: Color,
        bg: Color?,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        dim: Boolean,
        strikethrough: Boolean
    ): SpanStyle {
        val effectiveFg = if (dim) fg.copy(alpha = 0.5f) else fg
        return SpanStyle(
            color = effectiveFg,
            background = bg ?: Color.Unspecified,
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = when {
                underline && strikethrough -> TextDecoration.combine(
                    listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                )
                underline -> TextDecoration.Underline
                strikethrough -> TextDecoration.LineThrough
                else -> null
            }
        )
    }

    private fun get256Color(index: Int): Color {
        return when {
            index < 8 -> STANDARD_COLORS[index]
            index < 16 -> BRIGHT_COLORS[index - 8]
            index < 232 -> {
                // 216 color cube: 16 + 36*r + 6*g + b (r,g,b in 0..5)
                val adjusted = index - 16
                val r = (adjusted / 36) * 51
                val g = ((adjusted % 36) / 6) * 51
                val b = (adjusted % 6) * 51
                Color(r, g, b)
            }
            else -> {
                // Grayscale: 232-255 -> 8,18,...,238
                val gray = 8 + (index - 232) * 10
                Color(gray, gray, gray)
            }
        }
    }

    fun stripAnsi(text: String): String {
        return text.replace(Regex("\u001B\\[[0-9;]*m"), "")
    }
}
