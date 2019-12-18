package com.github.pankajkrastogi.usabilla.auth;

import com.github.pankajkrastogi.usabilla.client.model.RequestCommand;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Formatter;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UsabillaAuthBuilder {

    private static final Logger LOGGER = LogManager.getLogger(UsabillaAuthBuilder.class);

    private static final String HMAC_SHA_256 = "USBL1-HMAC-SHA256";

    private static final DateTimeFormatter shortDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter longDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final String Z = "Z";
    private static final String USBL_1_REQUEST = "/usbl1_request";

    private static final String x_usbl_date = "x-usbl-date";
    private static final String signedHeaders = "host;x-usbl-date";

    private final String secret;
    private final String accessKey;
    private final  String usabillaHost;

    public UsabillaAuthBuilder(final String secret, final String accessKey) {
        this.secret = secret;
        this.accessKey = accessKey;
        this.usabillaHost = "host:data.usabilla.com";
    }

    public RequestCommand buildRequestCommand(final Date currDate, final String method, final String requestUri,
                                            final String queryString) throws NoSuchAlgorithmException, InvalidKeyException {
        final String shortDate = getShortDateString(currDate);
        final String longDate = getLongDateString(currDate);
        final String credentialScope = getCredentialScope(shortDate);

        final String finalUri = getUri(requestUri, queryString);
       LOGGER.debug(String.format("usabillaHost:%s\nmethod:%s\nalgorithm:%s\nshortDate:%s\nlongDate:%s\ncredentialScope:%s\nuri:%s\nfinalUri:%s\n",
                usabillaHost, method, HMAC_SHA_256, shortDate, longDate, credentialScope, requestUri, finalUri));

        final String canonicalHeaders = getCanonicalHeaders(usabillaHost, longDate);
       LOGGER.debug(String.format("\ncanonicalHeaders:\n%s", canonicalHeaders));

       LOGGER.debug(String.format("\nsignedHeaders:\n%s", signedHeaders));

        final String requestPayload = DigestUtils.sha256Hex(StringUtils.EMPTY);
       LOGGER.debug(String.format("\nrequestPayload:\n%s", requestPayload));

        //Create Canonical String
        final String canonicalString = getCanonicalString(method, requestUri, canonicalHeaders, requestPayload, queryString);
       LOGGER.debug(String.format("\ncanonicalString:\n%s", canonicalString));

        final String hashedCanonicalString = DigestUtils.sha256Hex(canonicalString);
       LOGGER.debug(String.format("\nhashedCanonicalString:\n%s", hashedCanonicalString));

        //Create string to Sign
        final String stringToSign = getStringToSign(longDate, credentialScope, hashedCanonicalString);
       LOGGER.debug(String.format("\nstringToSign:\n%s", stringToSign));

        //Create signature
        final String secretKey = String.format("USBL1%s", secret);
       LOGGER.debug("\nsecretKey:\n" + secretKey);
       LOGGER.debug("\ndata:\n" + shortDate);

        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final byte[] kDateBytes = sha256_HMAC.doFinal(shortDate.getBytes());
        String kDate = toHexString(kDateBytes);
       LOGGER.debug(String.format("\nkDate:\n%s", kDate));

        secret_key = new SecretKeySpec(kDateBytes, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final byte[] signingKeyBytes = sha256_HMAC.doFinal("usbl1_request".getBytes());
        final String kSigning = toHexString(signingKeyBytes);
       LOGGER.debug(String.format("\nkSigning:\n%s", kSigning));

        secret_key = new SecretKeySpec(signingKeyBytes, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        final byte[] signatureBytes = sha256_HMAC.doFinal(stringToSign.getBytes());
        final String signature = toHexString(signatureBytes);
       LOGGER.debug(String.format("\nsignature:\n%s", signature));

        final String authorizationHeader = String.join(", ",
                String.join(StringUtils.EMPTY, HMAC_SHA_256, " Credential=", accessKey, "/", shortDate, "/usbl1_request"),
                String.join(StringUtils.EMPTY, "SignedHeaders=", signedHeaders),
                String.join(StringUtils.EMPTY, "Signature=", signature));
       LOGGER.debug(String.format("\nauthorizationHeader:\n%s", authorizationHeader));

        return new RequestCommand(authorizationHeader, longDate, finalUri);
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
