package by.istin.android.xcore.oauth2;

public interface OAuth2Helper {

    String getUrl() throws Exception;

    Credentials processUrl(String url) throws Exception;

    void sign(OAuth2Request httpUriRequest) throws Exception;

    boolean isExpired(Credentials credentials) throws Exception;

    boolean isRefreshTokenExpired(Credentials credentials) throws Exception;

    Credentials getCredentials();

    boolean isLogged();

    public static class Impl {

        public static OAuth2Helper create(Configuration configuration){
            return new DefaultOAuth2Helper(configuration);
        };

    }
}