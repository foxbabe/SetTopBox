package com.savor.ads.okhttp.request;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PostProtoBufferRequest extends OkHttpRequest {
    private static MediaType MEDIA_TYPE = MediaType.parse("x-protobuf;charset=utf-8");

    private byte[] content;

    public PostProtoBufferRequest(String url, Object tag, Map<String, String> params, Map<String, String> headers, byte[] content) {
        super(url, tag, params, headers);
        this.content = content;
    }

    @Override
    protected RequestBody buildRequestBody() {
        return RequestBody.create(MEDIA_TYPE, content);
    }

    @Override
    protected Request buildRequest(RequestBody requestBody) {
        return builder.post(requestBody).build();
    }
}
