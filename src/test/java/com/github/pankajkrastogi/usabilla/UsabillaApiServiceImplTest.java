package com.github.pankajkrastogi.usabilla;

import com.github.pankajkrastogi.usabilla.auth.UsabillaAuthBuilder;
import com.github.pankajkrastogi.usabilla.utils.CommonUtils;
import com.github.pankajkrastogi.usabilla.utils.HttpMethod;
import com.usabilla.AbstractTest;
import com.github.pankajkrastogi.usabilla.client.UsabillaClient;
import com.github.pankajkrastogi.usabilla.client.model.RequestCommand;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import java.nio.charset.Charset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UsabillaApiServiceImplTest extends AbstractTest {

    @Mock
    private UsabillaClient usabillaClient;

    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private HttpEntity httpEntity;

    private UsabillaAuthBuilder usabillaAuthBuilder;
    private UsabillaApiService usabillaApiService;

    @BeforeEach
    public void setUp() {
        usabillaAuthBuilder = new UsabillaAuthBuilder("secret", "accessKey");
        usabillaApiService = new UsabillaApiServiceImpl(usabillaAuthBuilder, usabillaClient);
    }

    @Test
    void getAllButtons() throws Exception {
        long since = 1571155433000L;
        int limit = 10;

        final Date currDate = new Date(since);
        final String method = HttpMethod.GET.name();
        final String requestUri = CommonUtils.BUTTONS_URI;
        final String queryString = String.format("limit=%s&since=%s", limit, since);

        final RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);

        //given
        Mockito.when(usabillaClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK, "OK");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);

        String buttonResponseJson = getJsonFromFile("metadata/GetButtonsResponse.json");
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpEntity.getContent()).thenReturn(IOUtils.toInputStream(buttonResponseJson, Charset.defaultCharset()));

        String allFeedbackButtons = usabillaApiService.getAllFeedbackButtons(requestCommand);
        assertNotNull(allFeedbackButtons);

        JSONAssert.assertEquals(buttonResponseJson, allFeedbackButtons, false);
    }

    @Test
    void getFeedbackOnButton() throws Exception {
        long since = 1571155433000L;
        int limit = 10;
        String buttonId = "button5056";

        final Date currDate = new Date(since);
        final String method = HttpMethod.GET.name();
        final String requestUri = CommonUtils.BUTTONS_URI + "/" + buttonId + "/feedback";
        final String queryString = String.format("limit=%s&since=%s", limit, since);

        final RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);

        Mockito.when(usabillaClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK, "OK");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);

        String expectedJson = getJsonFromFile("metadata/feedbackOnButton.json");
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpEntity.getContent()).thenReturn(IOUtils.toInputStream(expectedJson, Charset.defaultCharset()));

        String feedbacks = usabillaApiService.getFeedbackOnButton(requestCommand);
        assertNotNull(feedbacks);

        JSONAssert.assertEquals(expectedJson, feedbacks, false);
    }
}