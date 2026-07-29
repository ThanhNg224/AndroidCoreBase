package com.thanhng224.androidxmlbase.core.testing

import com.thanhng224.androidxmlbase.core.localization.AppLocaleApplier

/** [AppLocaleApplier] that records applied locale tags instead of touching AppCompat. */
public class FakeAppLocaleApplier(
    private var currentTags: String = "",
) : AppLocaleApplier {
    public val appliedTags: MutableList<String> = mutableListOf()

    override fun applyLocales(tag: String) {
        appliedTags += tag
        currentTags = tag
    }

    override fun currentLocaleTags(): String = currentTags
}
