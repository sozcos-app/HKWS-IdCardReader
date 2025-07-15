package com.sozcos.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    private static final String DOMAIN = "https://zhiwen-sdk.xfyun.cn";

    public static void main(String[] args) {
        String appId = "9d78f203";
        String secret = "YTIxNDA2ODAzMTdmODU2YTA2MDg3YTUx";
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = ApiAuthAlgorithm.getSignature(appId, secret, timestamp); // 见下章节： 接口签名 - 代码示例

        // POST JSON示例
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("bizUid", appId);

        HashMap<String, Object> headers = new HashMap<>();
        headers.put("appId", appId);
        headers.put("timestamp", timestamp);
        headers.put("signature", signature);
        try {
            String jsonResponse = HttpUtils.postJson(
                    DOMAIN + "/api/user/login",
                    headers,
                    requestBody
            );
            System.out.println("JSON Response: " + jsonResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送GET请求
     *
     * @param url     请求URL
     * @param params  请求参数，可以为null
     * @param headers 请求头，可以为null
     * @return 响应内容
     * @throws IOException 如果发生I/O错误
     */
    public static String get(String url, Map<String, Object> params, Map<String, Object> headers) throws IOException {
        String fullUrl = buildUrlWithParams(url, params);
        HttpURLConnection connection = null;
        try {
            connection = createConnection(fullUrl, "GET", headers);
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送POST请求（JSON格式，不带URL参数）
     *
     * @param url     请求URL
     * @param headers 请求头，可以为null
     * @param body    请求体Map，可以为null
     * @return 响应内容
     * @throws IOException 如果发生I/O错误
     */
    public static String postJson(String url, Map<String, Object> headers, Map<String, Object> body) throws IOException {
        return post(url, null, headers, body, BodyFormat.JSON);
    }

    /**
     * 发送POST请求（表单格式，不带URL参数）
     *
     * @param url     请求URL
     * @param headers 请求头，可以为null
     * @param body    请求体Map，可以为null
     * @return 响应内容
     * @throws IOException 如果发生I/O错误
     */
    public static String postForm(String url, Map<String, Object> headers, Map<String, Object> body) throws IOException {
        return post(url, null, headers, body, BodyFormat.FORM_URLENCODED);
    }

    /**
     * 发送POST请求（JSON格式，带URL参数）
     *
     * @param url     请求URL
     * @param params  URL参数，可以为null
     * @param headers 请求头，可以为null
     * @param body    请求体Map，可以为null
     * @return 响应内容
     * @throws IOException 如果发生I/O错误
     */
    public static String postJson(String url, Map<String, Object> params, Map<String, Object> headers, Map<String, Object> body) throws IOException {
        return post(url, params, headers, body, BodyFormat.JSON);
    }

    /**
     * 发送POST请求（表单格式，带URL参数）
     *
     * @param url     请求URL
     * @param params  URL参数，可以为null
     * @param headers 请求头，可以为null
     * @param body    请求体Map，可以为null
     * @return 响应内容
     * @throws IOException 如果发生I/O错误
     */
    public static String postForm(String url, Map<String, Object> params, Map<String, Object> headers, Map<String, Object> body) throws IOException {
        return post(url, params, headers, body, BodyFormat.FORM_URLENCODED);
    }

    /**
     * 发送POST请求（核心方法）
     */
    private static String post(String url, Map<String, Object> params, Map<String, Object> headers,
                               Map<String, Object> body, BodyFormat format) throws IOException {
        String fullUrl = buildUrlWithParams(url, params);
        HttpURLConnection connection = null;
        try {
            connection = createConnection(fullUrl, "POST", headers);

            if (body != null && !body.isEmpty()) {
                String requestBody = format == BodyFormat.JSON ?
                        mapToJson(body) : mapToFormUrlEncoded(body);

                connection.setDoOutput(true);
                if (format == BodyFormat.JSON) {
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                } else {
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                }

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 构建带参数的URL
     */
    private static String buildUrlWithParams(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder urlBuilder = new StringBuilder(url);
        if (!url.contains("?")) {
            urlBuilder.append("?");
        } else if (!url.endsWith("&")) {
            urlBuilder.append("&");
        }

        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(entry.getKey())
                    .append("=")
                    .append(encodeValue(entry.getValue()));
            first = false;
        }

        return urlBuilder.toString();
    }

    /**
     * 创建HTTP连接
     */
    private static HttpURLConnection createConnection(String url, String method, Map<String, Object> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // 设置请求头
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        return connection;
    }

    /**
     * 读取响应内容
     */
    private static String readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    errorResponse.append(line);
                }
                throw new IOException("HTTP error code: " + responseCode + ", response: " + errorResponse.toString());
            }
        }
    }

    /**
     * 对参数值进行URL编码
     */
    private static String encodeValue(Object value) {
        try {
            return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 将Map转换为JSON字符串
     */
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 将Map转换为表单编码字符串
     */
    private static String mapToFormUrlEncoded(Map<String, Object> map) {
        StringBuilder form = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                form.append("&");
            }
            form.append(entry.getKey())
                    .append("=")
                    .append(encodeValue(entry.getValue()));
            first = false;
        }
        return form.toString();
    }
}
