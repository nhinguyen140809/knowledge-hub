package com.knowledgehub.system.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.system.domain.SystemInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;

class SystemInfoServiceTests {

  @Test
  void reportsConfiguredValuesAndDefaultsVersionWhenNoBuildInfo() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("spring.application.name", "knowledge-hub");
    environment.setActiveProfiles("dev");

    @SuppressWarnings("unchecked")
    ObjectProvider<BuildProperties> noBuildInfo = mock(ObjectProvider.class);
    when(noBuildInfo.getIfAvailable()).thenReturn(null);

    SystemInfoService service = new SystemInfoService(environment, noBuildInfo);

    SystemInfo info = service.currentInfo();

    assertThat(info.application()).isEqualTo("knowledge-hub");
    assertThat(info.activeProfiles()).containsExactly("dev");
    assertThat(info.version()).isEqualTo("unknown");
  }
}
