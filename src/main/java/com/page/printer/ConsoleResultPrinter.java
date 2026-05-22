package com.page.printer;

import com.page.model.SimulationResult;
import com.page.model.SimulationStep;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 콘솔 출력 포맷팅만 담당 */
public class ConsoleResultPrinter implements ResultPrinter {

    private static final String FAULT_LABEL = "FAULT";
    private static final String HIT_LABEL   = "  hit";

    @Override
    public void print(SimulationResult result) {
        printHeader(result);
        printStepTable(result);
        printSummary(result);
    }

    // ─── 헤더 ────────────────────────────────────────────────────────────────

    private void printHeader(SimulationResult result) {
        String title = " Page Replacement Simulation: " + result.getAlgorithmName();
        int width = Math.max(title.length() + 2, 48);

        System.out.println();
        System.out.println("+" + "=".repeat(width) + "+");
        System.out.printf("|%-" + width + "s|%n", title);
        System.out.println("+" + "=".repeat(width) + "+");

        String refStr = IntStream.of(result.getReferenceString())
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(", "));

        System.out.printf("  Frame Count     : %d%n", result.getFrameCount());
        System.out.printf("  Reference String: %s%n", refStr);
        System.out.println();
    }

    // ─── 단계별 테이블 ────────────────────────────────────────────────────────

    private void printStepTable(SimulationResult result) {
        int frameCount = result.getFrameCount();
        List<SimulationStep> steps = result.getSteps();

        // 최대 페이지 번호를 기준으로 열 너비 계산
        int maxPage = IntStream.of(result.getReferenceString()).max().orElse(9);
        int pageDigits = String.valueOf(maxPage).length();

        int stepW    = Math.max(4, String.valueOf(steps.size()).length());
        int pageW    = Math.max(4, pageDigits);
        int framesW  = Math.max(10, frameCount * (pageDigits + 2) + 3); // [p, p, p]
        int faultW   = 5;
        int evictedW = Math.max(7, pageDigits + 2);

        String colFmt = "| %" + stepW + "s | %" + pageW + "s | %-" + framesW + "s | %-"
                + faultW + "s | %" + evictedW + "s |%n";
        String sep = "+" + "-".repeat(stepW + 2) + "+" + "-".repeat(pageW + 2)
                + "+" + "-".repeat(framesW + 2) + "+" + "-".repeat(faultW + 2)
                + "+" + "-".repeat(evictedW + 2) + "+";

        System.out.println(sep);
        System.out.printf(colFmt, "Step", "Page", "Frames", "Fault", "Evicted");
        System.out.println(sep);

        for (int i = 0; i < steps.size(); i++) {
            SimulationStep step = steps.get(i);
            String framesStr = formatFrames(step.getFrameSnapshot(), frameCount, pageDigits);
            String faultStr  = step.isPageFault() ? FAULT_LABEL : HIT_LABEL;
            String evictStr  = step.getEvictedPage() != null
                    ? String.valueOf(step.getEvictedPage()) : "-";

            System.out.printf(colFmt, i + 1, step.getPageNumber(), framesStr, faultStr, evictStr);
        }

        System.out.println(sep);
        System.out.println();
    }

    /** 프레임 내용을 [p1, p2, -] 형태로 포맷. 빈 슬롯은 '-'. */
    private String formatFrames(List<Integer> frames, int frameCount, int digits) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < frameCount; i++) {
            if (i < frames.size()) {
                sb.append(String.format("%" + digits + "d", frames.get(i)));
            } else {
                sb.append("-".repeat(digits));
            }
            if (i < frameCount - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    // ─── 요약 ─────────────────────────────────────────────────────────────────

    private void printSummary(SimulationResult result) {
        int w = 36;
        System.out.println("+" + "-".repeat(w) + "+");
        System.out.printf("|%-" + w + "s|%n", "  [ Summary ]");
        System.out.println("+" + "-".repeat(w) + "+");
        String faultRate = String.format("%.2f%%", result.getPageFaultRate());
        String hitRate   = String.format("%.2f%%", result.getPageHitRate());

        System.out.printf("|  %-20s: %-11d|%n", "Total References", result.getSteps().size());
        System.out.printf("|  %-20s: %-11d|%n", "Page Faults",      result.getPageFaultCount());
        System.out.printf("|  %-20s: %-11d|%n", "Page Hits",        result.getPageHitCount());
        System.out.printf("|  %-20s: %-11s|%n", "Fault Rate",       faultRate);
        System.out.printf("|  %-20s: %-11s|%n", "Hit Rate",         hitRate);
        System.out.println("+" + "-".repeat(w) + "+");
        System.out.println();
    }
}
