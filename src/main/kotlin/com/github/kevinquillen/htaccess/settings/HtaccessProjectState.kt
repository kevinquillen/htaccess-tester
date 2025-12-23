package com.github.kevinquillen.htaccess.settings

import com.intellij.util.xmlb.annotations.XCollection

/**
 * Persistent state for htaccess tester project-level data.
 */
data class HtaccessProjectState(
    @XCollection(style = XCollection.Style.v2)
    var savedTestCases: MutableList<SavedTestCase> = mutableListOf()
)
