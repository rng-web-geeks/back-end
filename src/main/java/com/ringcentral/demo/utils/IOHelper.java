package com.ringcentral.demo.utils;

import java.io.IOException;
import java.io.InputStream;

public final class IOHelper {

    public static InputStream loadFromResource(String location) throws IOException {
        ClassLoader loader = IOHelper.class.getClassLoader();
        loader = (loader == null) ? Thread.currentThread().getContextClassLoader() : loader;

        return loader.getResourceAsStream(location);
    }
}
