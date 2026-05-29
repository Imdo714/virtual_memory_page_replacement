package com.page.algorithm;

import com.page.model.SimulationStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * LRU(Least Recently Used) 페이지 교체 알고리즘
 *
 * 핵심 자료구조: LinkedHashMap (access-order 모드)
 *   - 내부적으로 HashMap + Doubly Linked List 구조
 *   - get() 호출 시 해당 항목이 자동으로 맨 뒤(MRU end)로 이동
 *   - iterator().next()는 항상 맨 앞(LRU end) 항목을 반환
 *
 * 동작 요약:
 *   Page Hit  → frames.get() 한 번으로 순서 재배치(해당 페이지를 맨 뒤로)
 *   Page Fault → 프레임이 가득 찼으면 맨 앞(가장 오래된) 페이지를 퇴출 후 새 페이지 삽입
 */

/** LRU(Least Recently Used) 페이지 교체 알고리즘 */
public class LRUAlgorithm implements PageReplacementAlgorithm {

    private final int frameCount;
    private LinkedHashMap<Integer, Boolean> frames;

    public LRUAlgorithm(int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("Frame count must be positive.");
        this.frameCount = frameCount;
        this.frames = newAccessOrderMap();
    }

    /** accessOrder=true: 접근된 항목을 자동으로 맨 뒤로 이동 */
    private LinkedHashMap<Integer, Boolean> newAccessOrderMap() {
        return new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    public SimulationStep accessPage(int pageNumber) {
        // Page Hit: get() 호출만으로 LinkedHashMap이 해당 항목을 맨 뒤로 재배치
        if (frames.containsKey(pageNumber)) {
            frames.get(pageNumber);
            return new SimulationStep(pageNumber, false, new ArrayList<>(frames.keySet()), null);
        }

        // Page Fault: 교체 대상 선정
        Integer evicted = null;
        if (frames.size() == frameCount) {
            // iterator의 첫 번째 항목 = LRU end(가장 오래전에 참조된 페이지)
            evicted = frames.keySet().iterator().next();
            frames.remove(evicted);
        }

        frames.put(pageNumber, Boolean.TRUE);
        return new SimulationStep(pageNumber, true, new ArrayList<>(frames.keySet()), evicted);
    }

    @Override
    public void reset() {
        frames = newAccessOrderMap();
    }

    @Override
    public String getName() {
        return "LRU";
    }

    @Override
    public int getFrameCount() {
        return frameCount;
    }
}
