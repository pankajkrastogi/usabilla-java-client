package com.usabilla.api.client.model;

import java.util.StringJoiner;

public class RequestCommand {

    private String authorizationHeader;

    private String xUsblDateHeader;

    private String uri;

    public RequestCommand(final String authorizationHeader, final String xUsblDateHeader, final String uri) {
        this.authorizationHeader = authorizationHeader;
        this.xUsblDateHeader = xUsblDateHeader;
        this.uri = uri;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RequestCommand.class.getSimpleName() + "[", "]")
                .add("authorizationHeader='" + authorizationHeader + "'")
                .add("xUsblDateHeader='" + xUsblDateHeader + "'")
                .add("url='" + uri + "'")
                .toString();
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public String getxUsblDateHeader() {
        return xUsblDateHeader;
    }

    public void setxUsblDateHeader(String xUsblDateHeader) {
        this.xUsblDateHeader = xUsblDateHeader;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
