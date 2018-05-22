package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language

fun fileTree(block: FileTreeBuilder.() -> Unit): FileTree = FileTree(FileTreeBuilderImpl().apply(block).intoDirectory())

interface FileTreeBuilder {
  fun dir(name: String, block: FileTreeBuilder.() -> Unit = {})
  fun file(name: String, code: String? = null)
  fun java(name: String, @Language("JAVA") code: String) = file(name, code)
  fun kotlin(name: String, @Language("kotlin") code: String) = file(name, code)
  fun python(name: String, @Language("Python") code: String) = file(name, code)
  fun scala(name: String, @Language("Scala") code: String) = file(name, code)
}

class FileTree(private val rootDirectory: Entry.Directory) {

  fun assertEquals(baseDir: VirtualFile, fixture: CodeInsightTestFixture? = null) {

    fun go(expected: Entry.Directory, actual: VirtualFile) {
      val actualChildren = actual.children
        .filter { it.name !in IGNORED_FILES }
        .associateBy { it.name }
      check(expected.children.keys == actualChildren.keys) {
        "Mismatch in directory ${actual.path}\n" +
                "Expected: ${expected.children.keys}\n" +
                "Actual  : ${actualChildren.keys}"
      }

      for ((name, entry) in expected.children) {
        val child = actualChildren[name]!!
        when (entry) {
          is Entry.File -> {
            check(!child.isDirectory)
            if (fixture != null && entry.text != null) {
              fixture.openFileInEditor(child)
              fixture.checkResult(entry.text)
            }
            else if(entry.text != null) {
              val actualText = VfsUtil.loadText(child)
              check(actualText == entry.text, { "Expected: \n${entry.text} \n Actual: \n${actualText}" })
            }
          }
          is Entry.Directory -> go(entry, child)
        }
      }
    }

    fullyRefreshDirectory(baseDir)
    go(rootDirectory, baseDir)
  }

  fun create(root: VirtualFile) {
    fun go(dir: Entry.Directory, root: VirtualFile) {
      for ((name, entry) in dir.children) {
        when (entry) {
          is Entry.File -> {
            val vFile = root.findOrCreateChildData(root, name)
            VfsUtil.saveText(vFile, entry.text ?: "")
          }
          is Entry.Directory -> {
            go(entry, root.createChildDirectory(root, name))
          }
        }
      }
    }

    runWriteAction {
      go(rootDirectory, root)
      fullyRefreshDirectory(root)
    }
  }

  companion object {
    private val IGNORED_FILES = setOf(".idea")
  }
}

fun fullyRefreshDirectory(directory: VirtualFile) {
  VfsUtil.markDirtyAndRefresh(false, true, true, directory)
}

private class FileTreeBuilderImpl(val directory: MutableMap<String, Entry> = mutableMapOf()) : FileTreeBuilder {
  override fun dir(name: String, block: FileTreeBuilder.() -> Unit) {
    check('/' !in name) { "Bad directory name `$name`" }
    directory[name] = FileTreeBuilderImpl().apply(block).intoDirectory()
  }

  override fun file(name: String, code: String?) {
    check('/' !in name) { "Bad file name `$name`" }
    directory[name] = Entry.File(code?.trimIndent())
  }

  fun intoDirectory() = Entry.Directory(directory)
}

sealed class Entry {
  class File(val text: String?) : Entry()
  class Directory(val children: MutableMap<String, Entry>) : Entry()
}
