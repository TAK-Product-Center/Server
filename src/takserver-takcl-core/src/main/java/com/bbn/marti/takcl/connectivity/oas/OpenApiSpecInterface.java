package com.bbn.marti.takcl.connectivity.oas;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface OpenApiSpecInterface {

    @GET("v3/api-docs")
    @Headers({"Content-Type: application/json;charset=UTF-8"})
    Call<JsonObject> getOpenApiSpec();

}
