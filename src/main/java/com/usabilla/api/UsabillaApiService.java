package com.usabilla.api;

import com.usabilla.api.client.model.RequestCommand;

public interface UsabillaApiService {

   String getAllFeedbackButtons(final RequestCommand requestCommand) throws Exception;

   String getFeedbackOnButton(final RequestCommand requestCommand) throws Exception;
}
