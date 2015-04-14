package com.beetle.bauhinia.api;


import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Image;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import rx.Observable;

/**
 * Created by tsung on 10/10/14.
 */
public class IMHttpAPI {
    public static final String API_URL = "http://api.gobelieve.io";

    private static IMHttp newIMHttp() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(apiURL)
                .setConverter(new GsonConverter(new Gson()))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        if (IMHttpAPI.accessToken != null && !IMHttpAPI.accessToken.equals("")) {
                            request.addHeader("Authorization", "Bearer " + IMHttpAPI.accessToken);
                        }
                    }
                })
                .build();

        return adapter.create(IMHttp.class);
    }

    static final Object monitor = new Object();
    static IMHttp singleton;

    public static IMHttp Singleton() {
        if (singleton == null) {
            synchronized (monitor) {
                if (singleton == null) {
                    singleton = newIMHttp();
                }
            }
        }

        return singleton;
    }

    private static String apiURL = API_URL;
    private static String accessToken;

    public static void setAPIURL(String url) {
        apiURL = url;
    }

    public static void setToken(String token) {
        accessToken = token;
    }


    public interface IMHttp {
        @POST("/device/bind")
        Observable<Object> bindDeviceToken(@Body PostDeviceToken token);

        @POST("/device/unbind")
        Observable<Object> unBindDeviceToken(@Body PostDeviceToken token);

        @POST("/images")
        Observable<Image> postImages(@Header("Content-Type") String contentType, @Body TypedFile file);

        @POST("/audios")
        Observable<Audio> postAudios(@Header("Content-Type") String contentType, @Body TypedFile file);
    };


}
