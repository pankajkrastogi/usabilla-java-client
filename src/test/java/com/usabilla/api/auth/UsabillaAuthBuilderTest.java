package com.usabilla.api.auth;

import com.usabilla.api.client.model.RequestCommand;
import com.usabilla.api.utils.CommonUtils;
import com.usabilla.api.utils.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UsabillaAuthBuilderTest {

    private UsabillaAuthBuilder usabillaAuthBuilder;

    @BeforeEach
    void setUp() {
        usabillaAuthBuilder = new UsabillaAuthBuilder("mySecret", "myAccessKey");
    }

    @Test
    void buildRequestCommand() throws InvalidKeyException, NoSuchAlgorithmException {
        final LocalDateTime localDateTime = LocalDateTime.of(2019, 9, 19, 16, 52, 21, 10);
        final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        final Date currDate = Date.from(zonedDateTime.toInstant());

        final String method = HttpMethod.GET.name();
        final String requestUri = CommonUtils.BUTTONS_URI;
        final String queryString = "limit=10&since=1568714350000";

        RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);
        assertNotNull(requestCommand);
        assertEquals("USBL1-HMAC-SHA256 Credential=myAccessKey/20190919/usbl1_request, SignedHeaders=host;x-usbl-date, Signature=65fcfa32923501b1eec1b7751e57afa1c13720c9b769d484f5307a1a4899513e",
                requestCommand.getAuthorizationHeader());
        assertEquals("20190919T165221Z", requestCommand.getxUsblDateHeader());
        assertEquals("/live/websites/button?limit=10&since=1568714350000", requestCommand.getUri());
    }
}