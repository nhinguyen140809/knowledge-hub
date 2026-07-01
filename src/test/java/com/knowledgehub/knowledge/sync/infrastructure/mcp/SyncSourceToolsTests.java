package com.knowledgehub.knowledge.sync.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.knowledge.sync.application.SyncService;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import com.knowledgehub.shared.error.ErrorCode;
import com.knowledgehub.shared.error.ToolFailure;
import org.junit.jupiter.api.Test;

class SyncSourceToolsTests {

  private final SyncService syncService = mock(SyncService.class);
  private final SyncSourceTools tools = new SyncSourceTools(syncService);

  @Test
  void delegatesToTheSyncServiceAndReturnsItsResult() {
    SyncResult result = SyncResult.noChange("s-1", 3, 12L, "abc123");
    when(syncService.sync("s-1")).thenReturn(result);

    assertThat(tools.syncSource("s-1")).isSameAs(result);
  }

  @Test
  void mapsAnUnknownSourceToAToolFailureCarryingTheSameCode() {
    when(syncService.sync("nope")).thenThrow(new SourceNotFoundException("nope"));

    assertThatThrownBy(() -> tools.syncSource("nope"))
        .isInstanceOf(ToolFailure.class)
        .hasMessageContaining("[" + ErrorCode.SOURCE_NOT_FOUND.name() + "]")
        .satisfies(
            e -> assertThat(((ToolFailure) e).errorCode()).isEqualTo(ErrorCode.SOURCE_NOT_FOUND));
  }
}
