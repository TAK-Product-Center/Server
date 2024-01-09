package com.bbn.marti.takcl.connectivity.oas;

import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.connectivity.missions.MissionDataSyncClient;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class OpenApiSpecClient extends MissionDataSyncClient {

    private OpenApiSpecInterface _oasApi;
    private final String filePath = "/opt/tak/TEST_RESULTS/TEST_ARTIFACTS/openapispec.json";

    public OpenApiSpecClient(@NotNull AbstractUser user) {
        super(user);
    }

    private synchronized OpenApiSpecInterface oasApi() {
        if (_oasApi == null) {
            _oasApi = createApi(OpenApiSpecInterface.class);
        }
        return _oasApi;
    }

    private <T> Response<T> getResponse(Call<T> call) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Response<JsonObject> getOpenApiSpec() {
        Response<JsonObject> oas = getResponse(oasApi().getOpenApiSpec());
        if (oas.body() != null) {
            saveOpenApiSpec(oas.body());
        }
        return oas;
    }

    private void saveOpenApiSpec(JsonObject oas) {
        try {
            File file = new File(filePath);
            FileWriter fr = new FileWriter(file);
            fr.write(oas.toString());
            fr.close();
            System.out.println("Saved OAS to file " + filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
