package com.box.platform.jmeter.sampler;

import com.box.sdk.*;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;

import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * JMeter sampler client which creates a Box connection.
 */
public class GetBoxConnection implements JavaSamplerClient{
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String BOX_ACCESS_TOKEN = "box.access.token";
    private static final String BOX_CONFIG_PATH = "box.config.path";
    private String boxConfigPath = null;
    private int maxCacheEntries;
    private String userLogin = null;

    /**
     * Get default parameters for the sampler client
     */
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(BOX_CONFIG_PATH, "${" + BOX_CONFIG_PATH + "}");
        defaultParameters.addArgument(MAX_CACHE_ENTRIES, "${" + MAX_CACHE_ENTRIES + "}");
        defaultParameters.addArgument(USER_LOGIN, "${" + USER_LOGIN + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client and the necessary parameters to create a Box connection.
     */
    public void setupTest(JavaSamplerContext setupContext) {
        try {
            boxConfigPath = setupContext.getParameter(BOX_CONFIG_PATH);
            maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
            userLogin = setupContext.getParameter(USER_LOGIN);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Create the JWT encryption preferences
     * - Create a Box connection.
     */
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();

        try{
            IAccessTokenCache accessTokenCache = new InMemoryLRUAccessTokenCache(maxCacheEntries);

            // Instantiate Box Connection
            BoxConfig boxConfig = BoxConfig.readFrom(new FileReader(boxConfigPath));

            BoxDeveloperEditionAPIConnection api = BoxDeveloperEditionAPIConnection.getUserConnection(this.userLogin, boxConfig, accessTokenCache);
//            api.setCustomHeader("Accept-Encoding", "gzip, deflate");

            String accessToken = api.getAccessToken();

            JMeterUtils.getJMeterProperties().setProperty(BOX_ACCESS_TOKEN, accessToken);

            if(api != null){
                result.sampleEnd();
                result.setSuccessful(true);
                result.setResponseMessage("Successfully created Box platform connection with state: " + api.save());
                result.setResponseCodeOK();
            } else{
                result.setSuccessful(false);
                result.setResponseMessage("Failed to get a valid Box connection state.");

                StringWriter stringWriter = new StringWriter();
                stringWriter.append("Failed to get valid Box connection state");
                result.setResponseData(stringWriter.toString().getBytes());
                result.setDataType(SampleResult.TEXT);
                result.setResponseCode("500");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create Box platform connection: " + e);

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace( new PrintWriter(stringWriter));
            result.setResponseData(stringWriter.toString().getBytes());
            result.setDataType(SampleResult.TEXT);
            result.setResponseCode("500");
        }
        return result;
    }

    /**
     * Tear down the sampler client
     */
    public void teardownTest(JavaSamplerContext teardownContext) {
    }
}
