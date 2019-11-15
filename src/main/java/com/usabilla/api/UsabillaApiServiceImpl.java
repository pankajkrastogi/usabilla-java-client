package com.usabilla.api;

import com.usabilla.api.auth.UsabillaAuthBuilder;
import com.usabilla.api.client.UsabillaClient;
import com.usabilla.api.client.model.RequestCommand;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.usabilla.api.utils.CommonUtils.BASE_URL;

public class UsabillaApiServiceImpl implements UsabillaApiService {

    private static final Logger LOGGER = LogManager.getLogger(UsabillaApiServiceImpl.class);

    private static final String ACCEPT = "Accept";

    private int TimeoutInMs = 10000;

    private final UsabillaAuthBuilder usabillaAuthBuilder;

    private final UsabillaClient usabillaClient;

    public UsabillaApiServiceImpl(final UsabillaAuthBuilder usabillaAuthBuilder, final UsabillaClient usabillaClient) {
        this.usabillaAuthBuilder = usabillaAuthBuilder;
        this.usabillaClient = usabillaClient;
    }

    public String getAllFeedbackButtons(final RequestCommand requestCommand) throws Exception {
        final HttpGet httpGet = new HttpGet(buildUri(requestCommand.getUri()));

        try {
            httpGet.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            httpGet.setHeader("Authorization", requestCommand.getAuthorizationHeader());
            httpGet.setHeader("x-usbl-date", requestCommand.getxUsblDateHeader());

            final CloseableHttpResponse response = usabillaClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.debug("Usabilla response status:" + statusCode);

            final HttpEntity entity = response.getEntity();
            if (Objects.isNull(entity)) {
                return null;
            }
            final String responseBody = EntityUtils.toString(entity);
            LOGGER.debug("responseBody:" + responseBody);
            return responseBody;
        } catch (Exception e) {
            throw new InternalError(String.format("Failed to get all feedback buttons due to exception:%s", e));
        }
    }

    private URI buildUri(final String uri) throws URISyntaxException {
        return new URIBuilder(String.join("/", BASE_URL, uri))
                .build();
    }

    @Override
    public String getFeedbackOnButton(final RequestCommand requestCommand) throws Exception {

        final HttpGet httpGet = new HttpGet(buildUri(requestCommand.getUri()));

        // Terminate the request after given timeout value
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                httpGet.abort();
            }
        };
        new Timer(true).schedule(task, (TimeoutInMs) + 100);

        try {
            httpGet.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            httpGet.setHeader("Authorization", requestCommand.getAuthorizationHeader());
            httpGet.setHeader("x-usbl-date", requestCommand.getxUsblDateHeader());

            final CloseableHttpResponse response = usabillaClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.debug("Usabilla get feedback response status:" + statusCode);

            final HttpEntity entity = response.getEntity();
            if (Objects.isNull(entity)) {
                return null;
            }
            final String responseBody = EntityUtils.toString(entity);
            LOGGER.debug("responseBody:" + responseBody);
            return responseBody;
        } catch (Exception e) {
            throw new InternalError(String.format("Failed to get feedback due to exception:%s", e));
        }
    }

    //    private FeedbackCommand getFetchFeedbackCommand() throws Exception {
//
//        Date currDate = new Date();
//        String uri = "button/uri";
//        Integer limit = 100;
//        Long since = 12122445555L;
//
//        final String method = HttpMethod.GET.name();
//        final String shortDate = getShortDateString(currDate);
//        final String longDate = getLongDateString(currDate);
//        final String credentialScope = getCredentialScope(shortDate);
//
//        final String queryParameters = String.format("limit=%s&since=%s", limit, since);
//        final String finalUri = getUri(uri, queryParameters);
//
//        //uri = URLEncoder.encode(uri, Charset.defaultCharset());
//       LOGGER.debug(String.format("host:%s\nmethod:%s\nalgorithm:%s\nshortDate:%s\nlongDate:%s\ncredentialScope:%s\nqueryParameters:%s\nuri:%s\nfinalUri:%s\n",
//                usabillaHost, method, HMAC_SHA_256, shortDate, longDate, credentialScope, queryParameters, uri, finalUri));
//
//        String canonicalHeaders = getCanonicalHeaders(usabillaHost, longDate);
//       LOGGER.debug(String.format("\ncanonicalHeaders:\n%s", canonicalHeaders));
//
//       LOGGER.debug(String.format("\nsignedHeaders:\n%s", signedHeaders));
//
//        String payload = StringUtils.EMPTY;
//        String requestPayload = DigestUtils.sha256Hex(payload);
//       LOGGER.debug(String.format("\nrequestPayload:\n%s", requestPayload));
//
//        //Create Canonical String
//        String canonicalString = String.format("%s\n%s\n%s\n%s\n%s\n%s", method, uri, queryParameters, canonicalHeaders, signedHeaders, requestPayload);
//       LOGGER.debug(String.format("\ncanonicalString:\n%s", canonicalString));
//
//        String hashedCanonicalString = DigestUtils.sha256Hex(canonicalString);
//       LOGGER.debug(String.format("\nhashedCanonicalString:\n%s", hashedCanonicalString));
//
//        //Create string to Sign
//        String stringToSign = String.format("%s\n%s\n%s\n%s", HMAC_SHA_256, longDate, credentialScope, hashedCanonicalString);
//       LOGGER.debug(String.format("\nstringToSign:\n%s", stringToSign));
//
//        //Create signature
//        String secretKey = String.format("USBL1%s", secret);
//       LOGGER.debug("\nsecretKey:\n" + secretKey);
//       LOGGER.debug("\ndata:\n" + shortDate);
//
//        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
//        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
//        sha256_HMAC.init(secret_key);
//        byte[] kDateBytes = sha256_HMAC.doFinal(shortDate.getBytes());
//        String kDate = toHexString(kDateBytes);
//       LOGGER.debug(String.format("\nkDate:\n%s", kDate));
//
//        secret_key = new SecretKeySpec(kDateBytes, "HmacSHA256");
//        sha256_HMAC.init(secret_key);
//        byte[] signingKeyBytes = sha256_HMAC.doFinal("usbl1_request".getBytes());
//        String kSigning = toHexString(signingKeyBytes);
//       LOGGER.debug(String.format("\nkSigning:\n%s", kSigning));
//
//        secret_key = new SecretKeySpec(signingKeyBytes, "HmacSHA256");
//        sha256_HMAC.init(secret_key);
//        byte[] signatureBytes = sha256_HMAC.doFinal(stringToSign.getBytes());
//        String signature = toHexString(signatureBytes);
//       LOGGER.debug(String.format("\nsignature:\n%s", signature));
//
//        String authorizationHeader = String.join(", ",
//                String.join(StringUtils.EMPTY, HMAC_SHA_256, " Credential=", accessKey, "/", shortDate, "/usbl1_request"),
//                String.join(StringUtils.EMPTY, "SignedHeaders=", signedHeaders),
//                String.join(StringUtils.EMPTY, "Signature=", signature));
//       LOGGER.debug(String.format("\nauthorizationHeader:\n%s", authorizationHeader));
//
//        FeedbackCommand feedbackCommand = new FeedbackCommand(authorizationHeader, longDate, finalUri);
//        return feedbackCommand;
//    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    private static String getDateString(Date date, DateTimeFormatter dateTimeFormatter) {
        LocalDateTime localDateTime = Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return localDateTime.format(dateTimeFormatter);
    }
}
