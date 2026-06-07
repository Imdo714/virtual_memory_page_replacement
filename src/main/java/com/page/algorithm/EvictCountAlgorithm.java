package com.page.algorithm;

import com.page.model.SimulationStep;

import java.util.*;

/**
 * [아이디어]
 *   "한 번 버려졌다 다시 불린 페이지는 자주 필요한 페이지다."
 *   퇴출(evict)된 횟수를 이력 테이블에 누적하여 중요도 지표로 활용한다.
 *   evict count가 높을수록 = 자주 필요했던 페이지 = 프레임에서 보호
 *   evict count가 낮을수록 = 덜 중요한 페이지  = 교체 우선 대상
 *
 *   score 계산
 *      - accessCount(참조 횟수)를 복합 점수에 반영
 *      - 초반 evictCount = 0 구간에서 덜 참조된 페이지가 먼저 쫓겨남
 *      - score = evictCount * EVICT_WEIGHT + accessCount
 *
 *   순환 참조 취약 보완 — 시간 감쇠(Time Decay)
 *      - 마지막 퇴출 시점(tick)을 함께 기록
 *      - 오래된 evict 이력일수록 score 가중치 감소
 *      - decayedEvict = evictCount / (1.0 + age * DECAY_RATE)
 *
 *   Hit 시 재배치 제거 — LRU 타이브레이킹
 *      - score 동점이면 가장 오래전에 접근한 페이지를 victim 선택 (LRU 방식)
 *
 *   reset 모드 분리
 *      - reset()         : 프레임 + 이력 완전 초기화
 *      - resetFramesOnly(): 프레임만 초기화, 이력(evictHistory) 유지
 *
 * [Victim 선정 복합 점수]
 *   age           = tick - lastEvictedAt          (마지막 퇴출 이후 경과 tick)
 *   decayedEvict  = evictCount / (1.0 + age * DECAY_RATE)
 *   score         = decayedEvict * EVICT_WEIGHT + accessCount
 *   → score 최솟값 페이지가 victim
 *   → score 동점이면 lastAccessTime 오래된 페이지 (LRU 타이브레이킹)
 *
 * [자료구조]
 *   Set<Integer> frames
 *     - O(1) contains : 히트 여부 즉시 판별
 *     - O(1) remove   : victim 제거
 *
 *   Map<Integer, EvictRecord> evictHistory
 *     - key  : 페이지 번호
 *     - value: 누적 퇴출 횟수 + 마지막 퇴출 tick
 *
 *   Map<Integer, Integer> accessCount
 *     - key  : 페이지 번호
 *     - value: 누적 참조 횟수 (Cold Start 보완용)
 *
 *   Map<Integer, Integer> lastAccessTime
 *     - key  : 페이지 번호
 *     - value: 마지막 접근 tick (LRU 타이브레이킹용)
 *
 * [시간 복잡도]
 *   accessPage : O(1) — contains, remove, add 모두 O(1)
 *   findVictim : O(n) — 프레임 전체를 1-pass로 순회 (n = frameCount)
 *
 * [하이퍼파라미터]
 *   DECAY_RATE   : 시간 감쇠 강도 (클수록 오래된 이력 빠르게 소멸)
 *   EVICT_WEIGHT : evictCount vs accessCount 가중치 비율
 */
public class EvictCountAlgorithm implements PageReplacementAlgorithm {

    /** 시간 감쇠 강도: 값이 클수록 오래된 evict 이력의 영향력이 빠르게 줄어든다 */
    private static final double DECAY_RATE   = 0.05;
    /** evictCount 가중치: accessCount 대비 evict 신호를 얼마나 강하게 볼 것인가 */
    private static final double EVICT_WEIGHT = 2.0;

    private final int frameCount;
    private final Set<Integer>              frames;         // 현재 프레임 (O(1) 조회/삭제)
    private final Map<Integer, EvictRecord> evictHistory;   // 퇴출 횟수 + 마지막 퇴출 tick
    private final Map<Integer, Integer>     accessCount;    // 누적 참조 횟수 (Cold Start 보완)
    private final Map<Integer, Integer>     lastAccessTime; // 마지막 접근 tick (LRU 타이브레이킹)
    private int tick; // 논리 시계 (accessPage 호출마다 +1)

    /**
     * 페이지별 퇴출 이력 레코드
     *
     * @param count  누적 퇴출 횟수
     * @param lastEvictedAt 마지막으로 퇴출된 tick (시간 감쇠 계산에 사용)
     */
    public record EvictRecord(int count, int lastEvictedAt) {
        EvictRecord increment(int currentTick) {
            return new EvictRecord(count + 1, currentTick);
        }
    }

    public EvictCountAlgorithm(int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("Frame count must be positive.");
        this.frameCount      = frameCount;
        this.frames          = new HashSet<>();
        this.evictHistory    = new HashMap<>();
        this.accessCount     = new HashMap<>();
        this.lastAccessTime  = new HashMap<>();
        this.tick            = 0;
    }

    @Override
    public SimulationStep accessPage(int pageNumber) {
        tick++;

        // Page Hit
        if (frames.contains(pageNumber)) {
            // Hit 시 재삽입 제거 — lastAccessTime 갱신만으로 LRU 타이브레이킹 처리
            accessCount.merge(pageNumber, 1, Integer::sum);
            lastAccessTime.put(pageNumber, tick);
            return new SimulationStep(pageNumber, false, new ArrayList<>(frames), null);
        }

        // Page Fault
        Integer evicted = null;
        if (frames.size() == frameCount) {
            evicted = findVictim();
            frames.remove(evicted);

            if (evictHistory.containsKey(evicted)) {
                // 이미 쫓겨난 적 있으면 → increment()함수로 count++
                EvictRecord old = evictHistory.get(evicted);
                evictHistory.put(evicted, old.increment(tick));
            } else {
                // 처음 쫓겨났으면 count = 1로 객체 생성
                evictHistory.put(evicted, new EvictRecord(1, tick));
            }
        }

        frames.add(pageNumber);
        accessCount.merge(pageNumber, 1, Integer::sum);
        lastAccessTime.put(pageNumber, tick);

        return new SimulationStep(pageNumber, true, new ArrayList<>(frames), evicted);
    }

    /**
     * Victim 선정 (1-pass O(n))
     *
     * 복합 점수(score)가 가장 낮은 페이지를 victim으로 선택한다.
     *
     *   age          = tick - lastEvictedAt
     *   decayedEvict = evictCount / (1.0 + age * DECAY_RATE)   ← 시간 감쇠
     *   score        = decayedEvict * EVICT_WEIGHT + accessCount
     *
     * score 동점이면 lastAccessTime이 가장 오래된 페이지를 선택 (LRU 타이브레이킹)
     */
    private Integer findVictim() {
        Integer victim = null;
        double minScore = Double.MAX_VALUE;
        int minAccess = Integer.MAX_VALUE;

        for (Integer page : frames) {
            double score = computeScore(page);
            int accessed = lastAccessTime.getOrDefault(page, 0);

            if (score < minScore || (score == minScore && accessed < minAccess)) {
                minScore  = score;
                minAccess = accessed;
                victim    = page;
            }
        }
        return victim;
    }

    /**
     * 페이지의 중요도 복합 점수 계산
     *
     * 점수가 낮을수록 덜 중요한 페이지 → victim 우선 대상
     */
    private double computeScore(int page) {
        // 시간 감쇠가 적용된 evict 점수
        EvictRecord record = evictHistory.get(page);
        double decayedEvict;

        if (record == null) {
            decayedEvict = 0.0; // evict 이력 없음 → Cold Start 구간
        } else {
            int age = tick - record.lastEvictedAt(); // tick - 마지막으로 쫓겨난 시점
            decayedEvict = record.count() / (1.0 + age * DECAY_RATE); // 누적 퇴출 수 / (1.0 + age * 0.05)
        }

        // accessCount 가중치, 누적 참조 횟수 (Cold Start: 참조 적은 페이지가 낮은 점수)
        int accesses = accessCount.getOrDefault(page, 0);

        return decayedEvict * EVICT_WEIGHT + accesses;
    }

    /**
     * 완전 초기화: 프레임 + 모든 이력 리셋
     */
    @Override
    public void reset() {
        frames.clear();
        evictHistory.clear();
        accessCount.clear();
        lastAccessTime.clear();
        tick = 0;
    }

    /**
     * 프레임만 초기화: 퇴출 이력(evictHistory)은 유지
     * 이전 시뮬레이션의 워밍업 효과를 이어받고 싶을 때 사용
     */
    public void resetFramesOnly() {
        frames.clear();
        accessCount.clear();
        lastAccessTime.clear();
    }

    @Override
    public String getName()    { return "EvictCount"; }

    @Override
    public int getFrameCount() { return frameCount; }

    public Map<Integer, EvictRecord> getEvictHistory() {
        return Collections.unmodifiableMap(evictHistory);
    }

    public Map<Integer, Integer> getAccessCount() {
        return Collections.unmodifiableMap(accessCount);
    }
}
