package com.alibaba.mesh.demo;

import com.alibaba.fastjson.JSON;

import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author yiji
 */
public class HttpInvoker {

    private static OkHttpClient client;

    private static int invokeTimes = 1;

    public static void main(String[] args) throws Exception {
        init();
        for(int i = 0; i < invokeTimes; i++){
            invoke();
        }
    }

    public static void init() {

        // 使用100个连接，默认是5个。
        // okhttp使用http 1.1，默认打开keep-alive
        ConnectionPool pool = new ConnectionPool(100, 5L, TimeUnit.MINUTES);

        client = new OkHttpClient.Builder()
                .connectionPool(pool)
                .connectTimeout(60, TimeUnit.SECONDS)       //设置连接超时
                .readTimeout(60, TimeUnit.SECONDS)          //不考虑超时
                .writeTimeout(60, TimeUnit.SECONDS)          //不考虑超时
                .retryOnConnectionFailure(true)
                .build();
    }

    static Random r = new Random(System.currentTimeMillis());

    public static void invoke() throws Exception {


        String str = RandomStringUtils.random(r.nextInt(1024), true, true);
        String url = "http://127.0.0.1:20000";

        RequestBody formBody = new FormBody.Builder()
                .add("interface","com.alibaba.dubbo.performance.demo.provider.IHelloService")
                .add("method","hash")
                .add("parameterTypesString","Ljava/lang/String;")
                .add("parameter",str)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            byte[] bytes = response.body().bytes();
            int hash =  JSON.parseObject(bytes, Integer.class);
            int expectHash = str.hashCode();
            if(hash != expectHash){
                throw new RuntimeException("expected hash: " + expectHash + ", actual: " + hash);
            }
        }
    }

}
