package com.usabilla.api;

import com.usabilla.AbstractTest;
import com.usabilla.api.auth.UsabillaAuthBuilder;
import com.usabilla.api.client.model.RequestCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsabillaApiServiceImplTest extends AbstractTest {

    @Mock
    private UsabillaAuthBuilder usabillaAuthBuilder;

    private UsabillaApiService usabillaApiService;

    @BeforeEach
    public void setUp() {
//        usabillaAuthBuilder = new UsabillaAuthBuilder("fc044773c6cd90ffbe81", "c38c261fc6990360");
        usabillaApiService = new UsabillaApiServiceImpl(usabillaAuthBuilder);
    }

    @Test
    void getAllButtons() throws Exception {
        //given
        String buttonResponseJson = getJsonFromFile("metadata/GetButtonsResponse.json");

        RequestCommand requestCommand = new RequestCommand("USBL1-HMAC-SHA256 Credential=c38c261fc6990360/20190917/usbl1_request, SignedHeaders=host;x-usbl-date, Signature=df042c8b4b82d5ed36ebe506e3742589c50424f74693e306fb980deb4590dcf1"
                , "20190917T105910Z", "/live/websites/button?limit=10&since=1568714350000");
        Mockito.when(usabillaAuthBuilder.buildRequestCommand(any(Date.class), anyString(), anyString(), anyString()))
                .thenReturn(requestCommand);

        String allFeedbackButtons = usabillaApiService.getAllFeedbackButtons(10, 1568714350000L);
        assertNotNull(allFeedbackButtons);

        //assertEquals(buttonResponseJson, allFeedbackButtons);
    }
}