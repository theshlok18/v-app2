package com.samai.assistant.coding

import javax.inject.Inject
import javax.inject.Singleton

enum class CodeLanguage(val extension: String, val displayName: String) {
    PYTHON("py", "Python"),
    HTML("html", "HTML"),
    CSS("css", "CSS"),
    JAVASCRIPT("js", "JavaScript"),
    JAVA("java", "Java"),
    KOTLIN("kt", "Kotlin")
}

data class CodeFile(
    val name: String,
    val content: String,
    val language: CodeLanguage,
    val path: String = ""
)

data class CodeProject(
    val name: String,
    val files: List<CodeFile>,
    val mainFile: String
)

data class ExecutionResult(
    val output: String,
    val errors: String,
    val exitCode: Int,
    val success: Boolean
)

@Singleton
class CodeRunner @Inject constructor() {

    fun detectLanguage(fileName: String): CodeLanguage {
        return when {
            fileName.endsWith(".py") -> CodeLanguage.PYTHON
            fileName.endsWith(".html") -> CodeLanguage.HTML
            fileName.endsWith(".css") -> CodeLanguage.CSS
            fileName.endsWith(".js") -> CodeLanguage.JAVASCRIPT
            fileName.endsWith(".java") -> CodeLanguage.JAVA
            fileName.endsWith(".kt") -> CodeLanguage.KOTLIN
            else -> CodeLanguage.PYTHON
        }
    }

    fun canExecuteLocally(language: CodeLanguage): Boolean {
        // HTML/JS can be previewed in WebView; Python needs sandboxed environment
        return language == CodeLanguage.HTML || language == CodeLanguage.JAVASCRIPT
    }

    fun buildPreviewHtml(project: CodeProject): String {
        val htmlFile = project.files.find { it.language == CodeLanguage.HTML }
        val cssFile = project.files.find { it.language == CodeLanguage.CSS }
        val jsFile = project.files.find { it.language == CodeLanguage.JAVASCRIPT }

        return buildString {
            append(htmlFile?.content ?: "<html><body></body></html>")
            if (cssFile != null) {
                insert(indexOf("</head>"), "<style>${cssFile.content}</style>")
            }
            if (jsFile != null) {
                insert(indexOf("</body>"), "<script>${jsFile.content}</script>")
            }
        }
    }

    fun generateHtmlFromCode(code: String): String {
        return if (code.trimStart().startsWith("<!DOCTYPE") || code.trimStart().startsWith("<html")) {
            code
        } else {
            "<!DOCTYPE html><html><head><title>SAM Preview</title></head><body>$code</body></html>"
        }
    }
}

@Singleton
class CodeDebugger @Inject constructor() {
    fun analyzeForErrors(code: String, language: CodeLanguage): List<String> {
        val issues = mutableListOf<String>()
        val lines = code.lines()

        lines.forEachIndexed { index, line ->
            if (language == CodeLanguage.PYTHON) {
                if (line.trimStart().startsWith("def ") && !line.trimEnd().endsWith(":")) {
                    issues.add("Line ${index + 1}: Missing colon after function definition")
                }
                if (line.contains("(") && !line.contains(")") && line.contains("print")) {
                    issues.add("Line ${index + 1}: Possible missing closing parenthesis")
                }
            }
        }
        return issues
    }
}
