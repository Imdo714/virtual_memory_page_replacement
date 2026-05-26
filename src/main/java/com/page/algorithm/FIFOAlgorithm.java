package com.page.algorithm;

import com.page.model.SimulationStep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/** FIFO(First-In First-Out) 페이지 교체 알고리즘 */
public class FIFOAlgorithm implements PageReplacementAlgorithm {

    private final int frameCount;
    private final Queue<Integer> queue; // FIFO는 Queue 자료구조 사용
    private final Set<Integer> frameSet; // 현재 프레임 관리 Set 자료구조 사용

    public FIFOAlgorithm(int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("Frame count must be positive.");
        this.frameCount = frameCount;
        this.queue = new LinkedList<>();
        this.frameSet = new HashSet<>();
    }

    @Override
    public SimulationStep accessPage(int pageNumber) {
        // Page Hit
        if (frameSet.contains(pageNumber)) {
            return new SimulationStep(pageNumber, false, new ArrayList<>(queue), null);
        }

        // Page Fault: 교체 대상 선정
        Integer evicted = null;
        if (queue.size() == frameCount) {
            evicted = queue.poll(); // 가장 먼저 들어온 페이지 제거
            frameSet.remove(evicted);
        }

        queue.offer(pageNumber);
        frameSet.add(pageNumber);

        return new SimulationStep(pageNumber, true, new ArrayList<>(queue), evicted);
    }

    @Override
    public void reset() {
        queue.clear();
        frameSet.clear();
    }

    @Override
    public String getName() {
        return "FIFO";
    }

    @Override
    public int getFrameCount() {
        return frameCount;
    }

}
