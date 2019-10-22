package com.usabilla.api;

import com.usabilla.api.auth.UsabillaAuthBuilder;
import com.usabilla.api.client.model.RequestCommand;
import com.usabilla.api.utils.CommonUtils;
import com.usabilla.api.utils.HttpMethod;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.usabilla.api.utils.CommonUtils.BASE_URL;

public class UsabillaApiServiceImpl implements UsabillaApiService {

    private static final String TIMEOUT = "timeout";
    private static final String ACCEPT = "Accept";

    private int TimeoutInMs = 10000;
    private int KeepAliveInSec = 60;
    private int MaxThreads = 10;

    private String baseUrl = BASE_URL;

    private final UsabillaAuthBuilder usabillaAuthBuilder;

    public UsabillaApiServiceImpl(final UsabillaAuthBuilder usabillaAuthBuilder) {
        this.usabillaAuthBuilder = usabillaAuthBuilder;
    }

    public UsabillaApiServiceImpl(final String baseUrl, final UsabillaAuthBuilder usabillaAuthBuilder) {
        this.baseUrl = baseUrl;
        this.usabillaAuthBuilder = usabillaAuthBuilder;
    }

    public String getAllFeedbackButtons(final int limit, final long since) throws Exception {

        //https://data.usabilla.com/live/websites/button

        final Date currDate = new Date(since);

        final String method = HttpMethod.GET.name();
        final String requestUri = CommonUtils.BUTTONS_URI;
        final String queryString = String.format("limit=%s&since=%s", limit, since);

        final RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);
        System.out.println("requestCommand:" + requestCommand);

        final CloseableHttpClient closeableHttpClient = buildHttpClient(baseUrl);

        final HttpGet httpGet = new HttpGet(buildFeedbackUrl(requestCommand.getUri()));

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

            final CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Usabilla response status:" + statusCode);

            final HttpEntity entity = response.getEntity();
            if (Objects.isNull(entity)) {
                return null;
            }
            final String responseBody = EntityUtils.toString(entity);
            System.out.println("responseBody:" + responseBody);
            return responseBody;
        } catch (Exception e) {
            throw new InternalError(String.format("Failed to get feedback due to exception:%s", e));
        }
    }

    private URI buildFeedbackUrl(final String uri) throws URISyntaxException {
        return new URIBuilder(String.join("/", baseUrl, uri))
                .build();
    }


    private CloseableHttpClient buildHttpClient(final String endpoint) {
        final ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase(TIMEOUT)) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return KeepAliveInSec * 1000;
        };

        final HttpRoute route = new HttpRoute(new HttpHost(endpoint));

        final PoolingHttpClientConnectionManager connManager
                = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(MaxThreads);
        connManager.setDefaultMaxPerRoute(MaxThreads);
        connManager.setSocketConfig(route.getTargetHost(), SocketConfig.custom().
                setSoTimeout(TimeoutInMs).build());
        connManager.setValidateAfterInactivity(30 * 1000);

        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setRetryHandler(retryHandler())
                .setConnectionTimeToLive(50, TimeUnit.SECONDS)
                .build();
    }

    private HttpRequestRetryHandler retryHandler() {
        return (exception, executionCount, context) -> {

            if (executionCount > 2) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final HttpRequest request = clientContext.getRequest();
            // Retry if the request is considered idempotent
            return !(request instanceof HttpEntityEnclosingRequest);
        };
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
//        System.out.println(String.format("host:%s\nmethod:%s\nalgorithm:%s\nshortDate:%s\nlongDate:%s\ncredentialScope:%s\nqueryParameters:%s\nuri:%s\nfinalUri:%s\n",
//                usabillaHost, method, HMAC_SHA_256, shortDate, longDate, credentialScope, queryParameters, uri, finalUri));
//
//        String canonicalHeaders = getCanonicalHeaders(usabillaHost, longDate);
//        System.out.println(String.format("\ncanonicalHeaders:\n%s", canonicalHeaders));
//
//        System.out.println(String.format("\nsignedHeaders:\n%s", signedHeaders));
//
//        String payload = StringUtils.EMPTY;
//        String requestPayload = DigestUtils.sha256Hex(payload);
//        System.out.println(String.format("\nrequestPayload:\n%s", requestPayload));
//
//        //Create Canonical String
//        String canonicalString = String.format("%s\n%s\n%s\n%s\n%s\n%s", method, uri, queryParameters, canonicalHeaders, signedHeaders, requestPayload);
//        System.out.println(String.format("\ncanonicalString:\n%s", canonicalString));
//
//        String hashedCanonicalString = DigestUtils.sha256Hex(canonicalString);
//        System.out.println(String.format("\nhashedCanonicalString:\n%s", hashedCanonicalString));
//
//        //Create string to Sign
//        String stringToSign = String.format("%s\n%s\n%s\n%s", HMAC_SHA_256, longDate, credentialScope, hashedCanonicalString);
//        System.out.println(String.format("\nstringToSign:\n%s", stringToSign));
//
//        //Create signature
//        String secretKey = String.format("USBL1%s", secret);
//        System.out.println("\nsecretKey:\n" + secretKey);
//        System.out.println("\ndata:\n" + shortDate);
//
//        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
//        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
//        sha256_HMAC.init(secret_key);
//        byte[] kDateBytes = sha256_HMAC.doFinal(shortDate.getBytes());
//        String kDate = toHexString(kDateBytes);
//        System.out.println(String.format("\nkDate:\n%s", kDate));
//
//        secret_key = new SecretKeySpec(kDateBytes, "HmacSHA256");
//        sha256_HMAC.init(secret_key);
//        byte[] signingKeyBytes = sha256_HMAC.doFinal("usbl1_request".getBytes());
//        String kSigning = toHexString(signingKeyBytes);
//        System.out.println(String.format("\nkSigning:\n%s", kSigning));
//
//        secret_key = new SecretKeySpec(signingKeyBytes, "HmacSHA256");
//        sha256_HMAC.init(secret_key);
//        byte[] signatureBytes = sha256_HMAC.doFinal(stringToSign.getBytes());
//        String signature = toHexString(signatureBytes);
//        System.out.println(String.format("\nsignature:\n%s", signature));
//
//        String authorizationHeader = String.join(", ",
//                String.join(StringUtils.EMPTY, HMAC_SHA_256, " Credential=", accessKey, "/", shortDate, "/usbl1_request"),
//                String.join(StringUtils.EMPTY, "SignedHeaders=", signedHeaders),
//                String.join(StringUtils.EMPTY, "Signature=", signature));
//        System.out.println(String.format("\nauthorizationHeader:\n%s", authorizationHeader));
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
