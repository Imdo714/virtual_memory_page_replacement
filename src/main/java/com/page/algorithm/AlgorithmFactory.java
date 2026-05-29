package com.page.algorithm;

import java.util.List;

/** 알고리즘 객체 생성 책임만 담당 */
public class AlgorithmFactory {

    // 현재 지원하는 알고리즘 목록 리스트
    private static final List<String> SUPPORTED = List.of(
            "FIFO",
            "Optimal",
            "LRU",
            "EvictCount"
    );

    /**
     * 미래 참조열이 필요 없는 알고리즘용 (FIFO, LRU 등).
     * 기존 코드를 수정하지 않아도 동작하도록 기존 시그니처 유지.
     */
    public static PageReplacementAlgorithm create(String type, int frameCount) {
        return create(type, frameCount, null);
    }

    /**
     * 미래 참조열이 필요한 알고리즘(Optimal)을 포함한 통합 팩토리 메서드.
     * FIFO처럼 참조열이 불필요한 알고리즘은 referenceString을 무시한다.
     *
     * 새 알고리즘 추가 시 case 한 줄만 추가하면 됨.
     */
    public static PageReplacementAlgorithm create(String type, int frameCount, int[] referenceString) {
        return switch (type.toUpperCase()) {
            case "FIFO"       -> new FIFOAlgorithm(frameCount);
            case "OPTIMAL"    -> new OptimalAlgorithm(frameCount, referenceString);
            case "LRU"        -> new LRUAlgorithm(frameCount);
            case "EVICTCOUNT" -> new EvictCountAlgorithm(frameCount);
            default -> throw new IllegalArgumentException("지원하지 않는 알고리즘: " + type);
        };
    }

    public static List<String> getSupportedAlgorithms() {
        return SUPPORTED;
    }
}
