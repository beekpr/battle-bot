package io.beekeeper.battleBot.utils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class SDKHelper {
    public static <T> Response<T> rawExec(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response;
            } else {
                int code = response.raw().code();
                String message = response.raw().message();
                throw new RuntimeException(String.format("%s %s", code, message));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T exec(Call<T> call) {
        return rawExec(call).body();
    }
}