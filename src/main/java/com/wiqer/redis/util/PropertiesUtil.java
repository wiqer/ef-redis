package com.wiqer.redis.util;


import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PropertiesUtil {

    private PropertiesUtil() {
    }

    private static PropertiesUtil Instance = new PropertiesUtil();

    public static final PropertiesUtil getInstance() {
        return Instance;
    }

    public static Properties getProParams() {
        return PropertiesUtil.getInstance().getProParams("/redis_conf.properties");
    }

    public static String getNodeAddress() {
        String address = getProParams().getProperty("ip");
        if (StringUtil.isNullOrEmpty(address)) {
            return "127.0.0.1";
        }
        return address;
    }

    public static String getAofPath() {
        String aofDataDir = getProParams().getProperty("aof_data_dir");
        if (StringUtil.isNullOrEmpty(aofDataDir)) {
            return "./aof_data_dir/";
        }
        return aofDataDir;
    }

    /**
     * 是否开启Aof日志
     *
     * @return
     */
    public static boolean getAppendOnly() {
        String appendOnly = getProParams().getProperty("appendonly");
        if (!StringUtil.isNullOrEmpty(appendOnly)) {
            return appendOnly.trim().equals("yes");
        }
        return false;
    }

    public static boolean getTcpKeepAlive() {
        String appendOnly = getProParams().getProperty("tcp_keepalive");
        if (!StringUtil.isNullOrEmpty(appendOnly)) {
            return appendOnly.trim().equals("yes");
        }
        return false;
    }

    public static Integer getNodePort() {
        int port = 6379;
        try {
            String strPort = getProParams().getProperty("port");
            port = Integer.parseInt(strPort);
        } catch (Exception e) {
            return 6379;
        }
        if (port <= 0 || port > 60000) {
            return 6379;
        }
        return port;
    }

    public static Long getMaxMemory() {
        try {
            String strPort = getProParams().getProperty("maxmemory");
            return parseMaxMemory(strPort);
        } catch (Exception e) {
            return (long) -1;
        }
    }

    public static int getHz() {
        int hz = 10;
        try {
            String strPort = getProParams().getProperty("hz");
            hz = Integer.parseInt(strPort);
        } catch (Exception e) {
            return 6379;
        }
        if (hz <= 0 || hz > 60000) {
            return 6379;
        }
        return hz;
    }

    public static Long parseMaxMemory(String valueStr) {
        Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z]*)");
        Matcher matcher = pattern.matcher(valueStr);
        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toUpperCase();
            switch (unit) {
                case "":
                case "B":
                    return value;
                case "K":
                case "KB":
                    return value * 1024;
                case "M":
                case "MB":
                    return value * 1024 * 1024;
                case "G":
                case "GB":
                    return value * 1024 * 1024 * 1024;
                default:
                    return -1L;
            }
        }
        return -1L;
    }

    private Properties getProParams(String propertiesName) {
        InputStream is = getClass().getResourceAsStream(propertiesName);
        Properties prop = new Properties();
        try {
            prop.load(is);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }
}
