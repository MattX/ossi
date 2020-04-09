package io.terbium.ossi

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

private val parser = Parser.builder().build()
private val renderer = HtmlRenderer.builder()
    .escapeHtml(true)
    .sanitizeUrls(true)
    .build()

fun renderMarkdown(markdown: String): String = renderer.render(parser.parse(markdown))
