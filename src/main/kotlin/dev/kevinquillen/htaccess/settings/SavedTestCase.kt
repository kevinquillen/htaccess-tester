package dev.kevinquillen.htaccess.settings

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap

/**
 * Represents a saved test case that can be reloaded.
 */
@Tag("testCase")
data class SavedTestCase(
    @Tag("name")
    var name: String = "",

    @Tag("url")
    var url: String = "",

    @Tag("rules")
    var rules: String = "",

    @XMap
    var serverVariables: MutableMap<String, String> = mutableMapOf()
) {
    // No-arg constructor required for XML serialization
    constructor() : this("", "", "", mutableMapOf())
}
