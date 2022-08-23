package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFolder;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 *  JMeter sampler client which creates Box folders.
 */
public class CreateFolder implements JavaSamplerClient {

    private BoxDeveloperEditionAPIConnection api = null;

    private static final String BOX_CONFIG_PATH = "box.config.path";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FOLDER_ID = "current.folder.id";
    private static final String BASE_FOLDER_ID = "base.folder.id";
    private static final String BASE_FOLDER_NAME = "JMeterTest-SDK-";
    private static final String TEST_FOLDER_NAME = "-TestFolder";
    private static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmmss";
    private String jmeterBaseFolderId = null;


    /**
     * Get default parameters for the sampler client
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(BOX_CONFIG_PATH, "${"+ BOX_CONFIG_PATH + "}");
        defaultParameters.addArgument(MAX_CACHE_ENTRIES, "${" + MAX_CACHE_ENTRIES + "}");
        defaultParameters.addArgument(USER_LOGIN, "${" + USER_LOGIN + "}");
        defaultParameters.addArgument(BASE_FOLDER_ID,"${" + BASE_FOLDER_ID + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     *  - Create the base JMeterTest folder and append the current date/time
     *  - Set the base JMeter folder id
     */
    @Override
    public void setupTest(JavaSamplerContext setupContext) {
        try {
            String boxConfigPath = setupContext.getParameter(BOX_CONFIG_PATH);
            int maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
            String userLogin = setupContext.getParameter(USER_LOGIN);
            BoxConnectionUtil util = new BoxConnectionUtil(boxConfigPath, maxCacheEntries);
            this.api = util.getAppUserConnection(userLogin);
            String baseFolderId = setupContext.getParameter(BASE_FOLDER_ID);
            BoxFolder baseFolder = new BoxFolder(api, baseFolderId);
            System.out.println("Found JMeter Base Folder: " + baseFolder.getID());


            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            String now = localDateTime.format(dateTimeFormatter);

            BoxFolder.Info rootFolderInfo = baseFolder.createFolder(BASE_FOLDER_NAME + UUID.randomUUID() + "-" + now);
            System.out.println("Found JMeter Test Folder: " + rootFolderInfo.getID());
            jmeterBaseFolderId = rootFolderInfo.getID();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Create the test folder
     * - Set add the newly created folder id to a thread group property
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{
            BoxFolder parentFolder = new BoxFolder(api, jmeterBaseFolderId);

            BoxFolder.Info folderInfo = parentFolder.createFolder(UUID.randomUUID() + TEST_FOLDER_NAME);
            String currentFolderId = folderInfo.getID();

            runContext.getJMeterVariables().put(CURRENT_FOLDER_ID, currentFolderId);

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully created Box folders.");
            result.setResponseCodeOK();
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create Box folders: " + e);

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
    @Override
    public void teardownTest(JavaSamplerContext teardownContext) {
    }
}
