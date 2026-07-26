package com.knowledgehub.system.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.shared.config.SystemProperties;
import com.knowledgehub.system.domain.SystemInfo;
import com.knowledgehub.system.domain.port.SystemSettings;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;

class SystemInfoServiceTests {

  @SuppressWarnings("unchecked")
  private static ObjectProvider<BuildProperties> noBuildInfo() {
    ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    return provider;
  }

  @Test
  void reportsConfiguredValuesAndDefaultsVersionWhenNoBuildInfo() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("spring.application.name", "knowledge-hub");
    environment.setActiveProfiles("dev");
    SystemSettings settings = mock(SystemSettings.class);
    when(settings.productName()).thenReturn(Optional.empty());

    SystemInfoService service =
        new SystemInfoService(
            environment, noBuildInfo(), settings, new SystemProperties(null));

    SystemInfo info = service.currentInfo();

    assertThat(info.application()).isEqualTo("knowledge-hub");
    assertThat(info.activeProfiles()).containsExactly("dev");
    assertThat(info.version()).isEqualTo("unknown");
  }

  @Test
  void productNameFallsBackToApplicationWhenNeitherStoredNorConfigured() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("spring.application.name", "knowledge-hub");
    SystemSettings settings = mock(SystemSettings.class);
    when(settings.productName()).thenReturn(Optional.empty());

    SystemInfoService service =
        new SystemInfoService(
            environment, noBuildInfo(), settings, new SystemProperties(null));

    assertThat(service.currentInfo().productName()).isEqualTo("knowledge-hub");
  }

  @Test
  void productNameUsesConfiguredValueOverApplication() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("spring.application.name", "knowledge-hub");
    SystemSettings settings = mock(SystemSettings.class);
    when(settings.productName()).thenReturn(Optional.empty());

    SystemInfoService service =
        new SystemInfoService(
            environment, noBuildInfo(), settings, new SystemProperties("Knowledge Hub"));

    assertThat(service.currentInfo().productName()).isEqualTo("Knowledge Hub");
  }

  @Test
  void storedProductNameOverrideWinsOverConfig() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("spring.application.name", "knowledge-hub");
    SystemSettings settings = mock(SystemSettings.class);
    when(settings.productName()).thenReturn(Optional.of("Operator Override"));

    SystemInfoService service =
        new SystemInfoService(
            environment, noBuildInfo(), settings, new SystemProperties("Configured Name"));

    assertThat(service.currentInfo().productName()).isEqualTo("Operator Override");
  }
}
