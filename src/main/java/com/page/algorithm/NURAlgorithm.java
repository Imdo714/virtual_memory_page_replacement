package com.page.algorithm;

import com.page.model.SimulationStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NUR(Not Used Recently) 페이지 교체 알고리즘
 *
 * 참조 비트(R)와 수정 비트(M)를 사용해 페이지를 4개 클래스로 나눈 뒤,
 * 번호가 가장 낮은 클래스의 페이지를 교체 대상으로 선택한다.
 *
 *   Class 0: R = 0, M = 0  최근 사용되지 않았고 수정되지 않은 페이지
 *   Class 1: R = 0, M = 1  최근 사용되지 않았지만 수정된 페이지
 *   Class 2: R = 1, M = 0  최근 사용되었지만 수정되지 않은 페이지
 *   Class 3: R = 1, M = 1  최근 사용되었고 수정된 페이지
 *
 * 현재 시뮬레이터의 기본 입력은 페이지 번호만 있으므로 accessPage(int)는 읽기 접근으로 처리한다.
 * 쓰기 접근까지 표현하려면 accessPage(pageNumber, true)를 사용하면 된다.
 */
public class NURAlgorithm implements PageReplacementAlgorithm {

    private final int frameCount;
    private final LinkedHashMap<Integer, Boolean> referenceBits; // 페이지 삽입 순서와 참조 비트(R) 관리
    private final Map<Integer, Boolean> modifiedBits;            // 페이지별 수정 비트(M) 관리

    public NURAlgorithm(int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("Frame count must be positive.");
        this.frameCount = frameCount;
        this.referenceBits = new LinkedHashMap<>();
        this.modifiedBits = new LinkedHashMap<>();
    }

    @Override
    public SimulationStep accessPage(int pageNumber) {
        return accessPage(pageNumber, false); // 기본 시뮬레이터 입력은 읽기 접근으로 처리
    }

    /**
     * 참조열의 1R / 1W 형식에 맞춰 페이지에 접근한다.
     *
     * [Page Hit]  이미 프레임에 존재 → R=1 로 업데이트, 쓰기(W)면 M=1 도 추가
     * [Page Fault] 프레임에 없음    → R=0, M=0 으로 적재만 수행
     *              이유: 막 메모리에 올라온 페이지는 아직 사용된 것이 아니므로
     *              R·M 비트는 0 에서 시작해야 한다.
     *              이후 같은 페이지가 다시 참조열에 등장하면(Hit) 그때 비트가 올라간다.
     */
    public SimulationStep accessPage(int pageNumber, boolean write) {
        // Page Hit: 이미 프레임에 있으면 접근 유형에 따라 비트를 갱신한다
        if (referenceBits.containsKey(pageNumber)) {
            referenceBits.put(pageNumber, true);       // 읽기·쓰기 모두 R=1
            if (write) {
                modifiedBits.put(pageNumber, true);    // 쓰기(W)면 M=1 추가
            }
            return new SimulationStep(pageNumber, false, frameSnapshot(), null);
        }

        // Page Fault: 프레임이 가득 찬 경우 4개 클래스 기준으로 교체 대상 선정
        Integer evicted = null;
        if (referenceBits.size() == frameCount) {
            evicted = findVictim();
            referenceBits.remove(evicted);
            modifiedBits.remove(evicted);
        }

        // 새로 적재된 페이지는 R=0, M=0 에서 시작한다
        // (참조열에서 이 페이지가 다시 등장할 때 Hit 경로에서 비트가 업데이트됨)
        referenceBits.put(pageNumber, false);
        modifiedBits.put(pageNumber, false);
        return new SimulationStep(pageNumber, true, frameSnapshot(), evicted);
    }

    /** 4개 클래스 중 가장 낮은 클래스에 속한 페이지를 선택한다. */
    private Integer findVictim() {
        for (int targetClass = 0; targetClass <= 3; targetClass++) {
            Integer victim = findFirstPageInClass(targetClass);
            if (victim != null) {
                return victim;
            }
        }

        // 이론상 도달하지 않지만, 방어적으로 가장 오래된 페이지를 반환
        return referenceBits.keySet().iterator().next();
    }

    /** 삽입 순서대로 탐색해 지정된 클래스의 가장 오래된 페이지를 반환한다. */
    private Integer findFirstPageInClass(int targetClass) {
        for (Integer page : referenceBits.keySet()) {
            if (pageClass(page) == targetClass) {
                return page;
            }
        }
        return null;
    }

    /** 페이지의 (R, M) 비트를 0~3 클래스 번호로 변환한다. */
    private int pageClass(int page) {
        boolean referenced = referenceBits.getOrDefault(page, false);
        boolean modified = modifiedBits.getOrDefault(page, false);

        if (!referenced && !modified) return 0;
        if (!referenced) return 1;
        if (!modified) return 2;
        return 3;
    }

    public boolean isReferenced(int page) {
        return referenceBits.getOrDefault(page, false);
    }

    public boolean isModified(int page) {
        return modifiedBits.getOrDefault(page, false);
    }

    public int getPageClass(int page) {
        return pageClass(page);
    }

    /** OS의 주기적 reference bit 초기화를 흉내 내기 위해 모든 참조 비트를 false로 초기화한다. */
    public void clearReferenceBits() {
        for (Integer page : new ArrayList<>(referenceBits.keySet())) {
            referenceBits.put(page, false);
        }
    }

    /** 현재 프레임 상태를 삽입 순서대로 반환한다. */
    private ArrayList<Integer> frameSnapshot() {
        return new ArrayList<>(referenceBits.keySet());
    }

    @Override
    public void reset() {
        referenceBits.clear();
        modifiedBits.clear();
    }

    @Override
    public String getName() {
        return "NUR";
    }

    @Override
    public int getFrameCount() {
        return frameCount;
    }
}
