package com.page.algorithm;

import com.page.model.SimulationStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NURAlgorithmTests {

    @Test
    void evictsOldestUnmodifiedPageWhenAllPagesWereReferenced() {
        NURAlgorithm algorithm = new NURAlgorithm(3);

        algorithm.accessPage(1);
        algorithm.accessPage(2);
        algorithm.accessPage(3);
        SimulationStep step = algorithm.accessPage(4);

        assertThat(step.isPageFault()).isTrue();
        assertThat(step.getEvictedPage()).isEqualTo(1);
        assertThat(step.getFrameSnapshot()).containsExactly(2, 3, 4);
    }

    @Test
    void prefersUnreferencedUnmodifiedPageOverUnreferencedModifiedPage() {
        NURAlgorithm algorithm = new NURAlgorithm(3);

        algorithm.accessPage(1, true);  // Class 3: R = 1, M = 1
        algorithm.accessPage(2);        // Class 2: R = 1, M = 0
        algorithm.accessPage(3, true);  // Class 3: R = 1, M = 1
        algorithm.clearReferenceBits(); // 1,3은 Class 1 / 2는 Class 0
        SimulationStep step = algorithm.accessPage(4);

        assertThat(step.isPageFault()).isTrue();
        assertThat(step.getEvictedPage()).isEqualTo(2);
        assertThat(step.getFrameSnapshot()).containsExactly(1, 3, 4);
    }

    @Test
    void evictsUnreferencedModifiedPageBeforeReferencedPages() {
        NURAlgorithm algorithm = new NURAlgorithm(3);

        algorithm.accessPage(1, true);  // Class 3: R = 1, M = 1
        algorithm.accessPage(2, true);  // Class 3: R = 1, M = 1
        algorithm.accessPage(3);        // Class 2: R = 1, M = 0
        algorithm.clearReferenceBits(); // 1,2는 Class 1 / 3은 Class 0
        algorithm.accessPage(3);        // 3만 다시 Class 2로 변경
        SimulationStep step = algorithm.accessPage(4);

        assertThat(step.isPageFault()).isTrue();
        assertThat(step.getEvictedPage()).isEqualTo(1);
        assertThat(step.getFrameSnapshot()).containsExactly(2, 3, 4);
    }
}
