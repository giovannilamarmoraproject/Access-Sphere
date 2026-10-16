package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.EmailUtils;
import io.github.giovannilamarmora.accesssphere.email.sender.EmailSenderService;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailContent;
import io.github.giovannilamarmora.accesssphere.email.sender.dto.EmailResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailSendTestRequest;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateDataResponse;
import io.github.giovannilamarmora.accesssphere.email.template.dto.EmailTemplateRequest;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.roles.PermissionRoleManager;
import io.github.giovannilamarmora.accesssphere.roles.RoleUtils;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.SystemPermission;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Application Service per la gestione dei Template Email.
 *
 * <p>Responsabilita:
 *
 * <ul>
 *   <li>Funge da Gatekeeper di sicurezza e scoping multi-tenant per gli endpoint REST tramite
 *       {@link PermissionRoleManager}.
 *   <li>Delega le operazioni di dominio e persistenza a {@link EmailTemplateDomainService}.
 *   <li>Coordina l'invio di email di test tramite {@link EmailSenderService}.
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class EmailTemplateService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private EmailTemplateDomainService emailTemplateDomainService;
  @Autowired private EmailSenderService emailSenderService;
  @Autowired private PermissionRoleManager permissionRoleManager;
  @Autowired private OAuth2Authorization oAuth2Authorization;

  /**
   * Recupera la lista dei Template Email autorizzati per l'operatore chiamante.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<List<EmailTemplateDataResponse>> getAllTemplates() {
    SystemPermission emailPermission = SystemPermission.EMAILS_MANAGE;
    List<RoleDefinition> userRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(emailPermission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(emailPermission);
    Set<String> authorizedClientIds = RoleUtils.extractAuthorizedClientIds(userRoles);

    LOG.info("📧 Retrieving all authorized email templates");

    return emailTemplateDomainService
        .getAllTemplates()
        .map(
            allTemplates -> {
              if (isAdmin) return allTemplates;
              return allTemplates.stream()
                  .filter(
                      t ->
                          EmailUtils.hasClientAccess(
                              t.getClientId(), authorizedClientIds, false))
                  .toList();
            })
        .map(EmailTemplateMapper::toResponseList);
  }

  /**
   * Risolve un template email in base a Categoria, Locale e Client (delegando al Domain Service).
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateDataResponse> getTemplateByCategoryLocaleAndClient(
      String category, String locale, String clientId) {
    return emailTemplateDomainService
        .getTemplateByCategoryLocaleAndClient(category, locale, clientId)
        .map(EmailTemplateMapper::toResponse);
  }

  /**
   * Registra un nuovo Template Email verificando i permessi e il perimetro client dell'operatore.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateDataResponse> saveTemplate(EmailTemplateRequest request) {
    checkManagePermissionForClient(request.getClientId(), "create");
    LOG.info("📧 Creating new email template: {}", request.getIdentifier());

    EmailTemplate model = EmailTemplateMapper.toModel(request);
    return emailTemplateDomainService
        .saveTemplate(model)
        .map(EmailTemplateMapper::toResponse);
  }

  /**
   * Aggiorna un Template Email esistente verificando i permessi e il perimetro client
   * dell'operatore.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailTemplateDataResponse> updateTemplate(
      String identifier, EmailTemplateRequest request) {
    checkManagePermissionForClient(request.getClientId(), "update");
    LOG.info("📧 Updating email template by identifier: {}", identifier);

    EmailTemplate model = EmailTemplateMapper.toModel(request);
    return emailTemplateDomainService
        .updateTemplate(identifier, model)
        .map(EmailTemplateMapper::toResponse);
  }

  /**
   * Elimina un Template Email verificando che l'operatore disponga dei permessi sul client del
   * template.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<Void> deleteTemplate(String identifier) {
    SystemPermission emailPermission = SystemPermission.EMAILS_MANAGE;
    List<RoleDefinition> userRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(emailPermission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(emailPermission);
    Set<String> authorizedClientIds = RoleUtils.extractAuthorizedClientIds(userRoles);

    LOG.info("📧 Deleting email template: {}", identifier);

    return emailTemplateDomainService
        .getTemplateByIdentifier(identifier)
        .doOnNext(
            existing -> {
              if (!EmailUtils.hasClientAccess(
                  existing.getClientId(), authorizedClientIds, isAdmin)) {
                LOG.error(
                    "⛔ Access denied: missing permission [{}] on delete template [{}] for client [{}]",
                    emailPermission,
                    identifier,
                    existing.getClientId());
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
              }
              emailTemplateDomainService.deleteTemplate(identifier);
            })
        .then();
  }

  /**
   * Compila e invia un'email di test verificando i permessi dell'operatore.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<EmailResponse> sendTestEmail(EmailSendTestRequest request) {
    permissionRoleManager.checkPermissionAndReturnUserRoles(SystemPermission.EMAILS_MANAGE);
    EmailTemplateValidator.validateTestRequest(request);

    LOG.info("📧 Preparing test email dispatch to: {}", request.getTo());

    return resolveTestEmailContent(request).flatMap(emailSenderService::sendEmail);
  }

  private Mono<EmailContent> resolveTestEmailContent(EmailSendTestRequest request) {
    if (!ObjectToolkit.isNullOrEmpty(request.getCustomHtml())) {
      String subject =
          !ObjectToolkit.isNullOrEmpty(request.getSubject())
              ? request.getSubject()
              : "Test Email - Access Sphere";
      String renderedHtml =
          EmailUtils.replacePlaceholders(request.getCustomHtml(), request.getParams());
      return Mono.just(buildEmailContent(request.getTo(), subject, renderedHtml));
    }

    Mono<EmailTemplate> templateMono =
        !ObjectToolkit.isNullOrEmpty(request.getIdentifier())
            ? emailTemplateDomainService.getTemplateByIdentifier(request.getIdentifier())
            : emailTemplateDomainService.getTemplateById(request.getTemplateId());

    return templateMono.map(
        template -> {
          String subject =
              !ObjectToolkit.isNullOrEmpty(request.getSubject())
                  ? request.getSubject()
                  : template.getSubject();
          String renderedHtml =
              EmailUtils.renderTemplate(
                  template.getTemplate(), template.getParams(), request.getParams());
          return buildEmailContent(request.getTo(), subject, renderedHtml);
        });
  }

  private EmailContent buildEmailContent(String to, String subject, String text) {
    return EmailContent.builder()
        .to(to)
        .subject(subject)
        .text(text)
        .sentDate(new Date())
        .build();
  }

  private void checkManagePermissionForClient(String targetClientId, String action) {
    SystemPermission emailPermission = SystemPermission.EMAILS_MANAGE;
    List<RoleDefinition> userRoles =
        permissionRoleManager.checkPermissionAndReturnUserRoles(emailPermission);
    boolean isAdmin = permissionRoleManager.hasAdminPermission(emailPermission);
    Set<String> authorizedClientIds = RoleUtils.extractAuthorizedClientIds(userRoles);

    if (!EmailUtils.hasClientAccess(targetClientId, authorizedClientIds, isAdmin)) {
      LOG.error(
          "⛔ Access denied: missing permission [{}] on {} email template for client [{}] and identifier [{}]",
          emailPermission,
          action,
          targetClientId,
          oAuth2Authorization.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
  }
}
