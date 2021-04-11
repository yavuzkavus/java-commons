package com.readjournal.util;

import static com.readjournal.util.FileUtil.getBaseName;
import static com.readjournal.util.FileUtil.getDirectory;
import static com.readjournal.util.FileUtil.getExtension;
import static com.readjournal.util.FileUtil.getFileName;
import static com.readjournal.util.FileUtil.normalizePath;
import static com.readjournal.util.FileUtil.relativizePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileUtilTest {
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	public void testGetFileName() {
		assertEquals("", getFileName("/"));
		assertEquals("", getFileName("////"));
		assertEquals("", getFileName("//C//.//../"));
		assertEquals("", getFileName("C//.//../"));
		assertEquals("C", getFileName("C"));
		assertEquals("C", getFileName("/C"));
		assertEquals("C", getFileName("/C/"));
		assertEquals("C", getFileName("//C//"));
		assertEquals("C", getFileName("/A/B/C/"));
		assertEquals("C", getFileName("/A/B/C"));
		assertEquals("C", getFileName("/C/./B/../"));
		assertEquals("C", getFileName("//C//./B//..///"));
		assertEquals("user", getFileName("/user/java/.."));
		assertEquals("C:", getFileName("C:"));
		assertEquals("C:", getFileName("/C:"));
		assertEquals("java", getFileName("C:\\Program Files (x86)\\java\\bin\\.."));
		assertEquals("C.ext", getFileName("/A/B/C.ext"));
		assertEquals("C.ext", getFileName("C.ext"));
	}

	@Test
	public void testGetBaseName() {
		assertEquals("java", getBaseName("C:\\Program Files (x86)\\java\\bin\\.."));
		assertEquals("C", getBaseName("/A/B/C.ext"));
		assertEquals("C", getBaseName("C.ext"));
	}

	@Test
	public void testGetExtension() {
		assertEquals("", getExtension("C"));
		assertEquals("ext", getExtension("C.ext"));
		assertEquals("ext", getExtension("A/B/C.ext"));
		assertEquals("", getExtension("A/B/C.ext/"));
		assertEquals("", getExtension("A/B/C.ext/.."));
		assertEquals("bin", getExtension("A/B/C.bin"));
		assertEquals("hidden", getExtension(".hidden"));
		assertEquals("dsstore", getExtension("/user/home/.dsstore"));
		assertEquals("", getExtension(".strange."));
		assertEquals("3", getExtension("1.2.3"));
		assertEquals("exe", getExtension("C:\\Program Files (x86)\\java\\bin\\javaw.exe"));
	}

	@Test
	public void testGetDirectory() {
		assertEquals("", getDirectory("/"));
		assertEquals("", getDirectory("////"));
		assertEquals("//C/", getDirectory("//C//D/E/.//../"));
		assertEquals("", getDirectory("C//.//../"));
		assertEquals("C/", getDirectory("C/D"));
		assertEquals("/C/", getDirectory("/C//D"));
		assertEquals("/", getDirectory("/C/"));
		assertEquals("//C/", getDirectory("//C//D//"));
		assertEquals("/A/B/C/", getDirectory("/A/B/C/D/"));
		assertEquals("/C/", getDirectory("/C/./B/D/../"));
		assertEquals("//C/", getDirectory("//C//./B//D//..///"));
		assertEquals("/user/", getDirectory("/user/etc/java/.."));
		assertEquals("C:/", getDirectory("C:/Program Files"));
		assertEquals("/C:/", getDirectory("/C:/Program Files"));
		assertEquals("C:\\Program Files (x86)\\", getDirectory("C:\\Program Files (x86)\\java\\bin\\.."));
		assertEquals("C:\\Program Files (x86)/", getDirectory("C:\\Program Files (x86)/java\\bin\\.."));
		assertEquals("C:\\Program Files (x86)/", getDirectory("C:\\Program Files (x86)////java\\/bin\\.."));
		assertEquals("/A/B/", getDirectory("/A/B/C.ext"));
		assertEquals("", getDirectory("C.ext"));
	}

	@Test
	public void testNormalizePath() {
		assertEquals("/", normalizePath("/", '/'));
		assertEquals("//", normalizePath("////", '/'));
		assertEquals("\\\\", normalizePath("////", '\\'));
		assertEquals("C/", normalizePath("./C/", '/'));
		assertEquals("../C/", normalizePath("../C/", '/'));
		assertEquals("/C/D/", normalizePath("/C//D/E/.//../", '/'));
		assertEquals("", normalizePath("C//.//../", '\\'));
		assertEquals("C\\D", normalizePath("C/D", '\\'));
		assertEquals("/C/D", normalizePath("/C//D", '/'));
		assertEquals("\\\\C\\", normalizePath("\\\\C\\D\\..", '\\'));
		assertNull(normalizePath("/C/../D//../..", '/'));
		assertEquals("/C/D/", normalizePath("/C//D//", '/'));
		assertEquals("/A/B/C/D", normalizePath("/A/B/C/D/", '/', false));
		assertEquals("/C/B/E/", normalizePath("/C/./B/D/../E\\.", '/'));
		assertEquals("//C/B", normalizePath("//C//./B//D//..///", '/', false));
		assertEquals("~", normalizePath("~", '/'));
		assertEquals("~user", normalizePath("~user", '/'));
		assertEquals("~user/home/", normalizePath("~user/home/php/./..", '/'));
		assertEquals("/user/etc/", normalizePath("/user/etc/java/..", '/'));
		assertEquals("C:\\Program Files", normalizePath("C:/Program Files", '\\'));
		assertEquals("C:\\", normalizePath("/C:/", '\\'));
		assertNull(normalizePath("C:/Program Files/../../", '/'));
		assertEquals("C:\\Program Files (x86)\\java\\", normalizePath("C:\\Program Files (x86)\\java\\bin\\..", '\\'));
		assertEquals("C:\\Program Files (x86)\\java", normalizePath("C:\\Program Files (x86)\\java\\bin\\..", '\\', false));
		assertEquals("/A/B/C.ext/", normalizePath("\\A/B/C.ext", '/', true));
		assertEquals("", normalizePath("A/..", '\\'));
	}

	@Test
	public void testRelativizePath() {
		assertEquals("test.jpeg", relativizePath("test.jpeg", "", true));
		assertEquals("../test.jpeg", relativizePath("test.jpeg", "dir/file.html", true));
		assertEquals("test.jpeg", relativizePath("dir1/dir2/test.jpeg", "dir1/dir2/file.html", true));
		assertEquals("../test.jpeg", relativizePath("dir1/test.jpeg", "dir1/dir2/file.html", true));
		assertEquals("dir2/test.jpeg", relativizePath("dir1/dir2/test.jpeg", "dir1/file.html", true));
		assertEquals("..\\..\\b\\c\\d.ext", relativizePath("C:\\a\\b\\c\\d.ext", "C:\\a\\e\\f\\g.ext", false));
	}
}
