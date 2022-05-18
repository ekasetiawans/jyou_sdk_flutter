package id.kakzaki.jyou_sdk.ecg;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by zhou0 on 18.1.2.
 */

public class ShareUtil {
    private static SharedPreferences sharedPreferences;
    public static void init(Context context){
        sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }
    
    public static void setValue(String key, Object object){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (object instanceof String)
        {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer)
        {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean)
        {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float)
        {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long)
        {
            editor.putLong(key, (Long) object);
        } else
        {
            editor.putString(key, object.toString());
        }
        editor.apply();
    }
    
    public static Object getValue(String key, Object defaultObject){
        if (defaultObject instanceof String)
        {
            return sharedPreferences.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer)
        {
            return sharedPreferences.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean)
        {
            return sharedPreferences.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float)
        {
            return sharedPreferences.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long)
        {
            return sharedPreferences.getLong(key, (Long) defaultObject);
        }
        return null;
    }

}
