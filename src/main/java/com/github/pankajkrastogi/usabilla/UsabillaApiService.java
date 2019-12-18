package com.github.pankajkrastogi.usabilla;

import com.github.pankajkrastogi.usabilla.client.model.RequestCommand;

public interface UsabillaApiService {

   String getAllFeedbackButtons(final RequestCommand requestCommand) throws Exception;

   String getFeedbackOnButton(final RequestCommand requestCommand) throws Exception;
}
