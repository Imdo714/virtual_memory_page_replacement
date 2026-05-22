package com.page.algorithm;

import java.util.List;

/** 알고리즘 객체 생성 책임만 담당 */
public class AlgorithmFactory {

    // 현재 지원하는 알고리즘 목록 리스트
    private static final List<String> SUPPORTED = List.of(
            "FIFO"
            // "LRU"
    );

    // 알고리즘 객체 생성
    public static PageReplacementAlgorithm create(String type, int frameCount) {
        return switch (type.toUpperCase()) {
            case "FIFO" -> new FIFOAlgorithm(frameCount);
            // case "LRU"  -> new LRUAlgorithm(frameCount);
            default -> throw new IllegalArgumentException("지원하지 않는 알고리즘: " + type);
        };
    }

    public static List<String> getSupportedAlgorithms() {
        return SUPPORTED;
    }
}
