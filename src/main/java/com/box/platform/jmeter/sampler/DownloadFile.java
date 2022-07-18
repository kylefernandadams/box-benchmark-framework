package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFile;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  JMeter sampler client which uploads files to Box.
 */
public class DownloadFile implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;
    private static final String BOX_CONFIG_PATH = "box.config.path";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FILE_ID = "current.file.id";
    private static final String THREAD_GUID = "thread.num";
    private String threadGuid = null;


    /**
     * Get default parameters for the sampler client
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(BOX_CONFIG_PATH, "${"+ BOX_CONFIG_PATH + "}");
        defaultParameters.addArgument(MAX_CACHE_ENTRIES, "${" + MAX_CACHE_ENTRIES + "}");
        defaultParameters.addArgument(USER_LOGIN, "${" + USER_LOGIN + "}");
        defaultParameters.addArgument(THREAD_GUID,"${" + THREAD_GUID + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     */
    @Override
    public void setupTest(JavaSamplerContext setupContext) {
        try {
            String boxConfigPath = setupContext.getParameter(BOX_CONFIG_PATH);
            int maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
            String userLogin = setupContext.getParameter(USER_LOGIN);

            BoxConnectionUtil util = new BoxConnectionUtil(boxConfigPath, maxCacheEntries);
            this.api = util.getAppUserConnection(userLogin);
            this.threadGuid = setupContext.getParameter(THREAD_GUID);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Get the current folder id from thread group
     * - Get the current folder based on the id
     * - Get the current file count
     * - Get the sample file path
     * - Create FileInputStream based on the file path and upload the file to Box
     * - Set a thread group parameter for the newly created file id
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{

            String currentFileId = runContext.getJMeterVariables().get(CURRENT_FILE_ID);

            BoxFile boxFile = new BoxFile(api, currentFileId);
            BoxFile.Info boxFileInfo = boxFile.getInfo();

            String downloadPath = FileServer.getFileServer().getDefaultBase() + "/" + this.threadGuid + "." + boxFileInfo.getName();
            FileOutputStream fileOutputStream = new FileOutputStream(downloadPath);
            boxFile.download(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully downloaded Box file with id: " + boxFileInfo.getID());
            result.setResponseCodeOK();
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to download Box files: " + e);

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
