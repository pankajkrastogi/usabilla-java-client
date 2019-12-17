# Java client for Usabilla API

#Usage

## Example code to build Auth Token
   
   ```java
    class Test {
       void buildRequestCommand() throws InvalidKeyException, NoSuchAlgorithmException {
           final UsabillaAuthBuilder usabillaAuthBuilder = new UsabillaAuthBuilder("mySecret", "myAccessKey");
    
           final LocalDateTime localDateTime = LocalDateTime.of(2019, 9, 19, 16, 52, 21, 10);
           final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
           final Date currDate = Date.from(zonedDateTime.toInstant());
    
           final String method = HttpMethod.GET.name();
           final String requestUri = CommonUtils.BUTTONS_URI;
           final String queryString = "limit=10&since=1568714350000";
    
           RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);
       }
    }
   ```

## Example code to get all feedback buttons

```java
    class Test {
        void getAllButtons() throws Exception {
            long since = 1571155433000L;
            int limit = 10;
    
            final Date currDate = new Date(since);
            final String method = HttpMethod.GET.name();
            final String requestUri = CommonUtils.BUTTONS_URI;
            final String queryString = String.format("limit=%s&since=%s", limit, since);
    
            final RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);
            final String allFeedbackButtonsJson = usabillaApiService.getAllFeedbackButtons(requestCommand);
        }
    }
```

### Example response for get all feedback buttons
```json
{
  "items":[
    {
      "id":"5dc1a2d9b2461d5fa81533b6",
      "userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36",
      "comment":"Good collection of article on homepage ",
      "commentTranslated":"",
      "commentTranslatedFrom":"",
      "location":"",
      "browser":{
        "name":"Chrome",
        "version":"78.0.3904.87",
        "os":"MacOSX",
        "devicetype":"Desktop"
      },
      "date":"2019-11-05T16:27:06.005Z",
      "custom":{
        "accessType":"ae:REG_ONLINE_REGISTERED",
        "accountId":"ae:593",
        "feedback_category":"opt1"
      },
      "email":"",
      "image":"",
      "labels":[
        "compliment"
      ],
      "nps":0,
      "publicUrl":null,
      "rating":4,
      "buttonId":"button5056",
      "tags":[

      ],
      "url":"https://xxx.com/",
      "Bucket":""
    }
   ]
}
```

## Example code to get feedback on given button
```java
    class Test {
        void getAllButtons() throws Exception {
            long since = 1571155433000L;
            int limit = 10;
            String buttonId = "button5056";
    
            final Date currDate = new Date(since);
            final String method = HttpMethod.GET.name();
            final String requestUri = BUTTONS_URI + "/" + buttonId + "/feedback";
            final String queryString = String.format("limit=%s&since=%s", limit, since);
    
            final RequestCommand requestCommand = usabillaAuthBuilder.buildRequestCommand(currDate, method, requestUri, queryString);
            String feedbackJson = usabillaApiService.getFeedbackOnButton(requestCommand);
        }
    }
```

### Example response for get feedback on given button
```json
{
  "items":[
    {
      "id":"d84c9fdb7c8b",
      "name":"Feedback Button"
    },
    {
      "id":"7fb2bbcd20b7",
      "name":"Suggestion Button"
    }
  ],
  "count":2,
  "hasMore":false,
  "lastTimestamp":0
}
```