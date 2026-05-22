package com.page;

import com.page.algorithm.AlgorithmFactory;
import com.page.algorithm.PageReplacementAlgorithm;
import com.page.model.SimulationResult;
import com.page.printer.ConsoleResultPrinter;
import com.page.printer.ResultPrinter;
import com.page.simulator.Simulator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

/**
 * CLI 입출력 흐름만 담당
 * PageReplacementAlgorithm, ResultPrinter 추상에 의존
 */
@Component
public class SimulationRunner implements CommandLineRunner {

    private final ResultPrinter printer = new ConsoleResultPrinter();

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

            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> runInteractive(scanner);
                case "2" -> runDefaultExample(scanner);
                case "0" -> running = false;
                default  -> System.out.println("  [오류] 0, 1, 2 중 하나를 입력하세요.");
            }
        }

        System.out.println("프로그램을 종료합니다.");
        scanner.close();
    }

    // ─── 사용자 직접 입력 ─────────────────────────────────────────────────────

    private void runInteractive(Scanner scanner) {
        String algorithmType = readAlgorithmType(scanner);
        if (algorithmType == null) return;

        int frameCount = readFrameCount(scanner);
        if (frameCount < 0) return;

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

    // ─── 기본 예제 ────────────────────────────────────────────────────────────

    private void runDefaultExample(Scanner scanner) {
        String algorithmType = readAlgorithmType(scanner);
        if (algorithmType == null) return;

        int frameCount = 3;
        int[] ref = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};
        System.out.println("  [기본 예제] 알고리즘: " + algorithmType
                + " / 프레임: " + frameCount
                + " / 참조열: " + Arrays.toString(ref));
        simulate(algorithmType, frameCount, ref);
    }

    // ─── 시뮬레이션 실행 ──────────────────────────────────────────────────────

    private void simulate(String algorithmType, int frameCount, int[] referenceString) {
        PageReplacementAlgorithm algorithm = AlgorithmFactory.create(algorithmType, frameCount);
        Simulator simulator = new Simulator(algorithm);
        SimulationResult result = simulator.run(referenceString);
        printer.print(result);
    }

}
