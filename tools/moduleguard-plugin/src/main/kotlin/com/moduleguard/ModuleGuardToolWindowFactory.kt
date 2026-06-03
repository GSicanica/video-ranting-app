package com.moduleguard

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

class ModuleGuardToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        val logArea = JTextArea()
        logArea.isEditable = false
        logArea.lineWrap = true
        logArea.wrapStyleWord = true
        logArea.minimumSize = Dimension(200, 100)

        val runButton = JButton("Add Named Arguments + Compile")
        runButton.addActionListener {
            val result = addNamedArgumentsAndCompile(project = project)
            logArea.text = result
            Messages.showInfoMessage(project, result, "Named Arguments")
        }

        panel.add(runButton, BorderLayout.NORTH)
        panel.add(JBScrollPane(logArea), BorderLayout.CENTER)
        panel.border = JBUI.Borders.empty(8)

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun addNamedArgumentsAndCompile(project: Project): String {
        val base = project.baseDir ?: return "No project base directory."

        val baseline = runGradleTask(project = project, task = ":androidApp:compileDebugKotlin")
        if (!baseline.success) {
            return buildString {
                appendLine("Baseline compile: FAILED")
                appendLine()
                appendLine("No changes were made.")
                appendLine()
                appendLine("Gradle output (last 50 lines):")
                appendLine(baseline.tail)
            }
        }

        val maxAttempts = 6
        var attempt = 1
        val skipPaths = mutableSetOf<String>()
        var lastTail = ""

        while (attempt <= maxAttempts) {
            val report = applyAndCompileOnce(project = project, base = base, skipPaths = skipPaths, attempt = attempt)
            lastTail = report.tail
            if (report.success) {
                return report.message
            }
            // Rollback already done inside applyAndCompileOnce
            skipPaths.addAll(report.errorFiles)
            if (report.errorFiles.isEmpty()) {
                return finalizeNoOp(baseline.tail, report.tail)
            }
            attempt += 1
        }

        return finalizeNoOp(baselineTail = baseline.tail, lastTail = lastTail, skipped = skipPaths)
    }

    private data class AttemptReport(
        val success: Boolean,
        val message: String,
        val errorFiles: Set<String>,
        val tail: String
    )

    private fun applyAndCompileOnce(project: Project, base: VirtualFile, skipPaths: Set<String>, attempt: Int): AttemptReport {
        var filesTouched = 0
        var callsChanged = 0
        var argsNamed = 0
        var callsSkipped = 0

        val files = mutableListOf<VirtualFile>()
        VfsUtilCore.visitChildrenRecursively(base, object : VirtualFileVisitor<Any>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension == "kt") {
                    val path = file.path
                    if (!path.contains("/build/") &&
                        !path.contains("/.gradle/") &&
                        !path.contains("/generated/") &&
                        !path.contains("/build-logic/") &&
                        skipPaths.none { path.contains(it) }
                    ) {
                        files.add(file)
                    }
                }
                return true
            }
        })

        val paramIndex = buildParamIndex(files = files)
        val originalTexts = mutableMapOf<VirtualFile, String>()

        WriteCommandAction.runWriteCommandAction(project) {
            files.forEach { vf ->
                val text = VfsUtilCore.loadText(vf)
                val updated = applyNamedArgs(text, paramIndex)
                if (updated.changed) {
                    originalTexts[vf] = text
                    VfsUtilCore.saveText(vf, updated.text)
                    vf.refresh(false, false)
                    filesTouched += 1
                    callsChanged += updated.callsChanged
                    argsNamed += updated.argsNamed
                }
                callsSkipped += updated.callsSkipped
            }
        }

        val compileResult = runGradleTask(project = project, task = ":androidApp:compileDebugKotlin")
        if (!compileResult.success && originalTexts.isNotEmpty()) {
            WriteCommandAction.runWriteCommandAction(project) {
                originalTexts.forEach { (vf, original) ->
                    VfsUtilCore.saveText(vf, original)
                    vf.refresh(false, false)
                }
            }
        }

        val errorFiles = extractErrorFiles(tail = compileResult.tail, basePath = project.basePath ?: "")

        val message = buildString {
            appendLine("Attempt: $attempt")
            appendLine("Files scanned: ${files.size}")
            appendLine("Files touched: $filesTouched")
            appendLine("Calls changed: $callsChanged")
            appendLine("Arguments named: $argsNamed")
            appendLine("Calls skipped (unknown/ambiguous): $callsSkipped")
            appendLine()
            appendLine("Compile: ${if (compileResult.success) "SUCCESS" else "FAILED (changes reverted)"}")
            if (!compileResult.success) {
                appendLine("Rollback: reverted named args in modified files")
                appendLine("Error files: ${if (errorFiles.isEmpty()) "none" else errorFiles.joinToString(", ")}")
            }
            appendLine()
            appendLine("Gradle output (last 50 lines):")
            appendLine(compileResult.tail)
        }

        return AttemptReport(success = compileResult.success, message = message, errorFiles = errorFiles, tail = compileResult.tail)
    }

    private data class ApplyResult(
        val text: String,
        val changed: Boolean,
        val callsChanged: Int,
        val argsNamed: Int,
        val callsSkipped: Int
    )

    private data class GradleResult(
        val success: Boolean,
        val tail: String
    )

    private fun runGradleTask(project: Project, task: String): GradleResult {
        val basePath = project.basePath ?: return GradleResult(success = false, tail = "No project base path.")
        val cmd = listOf("/bin/sh", "-lc", "./gradlew $task")
        val process = ProcessBuilder(cmd)
            .directory(File(basePath))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val exit = process.waitFor()
        val tail = output.lines().takeLast(50).joinToString("\n")
        return GradleResult(success = exit == 0, tail = tail)
    }

    private fun extractErrorFiles(tail: String, basePath: String): Set<String> {
        val set = mutableSetOf<String>()
        val fileUrl = Regex("file:///([^:]+):\\d+:\\d+")
        val relPath = Regex("([A-Za-z0-9_./-]+\\.kt):\\d+:\\d+")
        fileUrl.findAll(tail).forEach { set.add(it.groupValues[1]) }
        relPath.findAll(tail).forEach { match ->
            val rel = match.groupValues[1]
            val abs = if (basePath.isNotEmpty()) File(basePath, rel).path else rel
            set.add(abs)
        }
        return set
    }

    private fun finalizeNoOp(baselineTail: String, lastTail: String, skipped: Set<String> = emptySet()): String {
        return buildString {
            appendLine("No safe changes could be applied without breaking the build.")
            appendLine("Result: SUCCESS (no changes applied)")
            if (skipped.isNotEmpty()) {
                appendLine("Skipped paths: ${skipped.joinToString(", ")}")
            }
            appendLine()
            appendLine("Baseline Gradle output (last 50 lines):")
            appendLine(baselineTail)
            if (lastTail.isNotEmpty()) {
                appendLine()
                appendLine("Last attempt Gradle output (last 50 lines):")
                appendLine(lastTail)
            }
        }
    }

    private fun buildParamIndex(files: List<VirtualFile>): Map<String, Map<Int, List<String>>> {
        val map = mutableMapOf<String, MutableMap<Int, List<String>>>()
        val ambiguous = mutableSetOf<Pair<String, Int>>()

        val funRegex = Regex("\\bfun\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(([^)]*)\\)")
        val classRegex = Regex("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(([^)]*)\\)")
        val dataClassRegex = Regex("\\bdata\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(([^)]*)\\)")

        files.forEach fileLoop@{ vf ->
            val text = VfsUtilCore.loadText(vf)
            val matches = funRegex.findAll(text) + classRegex.findAll(text) + dataClassRegex.findAll(text)
            matches.forEach matchLoop@{ m ->
                val name = m.groupValues[1]
                val params = parseParamNames(paramList = m.groupValues[2])
                if (params.isEmpty()) return@matchLoop
                val key = name to params.size
                if (ambiguous.contains(key)) return@matchLoop
                val bucket = map.getOrPut(name) { mutableMapOf() }
                val existing = bucket[params.size]
                if (existing != null && existing != params) {
                    ambiguous.add(key)
                    bucket.remove(params.size)
                } else {
                    bucket[params.size] = params
                }
            }
        }

        return map
    }

    private fun parseParamNames(paramList: String): List<String> {
        return paramList.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { part ->
                val noDefault = part.substringBefore('=')
                val cleaned = noDefault
                    .replace(Regex("@[A-Za-z_][A-Za-z0-9_]*(\\([^)]*\\))?\\s*"), "")
                    .replace(
                        Regex("\\b(private|public|protected|internal|override|lateinit|const|suspend|crossinline|noinline|vararg|val|var)\\b\\s*"),
                        ""
                    )
                    .trim()
                val match = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\b").find(cleaned)
                val name = match?.groupValues?.get(1)?.trim().orEmpty()
                if (name.isEmpty()) null else name
            }
    }

    private fun applyNamedArgs(
        text: String,
        paramIndex: Map<String, Map<Int, List<String>>>
    ): ApplyResult {
        val sb = StringBuilder()
        var idx = 0
        var changed = false
        var callsChanged = 0
        var argsNamed = 0
        var callsSkipped = 0

        while (idx < text.length) {
            val ch = text[idx]
            if (ch.isLetter() || ch == '_') {
                val start = idx
                idx += 1
                while (idx < text.length && (text[idx].isLetterOrDigit() || text[idx] == '_')) {
                    idx += 1
                }
                val name = text.substring(start, idx)
                if (!isEligibleCallSite(text = text, nameStart = start, name = name)) {
                    sb.append(name)
                    continue
                }
                val next = skipWhitespace(text = text, start = idx)
                if (next < text.length && text[next] == '(') {
                    val close = findClosingParen(text = text, openIdx = next)
                    if (close > next) {
                        val argsText = text.substring(next + 1, close)
                        val args = splitArgs(text = argsText)
                        val paramNames = paramIndex[name]?.get(args.size)
                        if (paramNames != null && args.isNotEmpty() && args.size == paramNames.size) {
                            if (args.any { it.contains('=') }) {
                                callsSkipped += 1
                                sb.append(name)
                                sb.append(text.substring(idx, close + 1))
                                idx = close + 1
                                continue
                            }
                            val newArgs = args.mapIndexed { i, arg ->
                                val trimmed = arg.trim()
                                "${paramNames[i]} = $trimmed"
                            }
                            sb.append(name)
                            sb.append(text.substring(idx, next))
                            sb.append('(')
                            sb.append(newArgs.joinToString(", "))
                            sb.append(')')
                            idx = close + 1
                            changed = true
                            callsChanged += 1
                            argsNamed += args.size
                            continue
                        } else if (args.isNotEmpty()) {
                            callsSkipped += 1
                        }
                    }
                }
                sb.append(name)
                continue
            }
            sb.append(ch)
            idx += 1
        }

        return ApplyResult(
            text = sb.toString(),
            changed = changed,
            callsChanged = callsChanged,
            argsNamed = argsNamed,
            callsSkipped = callsSkipped
        )
    }

    private fun isEligibleCallSite(text: String, nameStart: Int, name: String): Boolean {
        if (isDeclarationContext(text = text, nameStart = nameStart)) return false
        if (isQualifiedCall(text = text, nameStart = nameStart)) return false
        if (isBlacklistedName(name = name)) return false
        return true
    }

    private fun isBlacklistedName(name: String): Boolean {
        return name in setOf(
            "apply",
            "append",
            "configure",
            "set",
            "add",
            "remove",
            "put",
            "get"
        )
    }

    private fun isDeclarationContext(text: String, nameStart: Int): Boolean {
        val prevWord = findPrevWord(text = text, start = nameStart)
        if (prevWord.isEmpty()) return false
        return prevWord in setOf(
            "fun",
            "class",
            "interface",
            "object",
            "constructor",
            "annotation",
            "typealias",
            "override",
            "val",
            "var"
        )
    }

    private fun isQualifiedCall(text: String, nameStart: Int): Boolean {
        var i = nameStart - 1
        while (i >= 0 && text[i].isWhitespace()) i -= 1
        if (i < 0) return false
        // Skip member/qualified calls like x.foo(), obj?.foo(), pkg.foo(), Foo.Companion.foo()
        if (text[i] == '.') return true
        if (text[i] == '?' && i - 1 >= 0 && text[i - 1] == '.') return true
        if (text[i] == ':' && i - 1 >= 0 && text[i - 1] == ':') return true
        return false
    }

    private fun findPrevWord(text: String, start: Int): String {
        var i = start - 1
        while (i >= 0 && text[i].isWhitespace()) i -= 1
        if (i < 0) return ""
        var end = i
        while (i >= 0 && (text[i].isLetterOrDigit() || text[i] == '_')) i -= 1
        val begin = i + 1
        return if (begin <= end) text.substring(begin, end + 1) else ""
    }

    private fun skipWhitespace(text: String, start: Int): Int {
        var i = start
        while (i < text.length && text[i].isWhitespace()) i += 1
        return i
    }

    private fun findClosingParen(text: String, openIdx: Int): Int {
        var depth = 0
        var i = openIdx
        while (i < text.length) {
            val ch = text[i]
            if (ch == '(') depth += 1
            if (ch == ')') {
                depth -= 1
                if (depth == 0) return i
            }
            i += 1
        }
        return -1
    }

    private fun splitArgs(text: String): List<String> {
        val args = mutableListOf<String>()
        var depth = 0
        var last = 0
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '(') depth += 1
            if (ch == ')') depth -= 1
            if (ch == ',' && depth == 0) {
                args.add(text.substring(last, i))
                last = i + 1
            }
            i += 1
        }
        val tail = text.substring(last).trim()
        if (tail.isNotEmpty()) args.add(tail)
        return args
    }
}
