package com.example;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ FileSystemTest.class, SuperBlockTest.class, DirectoryTest.class, InodeTest.class })
public class AllTests {
}