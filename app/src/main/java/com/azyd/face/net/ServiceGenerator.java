package com.azyd.face.net;

import android.annotation.SuppressLint;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;

import com.azyd.face.constant.NetConstant;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author suntao
 * @creat-time 2018/12/12 on 17:49
 * $describe$
 */
public class ServiceGenerator {
    private final static Retrofit.Builder BUILDER = new Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(new StringConverterFactory())
            .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                    .setLenient()
                    .create()));



    private final static OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            //连接超时时间
            .connectTimeout(NetConstant.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            //写操作 超时时间
            .writeTimeout(NetConstant.WRITE_TIMEOUT, TimeUnit.SECONDS)
            //读操作超时时间
            .readTimeout(NetConstant.READ_TIMEOUT, TimeUnit.SECONDS)
//            .addInterceptor(new HttpsInterceptor())
            .sslSocketFactory(createSSLSocketFactory())
            .hostnameVerifier(new TrustAllHostnameVerifier())
            .build();


    public static <S> S createService(Class<S> serviceClass) {
        OkHttpClient client = HTTP_CLIENT.newBuilder()
                .build();

        Retrofit retrofit = BUILDER.baseUrl("https://w.cekid.com/")
                .client(client)
                .build();
        return retrofit.create(serviceClass);
    }




    /**
     * 默认信任所有的证书
     * @return SSLSocketFactory
     */
    @SuppressLint("TrulyRandom")
    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory sSLSocketFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllManager()},
                    new SecureRandom());
            sSLSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sSLSocketFactory;
    }

    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static class HttpsInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request source = chain.request();
            if (source.isHttps()) {
                return chain.proceed(source);
            }

            HttpUrl httpUrl = source.url().newBuilder()
                    .scheme("https")
                    .port(443)
                    .build();
            Request request = source.newBuilder()
                    .url(httpUrl)
                    .build();
            return chain.proceed(request);
        }
    }
}
