package com.github.kevinquillen.htaccess.engine.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun `parse rewrite cond with HTTPS off`() {
        val content = "RewriteCond %{HTTPS} off"
        val directives = HtaccessParser.parse(content)

        assertEquals(1, directives.size)
        val cond = directives[0] as Directive.RewriteCond
        assertEquals("%{HTTPS}", cond.testString)
        assertEquals("off", cond.pattern)
    }

    @Test
    fun `parse rewrite rule with redirect`() {
        val content = "RewriteRule ^(.*)\$ https://example.com/\$1 [R=301,L]"
        val directives = HtaccessParser.parse(content)

        assertEquals(1, directives.size)
        val rule = directives[0] as Directive.RewriteRule
        assertEquals("^(.*)\$", rule.pattern)
        assertEquals("https://example.com/\$1", rule.substitution)
        assertTrue(rule.flags.any { it is RuleFlag.L })
        assertTrue(rule.flags.any { it is RuleFlag.R && (it as RuleFlag.R).statusCode == 301 })
    }

    @Test
    fun `parse rewrite rule with trailing slash`() {
        val content = "RewriteRule ^(.+[^/])\$ \$1/ [R=301,L]"
        val directives = HtaccessParser.parse(content)

        assertEquals(1, directives.size)
        val rule = directives[0] as Directive.RewriteRule
        assertEquals("^(.+[^/])\$", rule.pattern)
        assertEquals("\$1/", rule.substitution)
    }
}
