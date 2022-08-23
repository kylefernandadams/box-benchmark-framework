package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFolder;
import com.eclipsesource.json.JsonObject;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.Logger.Level.INFO;

/**
 *  JMeter sampler client which creates Box folders.
 */
public class CreateFolderHttp3 implements JavaSamplerClient {
    HTTP3Client http3Client = null;

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

           http3Client = new HTTP3Client();

            // Configure HTTP3Client, for example:
            http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);

            // Start HTTP3Client.
            http3Client.start();


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



            SocketAddress serverAddress = new InetSocketAddress("api.box.com", 443);
            CompletableFuture<Session.Client> sessionCF = http3Client.connect(serverAddress, new Session.Client.Listener() {});
            Session.Client session = sessionCF.get();

            // Configure the request headers.
            HttpFields requestHeaders = HttpFields.build()
                    .put(HttpHeader.CONTENT_TYPE, "application/json")
                    .put(HttpHeader.AUTHORIZATION, "Bearer " + this.api.getAccessToken());


            // The request metadata with method, URI and headers.
            MetaData.Request request = new MetaData.Request("POST", HttpURI.from("https://api.box.com/2.0/folders"), HttpVersion.HTTP_3, requestHeaders);

            // The HTTP/3 HEADERS frame, with endStream=false to
            // signal that there will be more frames in this stream.
            HeadersFrame headersFrame = new HeadersFrame(request, false);

            // Open a Stream by sending the HEADERS frame.
            CompletableFuture<Stream> streamCF = session.newRequest(headersFrame, new Stream.Client.Listener() {

                @Override
                public void onResponse(Stream.Client stream, HeadersFrame frame)
                {
                    MetaData metaData = frame.getMetaData();
                    MetaData.Response response = (MetaData.Response)metaData;
                    System.getLogger("http3").log(INFO, "Received response {0}", response);
                }

                @Override
                public void onDataAvailable(Stream.Client stream)
                {
                    // Read a chunk of the content.
                    Stream.Data data = stream.readData();
                    if (data == null)
                    {
                        // No data available now, demand to be called back.
                        stream.demand();
                    }
                    else
                    {
                        // Process the content.
                        String jsonString = StandardCharsets.UTF_8.decode(data.getByteBuffer()).toString();
                        System.out.println("Found response: " + jsonString);

                        // Notify the implementation that the content has been consumed.
                        data.complete();

                        if (!data.isLast())
                        {
                            // Demand to be called back.
                            stream.demand();
                        }
                    }
                }
            });
            JsonObject parent = new JsonObject();
            parent.add("id", jmeterBaseFolderId);

            JsonObject newFolder = new JsonObject();
            newFolder.add("name", UUID.randomUUID() + TEST_FOLDER_NAME);
            newFolder.add("parent", parent);
            ByteBuffer jsonRequestBuffer = StandardCharsets.UTF_8.encode(newFolder.toString());


            // Block to obtain the Stream.
            // Alternatively you can use the CompletableFuture APIs to avoid blocking.
            streamCF.thenCompose(stream -> stream.data(new DataFrame(jsonRequestBuffer, true)));
            Stream stream = streamCF.get();


            // The request content, in two chunks.

//            String content1 = "{\"greet\": \"hello world\"}";
//            ByteBuffer buffer1 = StandardCharsets.UTF_8.encode(content1);
//            String content2 = "{\"user\": \"jetty\"}";
//            ByteBuffer buffer2 = StandardCharsets.UTF_8.encode(content2);

            // Send the first DATA frame on the stream, with endStream=false
            // to signal that there are more frames in this stream.

            // Only when the first chunk has been sent we can send the second,
            // with endStream=true to signal that there are no more frames.
//            CompletableFuture<Stream> dataCF1 = stream.data(new DataFrame(jsonRequestBuffer, true));
//            String dataCF1.get()
//            dataCF1.thenCompose(s -> s.data(new DataFrame(buffer2, true)));

//            runContext.getJMeterVariables().put(CURRENT_FOLDER_ID, currentFolderId);

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
        // Stop HTTP3Client.
        try {
            http3Client.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
