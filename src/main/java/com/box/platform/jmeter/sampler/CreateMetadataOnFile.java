package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.*;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  JMeter sampler client which creates metadata on Box files.
 */
public class CreateMetadataOnFile implements JavaSamplerClient {
    private BoxDeveloperEditionAPIConnection api = null;
    private static final String BOX_CONFIG_PATH = "box.config.path";
    private static final String MAX_CACHE_ENTRIES = "max.cache.entries";
    private static final String USER_LOGIN = "user.login";
    private static final String CURRENT_FILE_ID = "current.file.id";
    private static final String CURRENT_FILE_COUNT = "current.file.count";
    private static final String METADATA_TEMPLATE_KEY = "metadata.template.key";
    private static final String METADATA_KEYS = "metadata.keys";
    private static final String METADATA_VALUES = "metadata.values";
    private static final String THREAD_GUID = "thread.num";

    private String enterpriseId = null;
    private String metadataTemplateKey = null;
    private Metadata metadata = null;
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
        defaultParameters.addArgument(CURRENT_FILE_COUNT, "${" + CURRENT_FILE_COUNT + "}");
        defaultParameters.addArgument(METADATA_TEMPLATE_KEY, "${" + METADATA_TEMPLATE_KEY + "}");
        defaultParameters.addArgument(METADATA_KEYS, "${" + METADATA_KEYS + "}");
        defaultParameters.addArgument(METADATA_VALUES, "${" + METADATA_VALUES + "}");
        defaultParameters.addArgument(CURRENT_FILE_ID, "${" + CURRENT_FILE_ID + "}");
        defaultParameters.addArgument(THREAD_GUID,"${" + THREAD_GUID + "}");

        return defaultParameters;
    }

    /**
     * Setup the sampler client.
     *  - Get a Box connection
     *  - Get metadata keys and split into a String array
     *  - Get metadata values and split into a String array
     *  - Get the metadata template based on the provided key
     */
    @Override
    public void setupTest(JavaSamplerContext setupContext) {
        try {


            metadataTemplateKey = setupContext.getParameter(METADATA_TEMPLATE_KEY);
            String[] metadataKeysArray = setupContext.getParameter(METADATA_KEYS).split(",");
            String[] metadataValuesArray = setupContext.getParameter(METADATA_VALUES).split(",");

            String boxConfigPath = setupContext.getParameter(BOX_CONFIG_PATH);
            int maxCacheEntries = setupContext.getIntParameter(MAX_CACHE_ENTRIES);
            String userLogin = setupContext.getParameter(USER_LOGIN);
            BoxConnectionUtil util = new BoxConnectionUtil(boxConfigPath, maxCacheEntries);
            this.api = util.getAppUserConnection(userLogin);
            MetadataTemplate metadataTemplate = null;
            try{
                metadataTemplate = MetadataTemplate.getMetadataTemplate(api, metadataTemplateKey);
            }
            catch (BoxAPIException bae){
                System.out.println("Failed to get metadata template with key: " + metadataTemplateKey + " with exception: {}" + bae);
            }

            if(metadataTemplate != null){
                metadata = new Metadata();
                for (int i=0; i < metadataKeysArray.length; i++){
                    String metadataKey = metadataKeysArray[i];
                    String metadataValue = metadataValuesArray[i];
                    metadata.add("/" + metadataKey, metadataValue);
                }
            }
            this.threadGuid = setupContext.getParameter(THREAD_GUID);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the sampler client.
     * - Get the current file id from the thread group
     * - Get the BoxFile based on the current file id
     * - Create metadata on the file
     */
    @Override
    public SampleResult runTest(JavaSamplerContext runContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try{
            if(metadata != null){
                String currentFileId = runContext.getJMeterVariables().get(CURRENT_FILE_ID);

                BoxFile currentFile = new BoxFile(api, currentFileId);
                Metadata fileMetadata = currentFile.createMetadata(metadataTemplateKey, Metadata.ENTERPRISE_METADATA_SCOPE, metadata);

                runContext.getJMeterVariables().put(CURRENT_FILE_ID, currentFile.getID());

                result.sampleEnd();
                result.setSuccessful(true);
                result.setResponseMessage("Successfully created metadata on Box file: " + fileMetadata.toString());
                result.setResponseCodeOK();

            } else{
                result.sampleEnd();
                result.setSuccessful(false);
                result.setResponseMessage("Unable to create metadata key values for Box file.");
                result.setResponseCode("500");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Failed to create metadata on Box files: " + e);

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