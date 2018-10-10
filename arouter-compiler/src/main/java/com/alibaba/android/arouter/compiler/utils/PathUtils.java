package com.alibaba.android.arouter.compiler.utils;

import com.squareup.javapoet.ClassName;

import static com.alibaba.android.arouter.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;

public class PathUtils {

    public static ClassName pathExchange2MethodClass(String path) throws Exception {
        return ClassName.get(PACKAGE_OF_GENERATE_FILE, pathExchange2MethodClassName(path));
    }

    public static String pathExchange2MethodClassName(String path) throws Exception {
        return "ARouter$$" + PathUtils.pathExchange(path) + "$$Method";
    }

    private static String pathExchange(String path) throws Exception {
        String[] providerPathParts;
        if (null == path || "".equals(path)) {
            throw new Exception("path is empty");
        } else {
            providerPathParts = path.split("/");
            if (providerPathParts.length != 3) {
                throw new Exception("path [" + path + " ]is not available like: /xx/xx");
            } else {
                return captureString(providerPathParts[1]) + "$$" + captureString(providerPathParts[2]);
            }
        }
    }

    private static String captureString(String str) {
        char[] cs = str.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);

    }
}
