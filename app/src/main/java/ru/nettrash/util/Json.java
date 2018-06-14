package ru.nettrash.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by nettrash on 16.03.2018.
 */

public final class Json {

    public static <T> HashMap<String, T> jsonToMap(JSONObject json) throws JSONException {
        HashMap<String, T> retMap = new HashMap<String, T>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static <T> HashMap<String, T> toMap(JSONObject object) throws JSONException {
        HashMap<String, T> map = new HashMap<String, T>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            T value = (T)object.get(key);
            map.put(key, value);
        }
        return map;
    }
}
