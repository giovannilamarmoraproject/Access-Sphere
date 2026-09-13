package io.github.giovannilamarmora.accesssphere.settings;

import io.github.giovannilamarmora.accesssphere.settings.entity.AppSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAppSettingsDAO extends JpaRepository<AppSettingsEntity, Long> {
}
