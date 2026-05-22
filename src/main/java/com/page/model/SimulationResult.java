package com.page.model;

import java.util.Collections;
import java.util.List;

/** 시뮬레이션 전체 결과 객체 */
public class SimulationResult {

    private final String algorithmName; // 사용된 알고리즘 이름
    private final int[] referenceString; // 입력된 참조열
    private final int frameCount;
    private final List<SimulationStep> steps; // 참조열 길이만큼의 Step 목록

    public SimulationResult(String algorithmName, int[] referenceString,
                            int frameCount, List<SimulationStep> steps) {
        this.algorithmName = algorithmName;
        this.referenceString = referenceString.clone();
        this.frameCount = frameCount;
        this.steps = Collections.unmodifiableList(steps);
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public int[] getReferenceString() {
        return referenceString.clone();
    }

    public int getFrameCount() {
        return frameCount;
    }

    public List<SimulationStep> getSteps() {
        return steps;
    }

    public int getPageFaultCount() {
        return (int) steps.stream().filter(SimulationStep::isPageFault).count();
    }

    public int getPageHitCount() {
        return steps.size() - getPageFaultCount();
    }

    public double getPageFaultRate() {
        return steps.isEmpty() ? 0.0 : (double) getPageFaultCount() / steps.size() * 100;
    }

    public double getPageHitRate() {
        return steps.isEmpty() ? 0.0 : (double) getPageHitCount() / steps.size() * 100;
    }
}
