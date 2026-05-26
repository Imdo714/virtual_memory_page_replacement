package com.page.algorithm;

import com.page.model.SimulationStep;

import java.util.*;

/**
 * ════════════════════════════════════════════════════════════════
 *  Evict-Count Based 커스텀 페이지 교체 알고리즘
 * ════════════════════════════════════════════════════════════════
 *
 * [아이디어]
 *   "한 번 버려졌다 다시 불린 페이지는 자주 필요한 페이지다."
 *   퇴출(evict)된 횟수를 이력 테이블에 누적하여 중요도 지표로 활용한다.
 *   evict count가 높을수록 = 자주 필요했던 페이지 = 프레임에서 보호
 *   evict count가 낮을수록 = 덜 중요한 페이지  = 교체 우선 대상
 *
 * [교체 방식]
 *   1. Page Hit  : 해당 페이지를 frames의 맨 뒤로 재삽입
 *                  → FIFO 타이브레이킹 시 가장 늦게 쫓겨나도록 수명 연장
 *   2. Page Fault: frames에서 evict count가 가장 낮은 페이지를 victim으로 선택
 *                  → 동점(evict count 동일)이면 삽입 순서(FIFO)로 결정
 *                  → 퇴출된 페이지의 evict count +1 누적
 *
 * [자료구조]
 *   LinkedHashSet<Integer> frames
 *     - 삽입 순서 유지  : FIFO 타이브레이킹에 활용 (순회 시 오래된 순으로 등장)
 *     - O(1) contains  : 히트 여부 즉시 판별
 *     - O(1) remove    : victim 제거 시 Queue처럼 O(n) 탐색 불필요
 *     → Queue(순서) + HashSet(조회) 두 자료구조의 역할을 하나로 통합
 *
 *   Map<Integer, Integer> evictCounts
 *     - key  : 페이지 번호
 *     - value: 누적 퇴출 횟수 (시뮬레이션 전체에 걸쳐 유지)
 *
 * [시간 복잡도]
 *   accessPage : O(1) — contains, remove, add 모두 O(1)
 *   findVictim : O(n) — 프레임 전체를 1-pass로 순회 (n = frameCount)
 * ════════════════════════════════════════════════════════════════
 */
public class EvictCountAlgorithm implements PageReplacementAlgorithm {

    private final int frameCount;
    private final LinkedHashSet<Integer> frames; // 삽입 순서 유지 + O(1) 조회/삭제
    private final Map<Integer, Integer> evictCounts; // 페이지별 퇴출 횟수 이력 테이블

    public EvictCountAlgorithm(int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("Frame count must be positive.");
        this.frameCount  = frameCount;
        this.frames      = new LinkedHashSet<>();
        this.evictCounts = new HashMap<>();
    }

    @Override
    public SimulationStep accessPage(int pageNumber) {
        // ── Page Hit ──────────────────────────────────────────────────────────
        if (frames.contains(pageNumber)) {
            // 맨 뒤로 재삽입 → FIFO 타이브레이킹 시 수명 연장
            frames.remove(pageNumber);
            frames.add(pageNumber);
            return new SimulationStep(pageNumber, false, new ArrayList<>(frames), null);
        }

        // ── Page Fault ────────────────────────────────────────────────────────
        Integer evicted = null;
        if (frames.size() == frameCount) {
            evicted = findVictim();
            frames.remove(evicted);                      // O(1)
            evictCounts.merge(evicted, 1, Integer::sum); // evict count +1
        }

        frames.add(pageNumber);

        return new SimulationStep(pageNumber, true, new ArrayList<>(frames), evicted);
    }

    /**
     * Victim 선정 (1-pass O(n))
     *
     * LinkedHashSet은 삽입 순으로 순회되므로
     * 첫 번째로 발견된 minCount 페이지 = 가장 오래된 페이지
     * → 별도의 2-pass 없이 FIFO 타이브레이킹이 자동으로 충족된다.
     *
     * 예) frames = [2, 3, 4], evictCounts = {2:1, 3:0, 4:0}
     *     순회: 2(count=1) → 3(count=0, 새 min) → 4(count=0, 동점이지만 3이 먼저)
     *     → victim = 3
     */
    private Integer findVictim() {
        Integer victim   = null;
        int minCount = Integer.MAX_VALUE;

        for (Integer page : frames) {
            int count = evictCounts.getOrDefault(page, 0);
            if (count < minCount) {
                minCount = count;
                victim   = page;
            }
        }
        return victim;
    }

    @Override
    public void reset() {
        frames.clear();
        evictCounts.clear();
    }

    @Override
    public String getName()    { return "EvictCount"; }

    @Override
    public int getFrameCount() { return frameCount; }

    /** 최종 퇴출 횟수 이력 테이블 반환 (출력용) */
    public Map<Integer, Integer> getEvictCounts() {
        return Collections.unmodifiableMap(evictCounts);
    }
}
