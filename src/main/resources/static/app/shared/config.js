const appVersion = "v1.1.18";

function getConfig() {
  const configClientID = "ACCESS-SPHERE-TECH";
  const configRedirectUri = window.location.origin + "/app/users";

  const urlConfig = {
    baseUrl: window.location.origin,
    authorize: "/v1/oAuth/2.0/authorize",
    users: "/v1/users",
    logout: "/v1/oAuth/2.0/logout",
    client: "/v1/clients",
    register: "/v1/users/register",
    edit: "/v1/users/update",
    delete: "/v1/users",
    forgot: "/v1/users/change/password/request",
    reset: "/v1/users/change/password",
    param: "?",
    divider: "&",
    access_type: "access_type=online",
    client_id: "client_id=" + configClientID,
    redirect_uri: "redirect_uri=" + configRedirectUri,
    scope: "scope=openid",
    login_type_bearer: "type=bearer",
    login_type_google: "type=google",
    response_type: "response_type=token",
  };

  return {
    client_id: configClientID,
    redirect_uri: configRedirectUri,
    access_token: "access-token",
    strapi_token: "strapi-token",
    login_url: window.location.origin + "/app/login",
    authorize_url:
      urlConfig.baseUrl +
      urlConfig.authorize +
      urlConfig.param +
      urlConfig.client_id +
      urlConfig.divider +
      urlConfig.access_type +
      urlConfig.divider +
      urlConfig.redirect_uri +
      urlConfig.divider +
      urlConfig.scope +
      urlConfig.divider +
      urlConfig.response_type,
    users_url: urlConfig.baseUrl + urlConfig.users,
    logout_url:
      urlConfig.baseUrl +
      urlConfig.logout +
      urlConfig.param +
      urlConfig.client_id,
    client_id_url: urlConfig.baseUrl + urlConfig.client,
    register_user_url: urlConfig.baseUrl + urlConfig.register,
    forgot_password_url: urlConfig.baseUrl + urlConfig.forgot,
    reset_password_url: urlConfig.baseUrl + urlConfig.reset,
    edit_user_url: urlConfig.baseUrl + urlConfig.edit,
    delete_user_url: urlConfig.baseUrl + urlConfig.delete,
  };
}

function getVersion() {
  return sweetalert("info", "Access Sphere - " + appVersion);
}
