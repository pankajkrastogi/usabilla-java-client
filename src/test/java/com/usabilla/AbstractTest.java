package com.usabilla;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractTest {

    protected String getJsonFromFile(String fileName) throws IOException {
        Path resourceDirectory = Paths.get("src", "test", "resources").toAbsolutePath();
        File file = new File(resourceDirectory + "/" + fileName);

        return FileUtils.readFileToString(file, Charset.defaultCharset());
    }
}
