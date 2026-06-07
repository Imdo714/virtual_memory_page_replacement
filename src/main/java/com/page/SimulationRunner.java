package com.page;

import com.page.algorithm.AlgorithmFactory;
import com.page.algorithm.EvictCountAlgorithm;
import com.page.algorithm.NURAlgorithm;
import com.page.algorithm.PageReplacementAlgorithm;
import com.page.model.SimulationResult;
import com.page.model.SimulationStep;
import com.page.printer.ConsoleResultPrinter;
import com.page.printer.NURDebugPrinter;
import com.page.printer.ResultPrinter;
import com.page.simulator.Simulator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * CLI 입출력 흐름만 담당한다.
 * NUR은 수정 비트 입력이 필요하므로 별도 DEBUG UI로 실행한다.
 */
@Component
@ConditionalOnProperty(name = "page.cli.enabled", havingValue = "true")
public class SimulationRunner implements CommandLineRunner {

    private final ResultPrinter printer = new ConsoleResultPrinter();
    private final NURDebugPrinter nurDebugPrinter = new NURDebugPrinter();

    private record NurReference(int pageNumber, boolean write) {}

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("============================================");
        System.out.println("  OS Page Replacement Policy Simulator");
        System.out.println("============================================");

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("  1. 직접 입력하여 시뮬레이션");
            System.out.println("  2. 기본 예제 실행");
            System.out.println("  0. 종료");
            System.out.print("  선택 > ");

            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> runInteractive(scanner);
                case "2" -> runDefaultExample(scanner);
                case "0" -> running = false;
                default -> System.out.println("  [오류] 0, 1, 2 중 하나를 입력하세요.");
            }
        }

        System.out.println("프로그램을 종료합니다.");
        scanner.close();
    }

    private void runInteractive(Scanner scanner) {
        String algorithmType = readAlgorithmType(scanner);
        if (algorithmType == null) return;

        int frameCount = readFrameCount(scanner);
        if (frameCount < 0) return;

        if ("NUR".equalsIgnoreCase(algorithmType)) {
            NurReference[] referenceString = readNurReferenceString(scanner);
            if (referenceString == null) return;
            simulateNur(frameCount, referenceString);
            return;
        }

        int[] referenceString = readReferenceString(scanner);
        if (referenceString == null) return;

        simulate(algorithmType, frameCount, referenceString);
    }

    private String readAlgorithmType(Scanner scanner) {
        var supported = AlgorithmFactory.getSupportedAlgorithms();
        System.out.println("  알고리즘 선택:");
        for (int i = 0; i < supported.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, supported.get(i));
        }
        System.out.print("  선택 > ");
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= supported.size()) throw new IndexOutOfBoundsException();
            return supported.get(idx);
        } catch (Exception e) {
            System.out.println("  [오류] 올바른 번호를 입력하세요.");
            return null;
        }
    }

    private int readFrameCount(Scanner scanner) {
        System.out.print("  프레임 수 입력 (양의 정수): ");
        try {
            int n = Integer.parseInt(scanner.nextLine().trim());
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            System.out.println("  [오류] 양의 정수를 입력하세요.");
            return -1;
        }
    }

    private int[] readReferenceString(Scanner scanner) {
        System.out.print("  참조열 입력 (공백으로 구분된 정수, 예: 1 2 3 4 1 2): ");
        try {
            String[] tokens = scanner.nextLine().trim().split("\\s+");
            if (tokens.length == 0 || tokens[0].isEmpty()) throw new NumberFormatException();
            return Arrays.stream(tokens).mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException e) {
            System.out.println("  [오류] 공백으로 구분된 정수 목록을 입력하세요.");
            return null;
        }
    }

    private NurReference[] readNurReferenceString(Scanner scanner) {
        System.out.print("  NUR 참조열 입력 (예: 1R 2W 3R 1W, R 생략 시 읽기): ");
        try {
            String[] tokens = scanner.nextLine().trim().split("\\s+");
            if (tokens.length == 0 || tokens[0].isEmpty()) throw new NumberFormatException();

            List<NurReference> references = new ArrayList<>();
            for (String token : tokens) {
                references.add(parseNurReference(token));
            }
            return references.toArray(NurReference[]::new);
        } catch (Exception e) {
            System.out.println("  [오류] 예시처럼 페이지번호와 R/W를 입력하세요. 예: 1R 2W 3R");
            return null;
        }
    }

    private NurReference parseNurReference(String token) {
        String normalized = token.trim().toUpperCase();
        boolean write = normalized.endsWith("W");

        if (normalized.endsWith("R") || normalized.endsWith("W")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        int pageNumber = Integer.parseInt(normalized);
        return new NurReference(pageNumber, write);
    }

    private void runDefaultExample(Scanner scanner) {
        String algorithmType = readAlgorithmType(scanner);
        if (algorithmType == null) return;

        int frameCount = 3;

        if ("NUR".equalsIgnoreCase(algorithmType)) {
            NurReference[] ref = {
                    new NurReference(1, false),
                    new NurReference(2, true),
                    new NurReference(3, false),
                    new NurReference(1, true),
                    new NurReference(4, false),
                    new NurReference(2, false),
                    new NurReference(5, true)
            };
            System.out.println("  [NUR 기본 예제] 프레임: " + frameCount
                    + " / 참조열: " + formatNurReferences(ref));
            simulateNur(frameCount, ref);
            return;
        }

        int[] ref = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};
        System.out.println("  [기본 예제] 알고리즘: " + algorithmType
                + " / 프레임: " + frameCount
                + " / 참조열: " + Arrays.toString(ref));
        simulate(algorithmType, frameCount, ref);
    }

    private void simulate(String algorithmType, int frameCount, int[] referenceString) {
        PageReplacementAlgorithm algorithm = AlgorithmFactory.create(algorithmType, frameCount);
        Simulator simulator = new Simulator(algorithm);
        SimulationResult result = simulator.run(referenceString);
        printer.print(result);

        if (algorithm instanceof EvictCountAlgorithm evictAlgo) {
            printEvictCountTable(evictAlgo.getEvictHistory());
        }
    }

    private void simulateNur(int frameCount, NurReference[] referenceString) {
        NURAlgorithm algorithm = new NURAlgorithm(frameCount);
        int hits = 0;
        int faults = 0;
        int migrations = 0;

        System.out.println();
        System.out.println("== NUR UI Simulation ==");
        System.out.println("Frame Count: " + frameCount);
        System.out.println("Reference String: " + formatNurReferences(referenceString));
        System.out.println();

        for (int i = 0; i < referenceString.length; i++) {
            if (i > 0 && i % frameCount == 0) {
                algorithm.clearReferenceBits();
                System.out.println("== NUR reference bit reset ==");
                System.out.println();
            }

            NurReference reference = referenceString[i];
            SimulationStep step = algorithm.accessPage(reference.pageNumber(), reference.write());

            if (step.isPageFault()) {
                faults++;
                if (step.getEvictedPage() != null) {
                    migrations++;
                }
            } else {
                hits++;
            }

            nurDebugPrinter.printStep(algorithm, step, reference.write(), hits, faults, migrations);
        }

        nurDebugPrinter.printSummary(referenceString.length, hits, faults, migrations);
    }

    private String formatNurReferences(NurReference[] referenceString) {
        return Arrays.stream(referenceString)
                .map(ref -> ref.pageNumber() + (ref.write() ? "W" : "R"))
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private void printEvictCountTable(Map<Integer, EvictCountAlgorithm.EvictRecord> evictHistory) {
        int w = 48;
        System.out.println("+" + "-".repeat(w) + "+");
        System.out.printf("|%-" + w + "s|%n", "  [ Evict Count Table ]");
        System.out.println("+" + "-".repeat(w) + "+");
        System.out.printf("|  %-12s  %-12s  %-12s|%n", "Page", "Evict Count", "Last Evict");
        System.out.println("+" + "-".repeat(w) + "+");

        if (evictHistory.isEmpty()) {
            System.out.printf("|%-" + w + "s|%n", "  (퇴출된 페이지 없음)");
        } else {
            evictHistory.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e ->
                            System.out.printf("|  %-12d  %-12d  %-12d|%n",
                                    e.getKey(),
                                    e.getValue().count(),
                                    e.getValue().lastEvictedAt())
                    );
        }
        System.out.println("+" + "-".repeat(w) + "+");
        System.out.println();
    }
}
