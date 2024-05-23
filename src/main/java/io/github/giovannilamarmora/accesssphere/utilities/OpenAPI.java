package io.github.giovannilamarmora.accesssphere.utilities;

public final class OpenAPI {
  public final class Tag {
    public static final String OAUTH = "oAuth";
    public static final String TOKEN = "Token";
    public static final String USERS = "Users";
  }

  public final class Description {
    public static final String OAUTH = "API to handle authorization";
  }

  public final class Params {
    public final class Description {
      public static final String RESPONSE_TYPE =
          "Specifies the type of response desired from the authorization server.";
      public static final String ACCESS_TYPE =
          "Indicates the type of access requested. This can be either \"online\" or \"offline\". \"offline\" is used when a refresh token is needed.";
      public static final String CLIENT_ID =
          "The unique identifier of the client application making the request.";
      public static final String REDIRECT_URI =
          "The URI where the authorization server will send the user once the authorization process is complete.";
      public static final String SCOPE =
          "A space-delimited list of scopes that determine the level of access the client is requesting.";
      public static final String REGISTRATION_TOKEN =
          "(optional): A token used for client registration, if applicable.";
      public static final String STATE =
          "(optional): An opaque value used to maintain state between the request and callback. It is also used to prevent cross-site request forgery (CSRF) attacks.";
      public static final String GRANT_TYPE =
          "Specifies the type of grant being used. Common values are authorization_code, password, client_credentials, and refresh_token";
      public static final String CODE =
          "The authorization code that was previously obtained from the authorization server. This code is used to exchange for an access token.";
      public static final String PROMPT =
          "A parameter used to specify the prompt behavior. Common values include none, consent, and select_account";
      public static final String BASIC =
          "The credentials used to authenticate the client with the authorization server. This usually takes the form of an HTTP Basic Authentication header.";
      public static final String BEARER =
          "The credentials used to authenticate the client with the authorization server. This usually takes the form of an HTTP Bearer Authentication header.";
      public static final String INCLUDE_USER_INFO = "(optional): Include UserInfo Response.";
      public static final String INCLUDE_USER_DATA = "(optional): Include UserData Response.";
    }

    public final class Example {
      public static final String RESPONSE_TYPE = "code";
      public static final String ACCESS_TYPE = "online";
      public static final String CLIENT_ID = "CLIENT-ID-001";
      public static final String REDIRECT_URI = "http://localhost:8080/callback";
      public static final String SCOPE = "openid email profile";
      public static final String REGISTRATION_TOKEN = "abcdefg";
      public static final String STATE = "xyz123";
      public static final String GRANT_TYPE = "authorization_code";
      public static final String CODE = "abcdef";
      public static final String PROMPT = "consent";
      public static final String BASIC = "Basic dGVzdDpzZWNyZXQ=";
      public static final String BEARER = "Bearer dGVzdDpzZWNyZXQ.....";
      public static final String INCLUDE_USER_INFO = "true";
      public static final String INCLUDE_USER_DATA = "true";
    }
  }
}
