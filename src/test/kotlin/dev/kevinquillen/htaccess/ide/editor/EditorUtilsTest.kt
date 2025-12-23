package dev.kevinquillen.htaccess.ide.editor

import com.intellij.mock.MockVirtualFile
import org.junit.Assert.*
import org.junit.Test

class EditorUtilsTest {

    @Test
    fun `isHtaccessFile returns true for standard htaccess filename`() {
        val file = MockVirtualFile(".htaccess")
        assertTrue(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `isHtaccessFile returns true for uppercase htaccess`() {
        val file = MockVirtualFile(".HTACCESS")
        assertTrue(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `isHtaccessFile returns true for mixed case htaccess`() {
        val file = MockVirtualFile(".HtAccess")
        assertTrue(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `isHtaccessFile returns true for files ending with htaccess`() {
        val file = MockVirtualFile("test.htaccess")
        assertTrue(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `isHtaccessFile returns false for non-htaccess files`() {
        val file = MockVirtualFile("config.php")
        assertFalse(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `isHtaccessFile returns false for htaccess-like but not htaccess`() {
        val file = MockVirtualFile("htaccess.txt")
        assertFalse(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `isHtaccessFile returns false for null file`() {
        assertFalse(EditorUtils.isHtaccessFile(null))
    }

    @Test
    fun `isHtaccessFile returns false for files with htaccess in the middle`() {
        val file = MockVirtualFile("my-htaccess-backup.txt")
        assertFalse(EditorUtils.isHtaccessFile(file))
    }

    @Test
    fun `HtaccessEditorContent stores all properties`() {
        val content = HtaccessEditorContent(
            content = "RewriteRule ^test$ /result [L]",
            filePath = "/var/www/html/.htaccess",
            fileName = ".htaccess"
        )

        assertEquals("RewriteRule ^test$ /result [L]", content.content)
        assertEquals("/var/www/html/.htaccess", content.filePath)
        assertEquals(".htaccess", content.fileName)
    }

    @Test
    fun `HtaccessEditorContent equality works correctly`() {
        val content1 = HtaccessEditorContent(
            content = "RewriteEngine On",
            filePath = "/path/.htaccess",
            fileName = ".htaccess"
        )
        val content2 = HtaccessEditorContent(
            content = "RewriteEngine On",
            filePath = "/path/.htaccess",
            fileName = ".htaccess"
        )

        assertEquals(content1, content2)
    }
}
