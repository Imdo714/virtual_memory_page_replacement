package com.page.algorithm;

import com.page.model.SimulationStep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Optimal (OPT / Bélády's) 페이지 교체 알고리즘.
 *
 * 앞으로 가장 오래 참조되지 않을 페이지를 교체 대상으로 선정.
 * 이론상 page fault 횟수가 최소임을 보장하며,
 * 다른 알고리즘의 성능 비교 기준(upper bound)으로 사용된다.
 *
 * ※ 생성자에서 전체 참조열을 받는 이유:
 *    각 page fault 시점에 "미래에 가장 늦게 쓰이는 페이지"를 찾으려면
 *    전체 참조열(referenceString)을 미리 알아야 한다.
 */
public class OptimalAlgorithm implements PageReplacementAlgorithm {

    private final int frameCount;
    private final int[] referenceString; // 미래 참조 시점 계산용 (전체 참조열)
    private final List<Integer> frames;  // 현재 프레임에 적재된 페이지 목록
    private final Set<Integer> frameSet; // O(1) 포함 여부 검사용 보조 집합
    private int currentIndex;            // 현재 처리 중인 참조열 위치

    public OptimalAlgorithm(int frameCount, int[] referenceString) {
        if (frameCount <= 0)
            throw new IllegalArgumentException("Frame count must be positive.");
        if (referenceString == null || referenceString.length == 0)
            throw new IllegalArgumentException("Reference string must not be empty.");

        this.frameCount = frameCount;
        this.referenceString = referenceString.clone();
        this.frames = new ArrayList<>();
        this.frameSet = new HashSet<>();
        this.currentIndex = 0;
    }

    @Override
    public SimulationStep accessPage(int pageNumber) {
        // Page Hit
        if (frameSet.contains(pageNumber)) {
            currentIndex++;
            return new SimulationStep(pageNumber, false, new ArrayList<>(frames), null);
        }

        // Page Fault: 교체 대상 선정
        Integer evicted = null;
        if (frames.size() == frameCount) {
            evicted = findOptimalVictim();
            frames.remove(evicted);
            frameSet.remove(evicted);
        }

        frames.add(pageNumber);
        frameSet.add(pageNumber);
        currentIndex++;

        return new SimulationStep(pageNumber, true, new ArrayList<>(frames), evicted);
    }

    /**
     * 프레임에 있는 페이지 중 앞으로 가장 늦게(또는 영원히) 사용될 페이지를 반환.
     */
    private Integer findOptimalVictim() {
        Integer victim = null;
        int farthestUse = -1;

        for (Integer page : frames) {
            int nextUse = findNextUse(page, currentIndex + 1);
            if (nextUse > farthestUse) {
                farthestUse = nextUse;
                victim = page;
            }
        }
        return victim;
    }

    /**
     * fromIndex 이후 참조열에서 해당 page가 처음 등장하는 인덱스를 반환.
     * 등장하지 않으면 Integer.MAX_VALUE 반환 → 최우선 교체 대상.
     */
    private int findNextUse(int page, int fromIndex) {
        for (int i = fromIndex; i < referenceString.length; i++) {
            if (referenceString[i] == page) return i;
        }
        return Integer.MAX_VALUE; // 앞으로 사용되지 않음
    }

    @Override
    public void reset() {
        frames.clear();
        frameSet.clear();
        currentIndex = 0;
    }

    @Override
    public String getName()    { return "Optimal"; }

    @Override
    public int getFrameCount() { return frameCount; }
}
