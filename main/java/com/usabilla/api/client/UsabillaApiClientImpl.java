package com.usabilla.api.client;

import com.usabilla.api.client.model.FeedbackCommand;
import com.usabilla.api.client.utils.HttpMethod;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class UsabillaApiClientImpl implements UsabillaApiClient {

    private static final String HMAC_SHA_256 = "USBL1-HMAC-SHA256";

    private static final DateTimeFormatter shortDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter longDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final String Z = "Z";
    private static final String USBL_1_REQUEST = "/usbl1_request";

    private static final String BUTTONS_URI = "/live/websites/button";
    private static final String x_usbl_date = "x-usbl-date";
    private static final String signedHeaders = "host;x-usbl-date";
    private static final String TIMEOUT = "timeout";
    private static final String ACCEPT = "Accept";

    private final String secret;
    private final String accessKey;
    private String usabillaHost;
    private String BASE_URL;

    private int TimeoutInMs;
    private int KeepAliveInSec;
    private int MaxThreads;

    public UsabillaApiClientImpl(final String secret, final String accessKey) {
        this.secret = secret;
        this.accessKey = accessKey;

        this.BASE_URL = "https://data.usabilla.com";
        this.usabillaHost = "host:data.usabilla.com";
        this.TimeoutInMs = 10000;
        this.KeepAliveInSec = 60;
        this.MaxThreads = 5;
    }

    public UsabillaApiClientImpl(final String secret, final String accessKey,
                                 final String usabillaHost, final int timeoutInMs, final int keepAliveInSec, final int maxThreads) {
        this.secret = secret;
        this.accessKey = accessKey;
        this.usabillaHost = usabillaHost;
        this.TimeoutInMs = timeoutInMs;
        this.KeepAliveInSec = keepAliveInSec;
        this.MaxThreads = maxThreads;
    }

    public UsabillaApiClientImpl(final String secret, final String accessKey, final String usabillaHost,
                                 final String BASE_URL, final int timeoutInMs, final int keepAliveInSec, final int maxThreads) {
        this.secret = secret;
        this.accessKey = accessKey;
        this.usabillaHost = usabillaHost;
        this.BASE_URL = BASE_URL;
        this.TimeoutInMs = timeoutInMs;
        this.KeepAliveInSec = keepAliveInSec;
        this.MaxThreads = maxThreads;
    }

    public String getAllFeedbackButtons() throws Exception {

        //https://data.usabilla.com/live/websites/button

        LocalDateTime localDateTime = LocalDateTime.of(2019, 9, 19, 16, 52, 21, 10);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        final Date currDate = Date.from(zonedDateTime.toInstant());

        final String method = HttpMethod.GET.name();
        final String requestUri = BUTTONS_URI;
        final String host = usabillaHost;
        final String queryString = "limit=10&since=1568714350000";

        final FeedbackCommand requestCommand = getRequestCommand(currDate, method, requestUri, queryString, host);

        final CloseableHttpClient closeableHttpClient = buildHttpClient(BASE_URL);

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
        return new URIBuilder(String.join("/", BASE_URL, uri))
                .build();
    }

    private FeedbackCommand getRequestCommand(
            final Date currDate,
            final String method,
            final String requestUri,
            final String queryString,
            final String host
    ) throws NoSuchAlgorithmException, InvalidKeyException {
        final String shortDate = getShortDateString(currDate);
        final String longDate = getLongDateString(currDate);
        final String credentialScope = getCredentialScope(shortDate);

        final String finalUri = getUri(requestUri, queryString);
        System.out.println(String.format("host:%s\nmethod:%s\nalgorithm:%s\nshortDate:%s\nlongDate:%s\ncredentialScope:%s\nuri:%s\nfinalUri:%s\n",
                host, method, HMAC_SHA_256, shortDate, longDate, credentialScope, requestUri, finalUri));

        final String canonicalHeaders = getCanonicalHeaders(host, longDate);
        System.out.println(String.format("\ncanonicalHeaders:\n%s", canonicalHeaders));

        System.out.println(String.format("\nsignedHeaders:\n%s", signedHeaders));

        final String requestPayload = DigestUtils.sha256Hex(StringUtils.EMPTY);
        System.out.println(String.format("\nrequestPayload:\n%s", requestPayload));

        //Create Canonical String
        final String canonicalString = getCanonicalString(method, requestUri, canonicalHeaders, requestPayload, queryString);
        System.out.println(String.format("\ncanonicalString:\n%s", canonicalString));

        final String hashedCanonicalString = DigestUtils.sha256Hex(canonicalString);
        System.out.println(String.format("\nhashedCanonicalString:\n%s", hashedCanonicalString));

        //Create string to Sign
        final String stringToSign = getStringToSign(longDate, credentialScope, hashedCanonicalString);
        System.out.println(String.format("\nstringToSign:\n%s", stringToSign));

        //Create signature
        final String secretKey = String.format("USBL1%s", secret);
        System.out.println("\nsecretKey:\n" + secretKey);
        System.out.println("\ndata:\n" + shortDate);

        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final byte[] kDateBytes = sha256_HMAC.doFinal(shortDate.getBytes());
        String kDate = toHexString(kDateBytes);
        System.out.println(String.format("\nkDate:\n%s", kDate));

        secret_key = new SecretKeySpec(kDateBytes, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final byte[] signingKeyBytes = sha256_HMAC.doFinal("usbl1_request".getBytes());
        final String kSigning = toHexString(signingKeyBytes);
        System.out.println(String.format("\nkSigning:\n%s", kSigning));

        secret_key = new SecretKeySpec(signingKeyBytes, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final byte[] signatureBytes = sha256_HMAC.doFinal(stringToSign.getBytes());
        final String signature = toHexString(signatureBytes);
        System.out.println(String.format("\nsignature:\n%s", signature));

        final String authorizationHeader = String.join(", ",
                String.join(StringUtils.EMPTY, HMAC_SHA_256, " Credential=", accessKey, "/", shortDate, "/usbl1_request"),
                String.join(StringUtils.EMPTY, "SignedHeaders=", signedHeaders),
                String.join(StringUtils.EMPTY, "Signature=", signature));
        System.out.println(String.format("\nauthorizationHeader:\n%s", authorizationHeader));

        return new FeedbackCommand(authorizationHeader, longDate, finalUri);
    }

    private String getStringToSign(final String longDate, final String credentialScope, final String hashedCanonicalString) {
        return String.format("%s\n%s\n%s\n%s", HMAC_SHA_256, longDate, credentialScope, hashedCanonicalString);
    }

    private String getCanonicalString(final String method, final String requestUri, final String canonicalHeaders,
                                      final String requestPayload, final String queryParams) {
        if (StringUtils.isNoneEmpty(queryParams)) {
            return String.format("%s\n%s\n%s\n%s\n%s\n%s", method, requestUri, queryParams, canonicalHeaders, signedHeaders, requestPayload);
        }
        return String.format("%s\n%s\n%s\n%s\n%s", method, requestUri, canonicalHeaders, signedHeaders, requestPayload);
    }

    private String getCanonicalHeaders(final String host, final String longDate) {
        return String.format("%s\n%s:%s\n", host, x_usbl_date, longDate);
    }

    private String getUri(final String uri, final String queryParams) {
        final String baseUri = String.format("%s", uri);
        if (Objects.nonNull(queryParams)) {
            return String.format("%s?%s", baseUri, queryParams);
        }
        return baseUri;
    }

    private String getCredentialScope(final String shortDate) {
        return shortDate + USBL_1_REQUEST;
    }

    private String getLongDateString(final Date currDate) {
        return getDateString(currDate, longDateTimeFormatter) + Z;
    }

    private String getShortDateString(final Date currDate) {
        return getDateString(currDate, shortDateTimeFormatter);
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
