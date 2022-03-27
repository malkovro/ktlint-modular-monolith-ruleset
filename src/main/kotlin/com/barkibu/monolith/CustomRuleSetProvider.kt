package com.barkibu.monolith

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {
    override fun get(): RuleSet = RuleSet("custom", NoopRule())
}