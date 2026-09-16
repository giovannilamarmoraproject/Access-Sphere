package io.github.giovannilamarmora.accesssphere.data;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.UserException;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.utils.generic.Response;
import java.util.List;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface UserDataService {
  /**
   * Retrieves a {@link User} by its unique identifier from the local database.
   *
   * @param identifier the unique identifier of the user (e.g., username or email)
   * @return a {@link Mono} emitting the found {@link User}, or empty if not found
   */
  Mono<User> getUserByIdentifier(String identifier);

  /**
   * Retrieves a {@link User} by their email from the local database.
   *
   * @param email the email address of the user
   * @return a {@link Mono} emitting the found {@link User}, or empty if not found
   */
  Mono<User> getUserByEmail(String email);

  /**
   * Retrieves a {@link User} based on the provided password reset token.
   *
   * <p>This method checks the validity of the provided reset token and, if valid, retrieves the
   * corresponding user from the system. Typically used in the password reset flow to identify the
   * user who requested the reset.
   *
   * @param tokenReset the password reset token associated with the user
   * @return a {@link Mono} emitting the {@link User} associated with the provided reset token, or
   *     empty if the token is invalid or no user is found
   */
  Mono<User> getUserByTokenReset(String tokenReset);

  /**
   * Authenticates a user and returns an OAuth token response upon successful login.
   *
   * <p>This method performs user authentication by validating the provided username (or email) and
   * password. If the authentication is successful, it generates an OAuth token response. The
   * provided {@link ClientCredential} is used for client authentication, and the {@link
   * ServerHttpRequest} is used to capture any relevant request-specific data for the authentication
   * process.
   *
   * @param username the username of the user attempting to log in
   * @param email the email of the user attempting to log in (used as an alternative to username)
   * @param password the password of the user attempting to log in
   * @param clientCredential the client credentials used for authenticating the client making the
   *     request
   * @param request the server HTTP request, which may include additional data or context for
   *     authentication
   * @return a {@link Mono} emitting an {@link OAuthTokenResponse} containing the generated OAuth
   *     tokens
   */
  Mono<OAuthTokenResponse> login(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request);

  /**
   * Retrieves the user information based on the provided JWT data and token.
   *
   * <p>This method uses the provided JWT data to validate the user and fetch their information,
   * either from a local database or an external system. The {@code token} is used for validation or
   * additional data required during the lookup process.
   *
   * @param jwtData the data extracted from the JWT, containing the user identity and other claims
   * @param token the token used for additional validation or context during the retrieval of user
   *     info
   * @return a {@link Mono} emitting the {@link User} containing the retrieved user information or
   *     empty if no user is found or the token is invalid
   */
  Mono<User> getUserInfo(JWTData jwtData, String token);

  /**
   * Registers a new user in the system, optionally assigning a new client.
   *
   * <p>This method registers a new user by saving their data in the system and, if specified,
   * assigns them a new client credential. The user's registration process may also require
   * validating the provided bearer token for authentication or authorization purposes.
   *
   * @param bearer the authorization token (bearer token) used to validate the request
   * @param user the user data to be registered, containing the required user fields
   * @param clientCredential the client credentials to be associated with the user, if applicable
   * @param assignNewClient flag indicating whether to assign a new client credential to the user
   * @return a {@link Mono} emitting the registered {@link User} after the registration process is
   *     complete
   */
  Mono<User> registerUser(
      String bearer, User user, ClientCredential clientCredential, Boolean assignNewClient);

  /**
   * Updates a user in the system without modifying the user's password.
   *
   * <p>This method delegates the user update process, including updating the user entity in the
   * database.
   *
   * @param userToUpdate the user data to be updated, containing the fields to be modified
   * @return a {@link Mono} emitting the updated {@link User}, after the update process is complete
   * @throws UserException if any error occurs during the update process
   */
  Mono<User> updateUser(User userToUpdate);

  /**
   * Updates a user in the system, with an option to update the user's password.
   *
   * <p>This method delegates the user update process, including updating the user entity in the
   * database and optionally updating the user's password based on the {@code isUpdatePassword}
   * flag.
   *
   * @param userToUpdate the user data to be updated, containing the fields to be modified
   * @param isUpdatePassword flag indicating whether the user's password should be updated
   * @return a {@link Mono} emitting the updated {@link User}, after the update process is complete
   * @throws UserException if the password format is invalid or any other update error occurs
   */
  Mono<User> updateUser(User userToUpdate, boolean isUpdatePassword);

  /**
   * Deletes a user from the system based on the provided identifier.
   *
   * <p>This method removes the user from the local database. The {@code identifier} specifies which user to
   * delete.
   *
   * @param identifier the unique identifier of the user to be deleted (e.g., username, email, or
   *     ID)
   * @return a {@link Mono} emitting a {@link Response} indicating the outcome of the delete
   *     operation
   */
  Mono<Response> deleteUser(
      String identifier, String bearer, ServerWebExchange exchange);

  /**
   * Refreshes the JWT token using the provided access token data and client credentials.
   *
   * <p>This method validates the provided {@code token} and, if valid, generates a new JWT token.
   * The {@code accessTokenData} is used for additional context regarding the current session, while
   * the {@code clientCredential} is used for client authentication. The {@code request} is captured
   * for any additional request-specific data that may be needed during the process.
   *
   * @param accessTokenData the data associated with the current access token, used for session
   *     validation
   * @param clientCredential the client credentials used for authenticating the request
   * @param token the current JWT token to be refreshed
   * @param request the HTTP request, potentially containing additional information relevant to the
   *     refresh process
   * @return a {@link Mono} emitting an {@link OAuthTokenResponse} containing the newly generated
   *     JWT token
   */
  Mono<OAuthTokenResponse> refreshJWTToken(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      String token,
      ServerHttpRequest request);

  /**
   * Logs out the user by invalidating the provided refresh token.
   *
   * <p>This method performs any necessary cleanup related to the user's session, such as revoking
   * tokens or notifying external identity providers.
   *
   * @param accessTokenData additional access token data related to the user session, used for
   *     validation or auditing
   * @return a {@link Mono} signaling completion when the logout process is done
   */
  Mono<Void> logout(AccessTokenData accessTokenData);

  /**
   * Retrieves a list of all users from the local database.
   *
   * <p>This method queries the database for all registered users and returns them as a list. The
   * returned list is wrapped in a {@link Mono} to support reactive programming.
   *
   * @return a {@link Mono} emitting a list of {@link User} objects retrieved from the database or
   *     an empty list if no users are found
   */
  Mono<List<User>> getUsersFromDatabase();

  /**
   * Saves a user into the local database.
   *
   * <p>This method persists the provided {@link User} object into the local database. If the user
   * already exists, it may update the existing user record; otherwise, a new record will be
   * created.
   *
   * @param user the user object containing the data to be saved in the database
   */
  void saveUserIntoDatabase(User user);

  /**
   * Updates an existing user in the local database.
   *
   * <p>This method updates the provided {@link User} object in the database. The user must already
   * exist in the database, and the method will overwrite the existing record with the updated
   * information from the provided user object.
   *
   * @param user the user object containing the updated data to be saved in the database
   */
  void updateUserIntoDatabase(User user);

  /**
   * Deletes a user from the local database.
   *
   * <p>This method removes the provided {@link User} object from the database. The user must
   * already exist in the database, and the record will be permanently deleted.
   *
   * @param user the user object to be deleted from the database
   */
  void deleteUserFromDatabase(User user);
}
