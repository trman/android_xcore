package by.istin.android.xcore.annotations.converter.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;

import by.istin.android.xcore.annotations.converter.IConverter;

/**
 * GsonConverter provides Meta for gson elements parsing.
 */
public abstract class GsonConverter implements IConverter<GsonConverter.Meta> {

    /**
     * Meta provides basic information for parsing Gson elements. Used in the AbstractValuesAdapter.
     */
    public static class Meta {

        /**
         * Current jsonElement
         */
        private JsonElement jsonElement;

        /**
         * Current reflection type
         */
        private Type type;

        /**
         * Curreng gson context
         */
        private JsonDeserializationContext jsonDeserializationContext;

        /**
         * Default constructor
         *
         * @param jsonElement current jsonElement
         * @param type current reflection type
         * @param jsonDeserializationContext current gson context
         */
        public Meta(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            this.jsonElement = jsonElement;
            this.type = type;
            this.jsonDeserializationContext = jsonDeserializationContext;
        }

        /**
         * Returns JsonElement
         * @return jsonElement
         */
        public JsonElement getJsonElement() {
            return jsonElement;
        }

        /**
         * Return reflection type of field
         * @return reflection type
         */
        public Type getType() {
            return type;
        }

        /**
         * Return gson context
         * @return gson context
         */
        public JsonDeserializationContext getJsonDeserializationContext() {
            return jsonDeserializationContext;
        }
    }

}