package by.istin.android.xcore.utils;

import java.util.ArrayList;
import java.util.List;

public class UrlBuilder {

    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    public static final String AMP = "&";
    public static final String Q = "?";

    private class ParamValue {

        private ParamValue(String param, String value, boolean autoEncode) {
            this.param = param;
            this.value = value;
            this.autoEncode = autoEncode;
        }

        private String param;

        private String value;

        private boolean autoEncode = true;

    }

    private volatile Object mLock = new Object();

    private StringBuffer mUrlStringBuilder;

    private String mPath;

    private List<ParamValue> mListParams = new ArrayList<ParamValue>();

    private List<ParamValue> mListParamsUnknown = new ArrayList<ParamValue>();

    private UrlBuilder(String path) {
        mPath = path;
        mUrlStringBuilder = new StringBuffer(mPath);
    }

    public static UrlBuilder http(String host) {
        return new UrlBuilder(HTTP +host);
    }

    public static UrlBuilder parent(UrlBuilder urlBuilder) {
        UrlBuilder builder = new UrlBuilder(urlBuilder.mPath);
        builder.mListParams.addAll(urlBuilder.mListParams);
        builder.mListParamsUnknown.addAll(urlBuilder.mListParamsUnknown);
        return builder;
    }

    public static UrlBuilder https(String host) {
        return new UrlBuilder(HTTPS +host);
    }

    public UrlBuilder path(String path) {
        mPath+=path;
        synchronized (mLock) {
            mUrlStringBuilder.append(path);
        }
        return this;
    }

    public UrlBuilder s(String value) {
        path("/" + value);
        return this;
    }

    public UrlBuilder param(String param) {
        mListParamsUnknown.add(new ParamValue(param, StringUtil.EMPTY, false));
        return this;
    }

    public UrlBuilder param(String param, String value) {
        return param(param, value, true);
    }

    private String format(String ... params){
        String result = mUrlStringBuilder.toString();
        try {
            if (params == null) {
                return result;
            }
            return StringUtil.format(result, params);
        } finally {
            mUrlStringBuilder.setLength(0);
            mUrlStringBuilder.append(mPath);
        }
    }

    public UrlBuilder param(String param, String value, boolean autoEncode) {
        mListParams.add(new ParamValue(param, value, autoEncode));
        return this;
    }

    private static String[] join(String[] args1, String[] args2) {
        if (args1 == null) {
            return args2;
        }
        if (args2 == null) {
            return args1;
        }
        String[] argsAll = new String[args1.length + args2.length];

        System.arraycopy(args1, 0, argsAll, 0, args1.length);
        System.arraycopy(args2, 0, argsAll, args1.length, args2.length);

        return argsAll;
    }

    public String build(String ... args) {
        synchronized (mLock) {
            String[] resultArgs = null;
            if (!mListParams.isEmpty() || !mListParamsUnknown.isEmpty()) {
                String[] argsKnown = new String[mListParams.size()];
                int i = 0;
                for (ParamValue paramValue : mListParams) {
                    appendPrefix();
                    mUrlStringBuilder.append(paramValue.param + "=%s");
                    argsKnown[i] = (paramValue.autoEncode ? StringUtil.encode(paramValue.value) : paramValue.value);
                    i++;
                }
                for (ParamValue paramValue : mListParamsUnknown) {
                    appendPrefix();

                    mUrlStringBuilder.append(paramValue.param + "=%s");
                }
                resultArgs = join(argsKnown, args);
            }
            return format(resultArgs);
        }
    }

    private void appendPrefix() {
        if (mUrlStringBuilder.indexOf(Q) > 0) {
            mUrlStringBuilder.append(AMP);
        } else {
            mUrlStringBuilder.append(Q);
        }
    }

}
