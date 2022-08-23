package com.box.platform.jmeter.sampler;

import com.box.platform.jmeter.util.BoxConnectionUtil;
import com.box.sdk.*;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.*;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.http.HttpClientTransportOverHTTP3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.box.sdk.http.ContentType.APPLICATION_JSON;

public class ScratchPad {
    protected static final String BOUNDARY = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

//    private static Logger logger = LoggerFactory.getLogger(ScratchPad.class);

    public static final String BOX_CONFIG_PATH = "C:/Developer/Code/box_config.json";
    public static final String BASE_FOLDER_ID = "169325690980";
    public static final String BASE_FOLDER_NAME = "JMeterTest-HTTP3-";
    private static final String TEST_FOLDER_NAME = "-TestFolder";

    private static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmmss";
    public static final String USER_LOGIN = "kadams@boxdemo.com";
    public static final int MAX_CACHE_ENTRIES = 100;

    private static final String MULTI_PART_SEPARATOR = "--";
    private static final String LINE_SEPARATOR = "\r\n";

    private static final String SAMPLE_FILE_PATH = "C:/Users/kylea/Downloads/MO101-_1981290.pdf";


    private final HttpClient httpClient;

    private BoxDeveloperEditionAPIConnection api = null;

    private String jmeterBaseFolderId = null;

    private String accessToken = null;

    private String newFolderId = null;
    private String newFileId = null;

    private OutputStream outputStream;
    private boolean firstBoundary = true;




    public ScratchPad() {
        HTTP3Client h3Client = new HTTP3Client();
        h3Client.getQuicConfiguration().setSessionRecvWindow(64 * 1024 * 1024);
        h3Client.getQuicConfiguration().setVerifyPeerCertificates(false);
        h3Client.getQuicConfiguration().setBidirectionalStreamRecvWindow(64 * 1024 * 1024);
        h3Client.getQuicConfiguration().setMaxBidirectionalRemoteStreams(64 * 1024 * 1024);
        h3Client.getQuicConfiguration().setMaxUnidirectionalRemoteStreams(64 * 1024 * 1024);
        h3Client.getHTTP3Configuration().setMaxRequestHeadersSize(64 * 1024 * 1024);
        HttpClientTransportOverHTTP3 transport = new HttpClientTransportOverHTTP3(h3Client);


        this.httpClient = new HttpClient(transport);
    }


    public static void main(String[] args) {
        ScratchPad test = new ScratchPad();
//        test.createFolder();
//        test.uploadFile();

        test.createSignRequest();
    }

    private void createSignRequest() {
        BoxConnectionUtil util = new BoxConnectionUtil(BOX_CONFIG_PATH, MAX_CACHE_ENTRIES);
        this.api = util.getAppUserConnection(USER_LOGIN);
        this.accessToken = this.api.getAccessToken();

        List<BoxSignRequestFile> files = new ArrayList<BoxSignRequestFile>();
        BoxSignRequestFile file = new BoxSignRequestFile("925199127832");
        files.add(file);


        List<BoxSignRequestSigner> signers = new ArrayList<BoxSignRequestSigner>();
        BoxSignRequestSigner newSigner = new BoxSignRequestSigner("kyle.adams@massnerder.io");
        signers.add(newSigner);

        String destinationParentFolderId = "170027550081";

        BoxSignRequestCreateParams createParams = new BoxSignRequestCreateParams()
                .setIsDocumentPreparationNeeded(true);

        BoxSignRequest.Info signRequestInfo = BoxSignRequest.createSignRequest(api, files,
                signers, destinationParentFolderId, createParams);

        System.out.println("Sign Request Info: " + signRequestInfo.getJson());
    }

    private void createFolder() {
        try {
            BoxConnectionUtil util = new BoxConnectionUtil(BOX_CONFIG_PATH, MAX_CACHE_ENTRIES);
            this.api = util.getAppUserConnection(USER_LOGIN);
            this.accessToken = this.api.getAccessToken();
            BoxFolder baseFolder = new BoxFolder(api, BASE_FOLDER_ID);
            System.out.println("Found JMeter Base Folder: " + baseFolder.getID());

            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            String now = localDateTime.format(dateTimeFormatter);

            BoxFolder.Info rootFolderInfo = baseFolder.createFolder(BASE_FOLDER_NAME + UUID.randomUUID() + "-" + now);
            System.out.println("Found JMeter Test Folder: " + rootFolderInfo.getID());
            jmeterBaseFolderId = rootFolderInfo.getID();

            if (!httpClient.isStarted()) {
                httpClient.start();
            }

            URI uri = URI.create("https://api.box.com/2.0/folders");
            JsonObject parent = new JsonObject();
            parent.add("id", jmeterBaseFolderId);

            JsonObject newFolder = new JsonObject();
            newFolder.add("name", UUID.randomUUID() + TEST_FOLDER_NAME);
            newFolder.add("parent", parent);

            Request request = httpClient.newRequest("api.box.com", 443)
                    .scheme("https")
                    .path("/2.0/folders")
                    .agent("Jetty HTTP client")
                    .version(HttpVersion.HTTP_3)
                    .method(HttpMethod.POST)
                    .body(new StringRequestContent(APPLICATION_JSON, newFolder.toString()))
                    .headers(headers -> headers
                            .put("Authorization", "Bearer " + this.accessToken));

            System.out.println("Found URI: " + request.getURI().toString());

            ContentResponse contentResponse = request.send();

            String contentType = contentResponse.getHeaders() != null
                    ? contentResponse.getHeaders().get("Content-Type")
                    : null;
            System.out.println("Found content type: " + contentType);

            InputStream inputStream = new ByteArrayInputStream(contentResponse.getContent());
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject folderJson = Json.parse(text).asObject();
            System.out.println("Found response string: " + text);
            this.newFolderId = folderJson.get("id").asString();
            System.out.println("Found Folder Id: " + this.newFolderId);
            System.out.println("Found status: " + String.valueOf(contentResponse.getStatus()));

            String responseMessage = contentResponse.getReason() != null ? contentResponse.getReason()
                    : HttpStatus.getMessage(contentResponse.getStatus());
            System.out.println("Found response message: " + responseMessage);

            if(httpClient.isStarted()){
                httpClient.stop();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadFile() {
        try{
            if (!httpClient.isStarted()) {
                httpClient.start();
            }

            Request request = httpClient.newRequest("upload.box.com", 443)
                    .scheme("https")
                    .path("/api/2.0/files/content")
                    .agent("Jetty HTTP client")
                    .version(HttpVersion.HTTP_3)
                    .method(HttpMethod.POST)
                    .headers(headers -> headers
                            .put("Authorization", "Bearer " + this.accessToken)
                            .put("Content-Type", "multipart/form-data; boundary=" + BOUNDARY));

            Path filePath = Path.of(SAMPLE_FILE_PATH);
            String fileName = filePath.getFileName().toString();


//            StringBuilder postBody = new StringBuilder();

            MultiPartRequestContent multipartEntityBuilder = new MultiPartRequestContent();
//            Charset contentCharset = StandardCharsets.UTF_8;

            JsonObject parent = new JsonObject();
            parent.add("id", this.newFolderId);

            JsonObject fileJson = new JsonObject();
            fileJson.add("name", UUID.randomUUID() + "-Test.pdf");
            fileJson.add("parent", parent);

            multipartEntityBuilder.addFieldPart("attributes", new StringRequestContent(fileJson.toString()), null);
            multipartEntityBuilder.addFilePart("file", fileName, new PathRequestContent(filePath), null);
            multipartEntityBuilder.close();
            request.body(multipartEntityBuilder);

//            this.writePartHeader(new String[][]{{"name", "attributes"}});
//            this.writeOutput(fileJson.toString());
//
//            this.writePartHeader(new String[][]{{"name", "file"}, {"filename", fileName}}, "application/octet-stream");
//
//            this.writeBoundary();
//            this.writeOutput("--");

//            String disposition = "name=\"" + URLEncoder.encode("attributes", StandardCharsets.UTF_8) + "\"";
//            String contentType =  "application/pdf; charset=" + StandardCharsets.UTF_8;
//            String encoding = "8bit";
//            postBody.append(
//                    buildPartBody(boundary, disposition, contentType, encoding,
//                            URLEncoder.encode(contentCharset.name(), StandardCharsets.UTF_8)));
//            multipartEntityBuilder.addFieldPart("attributes",
//                    new StringRequestContent("application/json", fileJson.toString(), contentCharset), null);

//           Request.Content[] fileBodies = new PathRequestContent[1];
            // Cannot retrieve parts once added

//            String mimeTypeFile = "application/pdf";
//            fileBodies[0] = new PathRequestContent(mimeTypeFile, filePath);
//            String fileName = Paths.get(SAMPLE_FILE_PATH).getFileName().toString();
//            postBody.append(buildFilePartRequestBody("file", fileName, "application/pdf", boundary));
//            multipartEntityBuilder.add;
//            multipartEntityBuilder.addFilePart("file", fileName, fileBodies[0],
//                    null);
//            multipartEntityBuilder.close();
//            Request.Content  content = new InputStreamRequestContent();

//            System.out.println("Found URI: " + request.getURI().toString());

            ContentResponse contentResponse = request.send();

            InputStream inputStream = new ByteArrayInputStream(contentResponse.getContent());
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject newFileJson = Json.parse(text).asObject();
            System.out.println("Found response string: " + text);
            this.newFileId = newFileJson.get("id").asString();
            System.out.println("Found Folder Id: " + this.newFileId);
            System.out.println("Found status: " + String.valueOf(contentResponse.getStatus()));

            String responseMessage = contentResponse.getReason() != null ? contentResponse.getReason()
                    : HttpStatus.getMessage(contentResponse.getStatus());
            System.out.println("Found response message: " + responseMessage);

            if(httpClient.isStarted()){
                httpClient.stop();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


//    private String extractMultipartBoundary(MultiPartRequestContent multipartEntityBuilder) {
//        String contentType = multipartEntityBuilder.getContentType();
//        String boundaryParam = contentType.substring(contentType.indexOf(" ") + 1);
//        return boundaryParam.substring(boundaryParam.indexOf("=") + 1);
//    }
//
//    private String buildPartBody(String boundary, String disposition, String contentType,
//                                 String encoding, String value) {
//        return MULTI_PART_SEPARATOR + boundary + LINE_SEPARATOR +
//                HttpFields.build()
//                        .add("Content-Disposition", "form-data; " + disposition)
//                        .add(HttpHeader.CONTENT_TYPE.toString(), contentType)
//                        .add("Content-Transfer-Encoding", encoding)
//                        .toString()
//                + value + LINE_SEPARATOR;
//    }
//
//    private String buildFilePartRequestBody(String fileArgName, String fileName, String fileMimeType, String boundary) {
//        String disposition = "name=\"" + fileArgName + "\"; filename=\"" + fileName + "\"";
//        return buildPartBody(boundary, disposition, fileMimeType, "binary",
//                "<actual file content, not shown here>");
//    }

    private void writeBoundary() throws IOException {
        if (!this.firstBoundary) {
            this.writeOutput("\r\n");
        }

        this.firstBoundary = false;
        this.writeOutput("--");
        this.writeOutput(BOUNDARY);
    }

    private void writePartHeader(String[][] formData) throws IOException {
        this.writePartHeader(formData, null);
    }

    private void writePartHeader(String[][] formData, String contentType) throws IOException {
        this.writeBoundary();
        this.writeOutput("\r\n");
        this.writeOutput("Content-Disposition: form-data");
        for (String[] part : formData) {
            this.writeOutput("; ");
            this.writeOutput(part[0]);
            this.writeOutput("=\"");
            this.writeOutput(URLEncoder.encode(part[1], "UTF-8"));
            this.writeOutput("\"");
        }

        if (contentType != null) {
            this.writeOutput("\r\nContent-Type: ");
            this.writeOutput(contentType);
        }

        this.writeOutput("\r\n\r\n");
    }

    private void writeOutput(String s) throws IOException {
        this.outputStream.write(s.getBytes(StandardCharsets.UTF_8));
    }
}
