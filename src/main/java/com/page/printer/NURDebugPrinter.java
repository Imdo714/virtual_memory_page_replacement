package com.page.printer;

import com.page.algorithm.NURAlgorithm;
import com.page.model.SimulationStep;

import java.util.List;
import java.util.stream.Collectors;

/** 문서 3.3.1의 DEBUG 출력 형태를 NUR 알고리즘에 맞게 출력한다. */
public class NURDebugPrinter {

    public void printStep(NURAlgorithm algorithm, SimulationStep step,
                          boolean write, int hits, int faults, int migrations) {
        System.out.println("------------------------------------");
        System.out.println("== NUR operate() DEBUG ==");
        System.out.printf("Ref page: %d(%s) | Status: %s%n",
                step.getPageNumber(),
                write ? "W" : "R",
                step.isPageFault() ? "PAGEFAULT" : "HIT");
        System.out.println("Current Frame: " + formatFrame(step.getFrameSnapshot()));
        System.out.println("R/M/Class: " + formatClassInfo(algorithm, step.getFrameSnapshot()));

        if (step.getEvictedPage() != null) {
            System.out.printf("Evicted Page: %d%n", step.getEvictedPage());
        }

        System.out.printf("Total Hits: %d | Faults: %d | Migrations: %d%n",
                hits, faults, migrations);
        System.out.println("------------------------------------");
    }

    public void printSummary(int total, int hits, int faults, int migrations) {
        System.out.println();
        System.out.println("+------------------------------------+");
        System.out.println("|  [ NUR Summary ]                   |");
        System.out.println("+------------------------------------+");
        System.out.printf("|  %-15s: %-15d|%n", "References", total);
        System.out.printf("|  %-15s: %-15d|%n", "Page Hits", hits);
        System.out.printf("|  %-15s: %-15d|%n", "Page Faults", faults);
        System.out.printf("|  %-15s: %-15d|%n", "Migrations", migrations);
        System.out.println("+------------------------------------+");
        System.out.println();
    }

    private String formatFrame(List<Integer> frames) {
        return frames.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" ", "[ ", " ]"));
    }

    private String formatClassInfo(NURAlgorithm algorithm, List<Integer> frames) {
        return frames.stream()
                .map(page -> String.format("%d(R=%d,M=%d,C=%d)",
                        page,
                        algorithm.isReferenced(page) ? 1 : 0,
                        algorithm.isModified(page) ? 1 : 0,
                        algorithm.getPageClass(page)))
                .collect(Collectors.joining(" ", "[ ", " ]"));
    }
}
