package by.istin.android.xcore.model;

import android.provider.BaseColumns;

import com.google.gson.annotations.SerializedName;

import by.istin.android.xcore.annotations.dbEntity;
import by.istin.android.xcore.annotations.dbLong;
import by.istin.android.xcore.annotations.dbString;

/**
 * Created by IstiN on 13.11.13.
 */
public class SimpleEntityWithSubEntity implements BaseColumns {

    @dbLong
    @SerializedName(value = "id")
    public static final String ID = _ID;

    @dbString
    @SerializedName(value = "title")
    public static final String TITLE = "title";

    @dbString
    @SerializedName(value = "about")
    public static final String ABOUT = "about";

    @dbString
    @SerializedName(value = "image_url")
    public static final String IMAGE_URL = "image_url";

    @dbEntity(clazz = SimpleEntityWithParent.class, contentValuesKey = "subEntity")
    @SerializedName(value = "sub_entity")
    public static final String SUB_ENTITY = "sub_entity";



}
