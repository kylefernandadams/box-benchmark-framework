package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 *  JMeter sampler client which uploads files to Box.
 */
public class UploadFile implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;
    private static final String BOX_CONFIG_PATH = "box.config.path";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FILE_COUNT = "current.file.count";
    private static final String CURRENT_FILE_ID = "current.file.id";
    private static final String CURRENT_FOLDER_ID = "current.folder.id";
    private static final String SAMPLE_FILE_PATH = "sample.file.path";
    private static final String TEST_FILE_NAME = ".TestFile.txt";

    private static final String THREAD_GUID = "thread.num";
    private String threadGuid = null;
    private String fileGuid = null;


    /**
     * Get default parameters for the sampler client
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(BOX_CONFIG_PATH, "${"+ BOX_CONFIG_PATH + "}");
        defaultParameters.addArgument(MAX_CACHE_ENTRIES, "${" + MAX_CACHE_ENTRIES + "}");
        defaultParameters.addArgument(USER_LOGIN, "${" + USER_LOGIN + "}");
        defaultParameters.addArgument(CURRENT_FILE_COUNT,"${" + CURRENT_FILE_COUNT + "}");
        defaultParameters.addArgument(SAMPLE_FILE_PATH, "${" + SAMPLE_FILE_PATH + "}");
        defaultParameters.addArgument(CURRENT_FILE_ID, "${" + CURRENT_FILE_ID + "}");
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
            this.fileGuid = String.valueOf(UUID.randomUUID());

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
            String currentFolderId = runContext.getJMeterVariables().get(CURRENT_FOLDER_ID);

            BoxFolder folder = new BoxFolder(api, currentFolderId);
            String currentFileCount = runContext.getParameter(CURRENT_FILE_COUNT);
            String sampleFilePath = runContext.getParameter(SAMPLE_FILE_PATH);
            FileInputStream stream = new FileInputStream(sampleFilePath);
            String filename = UUID.randomUUID() + "." + currentFileCount + TEST_FILE_NAME;
            BoxFile.Info fileInfo = folder.uploadFile(stream, filename);
            stream.close();

            runContext.getJMeterVariables().put(CURRENT_FILE_ID, fileInfo.getID());

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully created Box file with id: " + fileInfo.getID());
            result.setResponseCodeOK();
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create Box files: " + e);

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
