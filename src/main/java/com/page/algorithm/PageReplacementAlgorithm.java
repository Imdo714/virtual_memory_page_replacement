package com.page.algorithm;

import com.page.model.SimulationStep;

/**
 * 페이지 교체 알고리즘이 반드시 제공해야 할 최소한의 기능만 정의
 * 새 알고리즘 추가 시 이 인터페이스를 구현하기만 하면 됨
 */
public interface PageReplacementAlgorithm {

    /** 페이지 참조 1회 처리, 히트/폴트 여부와 프레임 스냅샷을 SimulationStep으로 반환 */
    SimulationStep accessPage(int pageNumber);

    /** 내부 상태 초기화, 자료구조, 페레임 초기화 */
    void reset();

    String getName();

    int getFrameCount();
}
