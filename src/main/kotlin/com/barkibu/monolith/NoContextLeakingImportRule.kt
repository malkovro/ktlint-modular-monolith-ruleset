
package com.barkibu.monolith

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.core.ast.isRoot
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

@OptIn(FeatureInAlphaState::class)
class NoContextLeakingImportRule : Rule("no-context-leaking-import"), UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        moduleNamespaceDepth,
        excludedContextsProperty,
        authorizedContextsProperty
    )

    companion object {
        private const val KTLINT_MODULE_NAMESPACE_DEPTH_IDENTIFIER_NAME = "ktlint_module_namespace_depth"
        private const val KTLINT_MODULE_NAMESPACE_DEPTH_IDENTIFIER_DESCRIPTION = "Defines the depth of the namespace where the modules live"
        private const val KTLINT_EXCLUDED_CONTEXTS_IDENTIFIER_NAME = "ktlint_excluded_contexts"
        private const val KTLINT_AUTHORIZED_CONTEXTS_IDENTIFIER_NAME = "ktlint_authorized_contexts"
        private const val EXCLUDED_CONTEXTS_PROPERTY_DESCRIPTION = "Defines the modules that should be excluded from the rule"
        private const val AUTHORIZED_CONTEXTS_PROPERTY_DESCRIPTION = "Defines the contexts which can be imported everywhere"

        val moduleNamespaceDepth: UsesEditorConfigProperties.EditorConfigProperty<Int> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    KTLINT_MODULE_NAMESPACE_DEPTH_IDENTIFIER_NAME,
                    KTLINT_MODULE_NAMESPACE_DEPTH_IDENTIFIER_DESCRIPTION,
                    PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER
                ),
                defaultValue = 4
            )
        val excludedContextsProperty: UsesEditorConfigProperties.EditorConfigProperty<String> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    KTLINT_EXCLUDED_CONTEXTS_IDENTIFIER_NAME,
                    EXCLUDED_CONTEXTS_PROPERTY_DESCRIPTION,
                    PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER
                ),
                defaultValue = "infra"
            )
        val authorizedContextsProperty: UsesEditorConfigProperties.EditorConfigProperty<String> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    KTLINT_AUTHORIZED_CONTEXTS_IDENTIFIER_NAME,
                    AUTHORIZED_CONTEXTS_PROPERTY_DESCRIPTION,
                    PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER
                ),
                defaultValue = "core"
            )
    }

    private fun ASTNode.getEditorConfigStringArray(property: UsesEditorConfigProperties.EditorConfigProperty<String>): List<String> {
        return getEditorConfig(property).split(",")
    }

    private fun <T> ASTNode.getEditorConfig(property: UsesEditorConfigProperties.EditorConfigProperty<T>): T {
        var rootNode = this
        while (!rootNode.isRoot()) {
            rootNode = rootNode.treeParent
        }
        val editorConfigProperties = rootNode.getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
        return editorConfigProperties.getEditorConfigValue(property)
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val path = importDirective.importPath?.pathStr
            val packageDirective = importDirective.parent.parent.getChildOfType<KtPackageDirective>()

            if (packageDirective === null || path == null) {
                println("Cannot get the package directive or the imported path... ignoring this file")
                return
            }

            val importedNamespace = path.split(".")

            if (!packageDirective.belongsToSameApplication(importedNamespace)) {
                return
            }
            if (packageDirective.isExcludedContext()) {
                return
            }
            if (node.authorizesImportOf(importedNamespace)) {
                return
            }
            if (packageDirective.belongsToSameModule(importedNamespace)) {
                return
            }
            emit(node.startOffset, "Importing from another context package", false)
        }
    }

    private fun KtPackageDirective.belongsToSameApplication(importedPath: List<String>): Boolean {
        val applicationNamespaceDepth = node.getEditorConfig(moduleNamespaceDepth) - 2
        if (importedPath.size < applicationNamespaceDepth || fqName.pathSegments().size < applicationNamespaceDepth) {
            return false
        }
        for (index in 0..applicationNamespaceDepth) {
            if (fqName.pathSegments()[index].toString() != importedPath[index]) {
                return false
            }
        }
        return true
    }

    private fun KtPackageDirective.belongsToSameModule(importedPath: List<String>): Boolean {
        val moduleNamespaceDepthIndex = node.getEditorConfig(moduleNamespaceDepth) - 1
        if (importedPath.size < moduleNamespaceDepthIndex || fqName.pathSegments().size < moduleNamespaceDepthIndex) {
            return false
        }
        for (index in 0..moduleNamespaceDepthIndex) {
            if (fqName.pathSegments()[index].toString() != importedPath[index]) {
                return false
            }
        }
        return true
    }

    private fun KtPackageDirective.isExcludedContext(): Boolean {
        val excludedContexts = node.getEditorConfigStringArray(excludedContextsProperty)
        val moduleNamespaceDepthIndex = node.getEditorConfig(moduleNamespaceDepth) - 1
        return excludedContexts.contains(fqName.pathSegments()[moduleNamespaceDepthIndex].toString())
    }

    private fun ASTNode.authorizesImportOf(importedNamespace: List<String>): Boolean {
        val authorizedContexts = getEditorConfigStringArray(authorizedContextsProperty)
        val moduleNamespaceDepthIndex = getEditorConfig(moduleNamespaceDepth) - 1
        return authorizedContexts.contains(importedNamespace[moduleNamespaceDepthIndex])
    }
}
