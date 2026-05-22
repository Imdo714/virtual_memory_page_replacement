package com.page.printer;

import com.page.model.SimulationResult;

/**
 * ISP: 출력이라는 단 하나의 관심사만 정의.
 * OCP: 파일 출력, HTML 등 새 포맷 추가 시 이 인터페이스를 구현.
 */
public interface ResultPrinter {
    void print(SimulationResult result);
}
