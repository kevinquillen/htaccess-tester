package com.github.kevinquillen.htaccess.engine.parser

sealed class Directive {
    abstract val sourceLineNo: Int
    abstract val rawLine: String

    data class RewriteEngine(
        override val sourceLineNo: Int,
        override val rawLine: String,
        val enabled: Boolean
    ) : Directive()

    data class RewriteCond(
        override val sourceLineNo: Int,
        override val rawLine: String,
        val testString: String,
        val pattern: String,
        val flags: Set<CondFlag> = emptySet()
    ) : Directive()

    data class RewriteRule(
        override val sourceLineNo: Int,
        override val rawLine: String,
        val pattern: String,
        val substitution: String,
        val flags: Set<RuleFlag> = emptySet()
    ) : Directive()

    data class Comment(
        override val sourceLineNo: Int,
        override val rawLine: String
    ) : Directive()

    data class BlankLine(
        override val sourceLineNo: Int,
        override val rawLine: String
    ) : Directive()

    data class Unknown(
        override val sourceLineNo: Int,
        override val rawLine: String,
        val error: String? = null
    ) : Directive()
}

enum class CondFlag {
    NC,
    OR,
    NV
}

sealed class RuleFlag {
    object L : RuleFlag()
    object END : RuleFlag()
    object NC : RuleFlag()
    object QSA : RuleFlag()
    object NE : RuleFlag()
    object N : RuleFlag()
    object F : RuleFlag()
    object G : RuleFlag()
    object PT : RuleFlag()
    data class R(val statusCode: Int = 302) : RuleFlag()
    data class Unknown(val raw: String) : RuleFlag()

    override fun toString(): String = when (this) {
        is L -> "L"
        is END -> "END"
        is NC -> "NC"
        is QSA -> "QSA"
        is NE -> "NE"
        is N -> "N"
        is F -> "F"
        is G -> "G"
        is PT -> "PT"
        is R -> if (statusCode == 302) "R" else "R=$statusCode"
        is Unknown -> raw
    }
}
