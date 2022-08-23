package com.box.platform.jmeter.util;

import com.box.sdk.*;

import java.io.FileReader;

/**
 * Utility class to get a Box connection
 */
public class BoxConnectionUtil {

    private BoxDeveloperEditionAPIConnection api = null;

    private BoxConfig boxConfig = null;
    private String boxConfigPath = null;
    private int maxCacheEntries;

    public BoxConnectionUtil(String boxConfigPath, int maxCacheEntries){
        this.boxConfigPath = boxConfigPath;
        this.maxCacheEntries = maxCacheEntries;
    }

    public BoxConnectionUtil(BoxConfig boxConfig, int maxCacheEntries){
        this.boxConfig = boxConfig;
        this.maxCacheEntries = maxCacheEntries;
    }

    /**
     * Get an app user connection
     */
    public BoxDeveloperEditionAPIConnection getAppUserConnection(String userLogin){
        try{
            if(boxConfig == null) {
                boxConfig = BoxConfig.readFrom(new FileReader(boxConfigPath));
            }
            IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(maxCacheEntries);

            // Instantiate Box Connection
            this.api = BoxDeveloperEditionAPIConnection.getUserConnection(userLogin, boxConfig, accessTokenCache);
//            this.api.setCustomHeader("Accept-Encoding", "gzip, deflate");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return api;
    }

    /**
     * Get an enterprise connection
     */
    public BoxDeveloperEditionAPIConnection getEnterpriseConnection(){
        try{
            if(boxConfig == null) {
                boxConfig = BoxConfig.readFrom(new FileReader(boxConfigPath));
            }
            IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(maxCacheEntries);

            // Instantiate Box Connection
            api = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(boxConfig, accessTokenCache);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return api;
    }
}
