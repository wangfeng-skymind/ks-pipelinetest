package org.konduit.test;

import io.vertx.core.json.Json;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.nd4j.common.io.ClassPathResource;

import java.io.File;

public class TestJava {

    @Test
    public void TestOne() throws Exception {
        String data = new ClassPathResource("5.png").getFile().getAbsolutePath();
        File file =new ClassPathResource("5.png").getFile();
        String url = "http://127.0.0.1:9000/predict";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(200000).setSocketTimeout(200000000).build();
        httpPost.setConfig(requestConfig);
        httpPost.setHeader("Accept", "application/json");
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addBinaryBody("dbImgI",file, ContentType.IMAGE_PNG,"test12Img");
        multipartEntityBuilder.addTextBody("comment", "Testing Example");
        HttpEntity httpEntity = multipartEntityBuilder.build() ;
        httpPost.setEntity(httpEntity);
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity responseEntity = httpResponse.getEntity();
        int statusCode= httpResponse.getStatusLine().getStatusCode();

        if(statusCode == 200){
            String strResult = EntityUtils.toString(responseEntity, "UTF-8");
            Object jsonObject = Json.decodeValue(strResult);
            System.out.println("Response content11: " + strResult);

        }
        httpClient.close();
        if(httpResponse!=null){
            httpResponse.close();
        }
    }
}
