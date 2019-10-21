package com.usabilla.api.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsabillaApiClientImplTest {

    private UsabillaApiClient usabillaApiClient;

    @BeforeEach
    void setUp() {
        usabillaApiClient = new UsabillaApiClientImpl("fc044773c6cd90ffbe81", "c38c261fc6990360");
    }

    @Test
    void getAllButtons() throws Exception {

        String allFeedbackButtons = usabillaApiClient.getAllFeedbackButtons();
    }
}