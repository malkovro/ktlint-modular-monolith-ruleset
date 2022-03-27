package com.barkibu.monolith

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoContextLeakingImportRuleTest {
    @Test
    fun `Importing from another context package`() {
        // whenever KTLINT_DEBUG env variable is set to "ast" or -DktlintDebug=ast is used
        // com.pinterest.ktlint.test.(lint|format) will print AST (along with other debug info) to the stderr.
        // this can be extremely helpful while writing and testing rules.
        // uncomment the line below to take a quick look at it
        // System.setProperty("ktlintDebug", "ast")
        val code =
            """
            package com.barkibu.app.contextA
            
            import com.barkibu.app.contextB.MyClass
            
            fun fn() {
                var v = "var"
            }
            """.trimIndent()
        assertThat(NoContextLeakingImportRule().lint(code))
            .isEqualTo(listOf(LintError(3, 1, "no-context-leaking-import", "Importing from another context package")))
    }

    @Test
    fun `Importing from same context package`() {
        val code =
            """
            package com.barkibu.app.contextA
            
            import com.barkibu.app.contextA.MyClass
            
            fun fn() {
                var v = "var"
            }
            """.trimIndent()
        assertThat(NoContextLeakingImportRule().lint(code)).isEmpty()
    }

    @Test
    fun `Importing from common context package`() {
        val code =
            """
            package com.barkibu.app.contextA
            
            import com.barkibu.app.core.MyClass
            
            fun fn() {
                var v = "var"
            }
            """.trimIndent()
        assertThat(NoContextLeakingImportRule().lint(code)).isEmpty()
    }

    @Test
    fun `Importing in the infra package`() {
        val code =
            """
            package com.barkibu.app.infra
            
            import com.barkibu.app.core.MyClass
            
            fun fn() {
                var v = "var"
            }
            """.trimIndent()
        assertThat(NoContextLeakingImportRule().lint(code)).isEmpty()
    }
}
