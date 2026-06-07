package com.page.algorithm;

import com.page.model.SimulationResult;
import com.page.simulator.Simulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * EvictCount vs LRU 성능 비교 테스트
 * 다양한 참조 패턴에서 Page Fault 수를 직접 비교한다.
 */
class EvictCountVsLRUTest {

    // ── 공통 유틸 ────────────────────────────────────────────────────────────

    private record Result(int faults, double faultRate) {}

    private Result run(PageReplacementAlgorithm algo, int[] ref) {
        SimulationResult result = new Simulator(algo).run(ref);
        return new Result(result.getPageFaultCount(), result.getPageFaultRate());
    }

    private void compare(String scenario, int frames, int[] ref) {
        Result lru    = run(new LRUAlgorithm(frames), ref);
        Result evict  = run(new EvictCountAlgorithm(frames), ref);
        Result fifo   = run(new FIFOAlgorithm(frames), ref);

        String winner;
        if (evict.faults() < lru.faults())       winner = "✅ EvictCount WIN";
        else if (evict.faults() == lru.faults())  winner = "🟡 DRAW";
        else                                       winner = "❌ LRU WIN";

        System.out.println("┌─────────────────────────────────────────────────┐");
        System.out.printf ("│ 시나리오: %-38s│%n", scenario);
        System.out.printf ("│ 프레임=%d  참조열길이=%d%-27s│%n", frames, ref.length, "");
        System.out.println("├──────────────┬───────────┬──────────────────────┤");
        System.out.println("│   알고리즘   │  Fault 수 │      Fault Rate      │");
        System.out.println("├──────────────┼───────────┼──────────────────────┤");
        System.out.printf ("│ %-12s │     %3d   │      %5.1f%%          │%n", "LRU",           lru.faults(), lru.faultRate());
        System.out.printf ("│ %-12s │     %3d   │      %5.1f%%          │%n", "EvictCount",    evict.faults(), evict.faultRate());
        System.out.printf ("│ %-12s │     %3d   │      %5.1f%%          │%n", "FIFO",          fifo.faults(), fifo.faultRate());
        System.out.println("├──────────────┴───────────┴──────────────────────┤");
        System.out.printf ("│  결과: %-42s│%n", winner);
        System.out.println("└─────────────────────────────────────────────────┘");
        System.out.println();
    }

    // ── 테스트 케이스 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. 워킹셋 + 일시적 침입자 패턴 (EvictCount 유리)")
    void workingSetWithIntruders() {
        // 핵심 페이지 1,2,3 / 침입자 4,5,6 이 간간이 끼어듦
        int[] ref = {1, 2, 3, 4, 1, 2, 3, 5, 1, 2, 3, 6, 1, 2, 3};
        compare("워킹셋(1,2,3) + 침입자(4,5,6)", 3, ref);
    }

    @Test
    @DisplayName("2. 침입자가 반복 등장하는 패턴 (EvictCount 유리)")
    void repeatedIntruder() {
        // 4가 반복 등장하며 1,2,3을 밀어내려 함
        int[] ref = {1, 2, 3, 4, 1, 4, 2, 4, 3, 4, 1, 2, 3, 1, 2, 3};
        compare("침입자(4) 반복 등장", 3, ref);
    }

    @Test
    @DisplayName("3. 순환 참조 패턴 (EvictCount 불리 예상)")
    void cyclicPattern() {
        // 모든 페이지가 균등하게 순환 → EvictCount 장점 없음
        int[] ref = {1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4};
        compare("순환 참조 (1→2→3→4 반복)", 3, ref);
    }

    @Test
    @DisplayName("4. 국부성(Locality) 강한 패턴 (LRU 유리 예상)")
    void strongLocality() {
        // 최근에 쓴 페이지가 계속 쓰이는 패턴 → LRU 최적
        int[] ref = {1, 2, 1, 2, 1, 2, 3, 4, 3, 4, 3, 4, 1, 2, 1, 2};
        compare("강한 지역성 (최근 페이지 반복)", 3, ref);
    }

    @Test
    @DisplayName("5. 워킹셋이 프레임보다 조금 큰 패턴 (핵심 시나리오)")
    void workingSetSlightlyLargerThanFrames() {
        // 워킹셋={1,2,3,4}, 프레임=3 → 한 페이지는 항상 밖에 있음
        // 이력이 쌓일수록 EvictCount가 더 현명한 교체를 함
        int[] ref = {1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 4, 3, 1, 4, 2, 3, 1, 2, 3, 4};
        compare("워킹셋(4개) > 프레임(3개)", 3, ref);
    }

    @Test
    @DisplayName("6. 장기 시뮬레이션 — 이력 누적 효과 확인")
    void longRunSimulation() {
        // 긴 참조열: 초반에는 비슷하다가 중반 이후 EvictCount 이력 효과 발동
        int[] ref = {
            1, 2, 3, 4, 1, 2, 5, 1, 2, 3,
            6, 1, 2, 3, 7, 1, 2, 3, 4, 1,
            2, 3, 5, 1, 2, 3, 6, 1, 2, 3,
            1, 2, 3, 1, 2, 3, 1, 2, 3, 1
        };
        compare("장기 시뮬레이션 (40회 참조)", 3, ref);
    }

    @Test
    @DisplayName("7. 전체 요약 출력")
    void summary() {
        int frames = 3;
        List<int[]> testCases = List.of(
            new int[]{1, 2, 3, 4, 1, 2, 3, 5, 1, 2, 3, 6, 1, 2, 3},
            new int[]{1, 2, 3, 4, 1, 4, 2, 4, 3, 4, 1, 2, 3, 1, 2, 3},
            new int[]{1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4},
            new int[]{1, 2, 1, 2, 1, 2, 3, 4, 3, 4, 3, 4, 1, 2, 1, 2},
            new int[]{1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 4, 3, 1, 4, 2, 3, 1, 2, 3, 4},
            new int[]{1,2,3,4,1,2,5,1,2,3,6,1,2,3,7,1,2,3,4,1,2,3,5,1,2,3,6,1,2,3,1,2,3,1,2,3,1,2,3,1}
        );
        List<String> names = List.of(
            "워킹셋+침입자", "침입자반복", "순환참조", "강한지역성", "워킹셋>프레임", "장기시뮬"
        );

        int evictWin = 0, draw = 0, lruWin = 0;

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║          EvictCount vs LRU 전체 결과 요약           ║");
        System.out.println("╠══════════════════════╦══════════╦══════════╦═════════╣");
        System.out.println("║       시나리오       ║   LRU   ║EvictCnt ║  결과   ║");
        System.out.println("╠══════════════════════╬══════════╬══════════╬═════════╣");

        for (int i = 0; i < testCases.size(); i++) {
            Result lru   = run(new LRUAlgorithm(frames),         testCases.get(i));
            Result evict = run(new EvictCountAlgorithm(frames),  testCases.get(i));
            String res;
            if (evict.faults() < lru.faults())      { res = "EC승 ✅"; evictWin++; }
            else if (evict.faults() == lru.faults()) { res = "무승부🟡"; draw++;    }
            else                                      { res = "LRU승❌"; lruWin++;  }
            System.out.printf("║ %-20s ║  %3d F  ║  %3d F  ║ %-7s ║%n",
                    names.get(i), lru.faults(), evict.faults(), res);
        }

        System.out.println("╠══════════════════════╩══════════╩══════════╩═════════╣");
        System.out.printf ("║  EvictCount 승: %d  무승부: %d  LRU 승: %d%-12s║%n",
                evictWin, draw, lruWin, "");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}
