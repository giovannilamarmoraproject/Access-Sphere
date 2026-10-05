package io.github.giovannilamarmora.accesssphere.email.template;

import io.github.giovannilamarmora.accesssphere.email.template.database.EmailRepository;
import io.github.giovannilamarmora.accesssphere.email.template.model.EmailTemplate;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EmailTemplateDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  private static final String EMAIL_DOMAIN_LOG = "Email Domain";

  @Autowired private EmailRepository emailRepository;

  public Mono<List<EmailTemplate>> getAllTemplates() {
    LOG.info("📋 {} Retrieving all email template from Database", EMAIL_DOMAIN_LOG);

    List<EmailTemplate> models = emailRepository.findAll().orElse(List.of());

    LOG.info("📋 {} Found {} clients into the Database", EMAIL_DOMAIN_LOG, models.size());
    return Mono.just(models);
  }
}
