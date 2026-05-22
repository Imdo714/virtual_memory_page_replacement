package com.page.model;

import java.util.Collections;
import java.util.List;

/** 페이지 참조 1회 당 결과 객체 */
public class SimulationStep {

    private final int pageNumber; // 이번에 참조된 페이지 번호
    private final boolean pageFault; // true = 폴트, false = 히트
    private final List<Integer> frameSnapshot; // 참조 처리 후 프레임 상태
    private final Integer evictedPage;         // 교체되어 쫓겨난 페이지 번호 (없으면 null)

    public SimulationStep(int pageNumber, boolean pageFault,
                          List<Integer> frameSnapshot, Integer evictedPage) {
        this.pageNumber = pageNumber;
        this.pageFault = pageFault;
        this.frameSnapshot = Collections.unmodifiableList(frameSnapshot);
        this.evictedPage = evictedPage;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public boolean isPageFault() {
        return pageFault;
    }

    public List<Integer> getFrameSnapshot() {
        return frameSnapshot;
    }

    public Integer getEvictedPage() {
        return evictedPage;
    }

}
