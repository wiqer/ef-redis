package com.wiqer.redis.util;



import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class PropertiesUtil {

    private PropertiesUtil() {}
    private static PropertiesUtil Instance = new PropertiesUtil();
    public static final PropertiesUtil getInstance() {
        return Instance;
    }

    public static Properties getProParams() {
        return PropertiesUtil.getInstance().getProParams("/redis_conf.properties");
    }
    public static String getNodeAddress() {
        String address =getProParams().getProperty("ip");
        if(StringUtil.isNullOrEmpty(address)){
            return "127.0.0.1";
        }
        return  address;
    }
    public static Integer getNodePort() {
        Integer port=6379;
        try{
            String strPort =getProParams().getProperty("port");
            port=Integer.parseInt(strPort);
        }catch (Exception e) {
            return 6379;
        }
        if(port<=0||port>60000){
            return 6379;
        }
        return port;
    }
    private Properties getProParams(String propertiesName) {
        InputStream is = getClass().getResourceAsStream(propertiesName);
        Properties prop = new Properties();
        try {
            prop.load(is);
        } catch (IOException e1) {
            e1.printStackTrace();
        }finally {
            if(is != null) {
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
