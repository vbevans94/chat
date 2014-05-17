package chat.app.manager.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

import java.lang.reflect.Type;

public class GsonUtils {

    private static final GsonBuilder mBuilder = new GsonBuilder();
    private static Gson mGson = mBuilder.create();

    /**
     * @return configured {@link Gson} instance.
     */
    public static Gson gson() {
        return mGson;
    }

    /**
     * Updates Gson processor to use given type adapter.
     * @param type to register type adapter for
     * @param typeAdapter to perform read/write conversions to and from JSON
     * @param <T> type of the class
     */
    public static <T> void registerTypeAdapter(Type type, TypeAdapter<T> typeAdapter) {
        mGson = mBuilder.registerTypeAdapter(type, typeAdapter).create();
    }
}