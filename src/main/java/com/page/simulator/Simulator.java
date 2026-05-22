package com.page.simulator;

import com.page.algorithm.PageReplacementAlgorithm;
import com.page.model.SimulationResult;
import com.page.model.SimulationStep;

import java.util.ArrayList;
import java.util.List;

/**
 * 참조열을 순서대로 알고리즘에 넘기고 결과를 모으는 Service Class 역할
 * 구체 클래스(FIFOAlgorithm)가 아닌 추상(PageReplacementAlgorithm)에 의존
 */
public class Simulator {

    private final PageReplacementAlgorithm algorithm;

    public Simulator(PageReplacementAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public SimulationResult run(int[] referenceString) {
        algorithm.reset();
        List<SimulationStep> steps = new ArrayList<>();

        for (int page : referenceString) {
            steps.add(algorithm.accessPage(page));
        }

        return new SimulationResult(
                algorithm.getName(),
                referenceString,
                algorithm.getFrameCount(),
                steps
        );
    }
}
