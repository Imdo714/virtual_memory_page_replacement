package com.page.ui;

import com.page.algorithm.EvictCountAlgorithm;
import com.page.algorithm.FIFOAlgorithm;
import com.page.algorithm.LRUAlgorithm;
import com.page.algorithm.NURAlgorithm;
import com.page.algorithm.PageReplacementAlgorithm;
import com.page.model.SimulationStep;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NURSwingFrame extends JFrame {

    private static final String NUR = "NUR";
    private static final String LRU = "LRU";
    private static final String FIFO = "FIFO";
    private static final String EVICT_COUNT = "EvictCount";

    private final JComboBox<String> algorithmCombo = new JComboBox<>(new String[]{NUR, LRU, FIFO, EVICT_COUNT});
    private final JSpinner frameCountSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
    private final JTextField referenceInput = new JTextField("1R 2W 3R 1W 4R 2R 5W");
    private final JCheckBox resetReferenceBits = new JCheckBox("NUR: 프레임 수마다 R 비트 초기화", true);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Step", "Ref", "Status", "Current Frame", "Evicted", "Detail", "Hits", "Faults", "Migrations"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(tableModel);
    private final JLabel hitLabel = metricLabel("Hits: 0");
    private final JLabel faultLabel = metricLabel("Faults: 0");
    private final JLabel hitRateLabel = metricLabel("Hit Rate: 0.00%");
    private final JLabel faultRateLabel = metricLabel("Fault Rate: 0.00%");
    private final JLabel migrationLabel = metricLabel("Migrations: 0");
    private final JLabel totalLabel = metricLabel("References: 0");

    public NURSwingFrame() {
        super("Page Replacement Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 640));
        setLocationRelativeTo(null);

        add(createHeader(), BorderLayout.NORTH);
        add(createTable(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        configureTable();
        updateInputForAlgorithm();
        runSimulation();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        panel.setBackground(new Color(244, 246, 248));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Page Replacement Simulator");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 6;
        gbc.weightx = 1;
        panel.add(title, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("알고리즘"), gbc);

        gbc.gridx = 1;
        algorithmCombo.setPreferredSize(new Dimension(130, 30));
        algorithmCombo.addActionListener(event -> updateInputForAlgorithm());
        panel.add(algorithmCombo, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("프레임 수"), gbc);

        gbc.gridx = 3;
        frameCountSpinner.setPreferredSize(new Dimension(90, 30));
        panel.add(frameCountSpinner, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("참조열"), gbc);

        gbc.gridx = 5;
        gbc.weightx = 1;
        referenceInput.setPreferredSize(new Dimension(520, 30));
        panel.add(referenceInput, gbc);

        JButton runButton = new JButton("실행");
        runButton.addActionListener(event -> runSimulation());

        gbc.gridx = 6;
        gbc.weightx = 0;
        panel.add(runButton, gbc);

        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(resetReferenceBits, gbc);

        return panel;
    }

    private JScrollPane createTable() {
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        return scrollPane;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        panel.setBackground(new Color(244, 246, 248));
        panel.add(totalLabel);
        panel.add(hitLabel);
        panel.add(faultLabel);
        panel.add(hitRateLabel);
        panel.add(faultRateLabel);
        panel.add(migrationLabel);
        return panel;
    }

    private void configureTable() {
        resultTable.setRowHeight(34);
        resultTable.setAutoCreateRowSorter(false);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.getTableHeader().setFont(resultTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        resultTable.setDefaultRenderer(Object.class, new ResultCellRenderer());

        resultTable.getColumnModel().getColumn(0).setPreferredWidth(52);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        resultTable.getColumnModel().getColumn(5).setPreferredWidth(410);
        resultTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(8).setPreferredWidth(90);
    }

    private void updateInputForAlgorithm() {
        String selected = selectedAlgorithm();
        boolean isNur = NUR.equals(selected);
        resetReferenceBits.setVisible(isNur);

        if (isNur) {
            referenceInput.setText("1R 2W 3R 1W 4R 2R 5W");
        } else {
            referenceInput.setText("1 2 3 4 1 2 5 1 2 3 4 5");
        }
    }

    private void runSimulation() {
        try {
            if (NUR.equals(selectedAlgorithm())) {
                runNurSimulation();
            } else {
                runBasicSimulation();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "참조열 형식을 확인하세요.\nNUR 예: 1R 2W 3R 1W\n기타 알고리즘 예: 1 2 3 4 1 2",
                    "입력 오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void runNurSimulation() {
        int frameCount = (Integer) frameCountSpinner.getValue();
        List<NurReference> references = parseNurReferences(referenceInput.getText());
        NURAlgorithm algorithm = new NURAlgorithm(frameCount);
        Counter counter = new Counter();
        tableModel.setRowCount(0);

        for (int i = 0; i < references.size(); i++) {
            if (resetReferenceBits.isSelected() && i > 0 && i % frameCount == 0) {
                algorithm.clearReferenceBits();
            }

            NurReference reference = references.get(i);
            SimulationStep step = algorithm.accessPage(reference.pageNumber(), reference.write());
            counter.apply(step);

            addRow(
                    i + 1,
                    reference.pageNumber() + (reference.write() ? "W" : "R"),
                    step,
                    formatNurDetail(algorithm, step.getFrameSnapshot()),
                    counter
            );
        }

        updateMetrics(references.size(), counter);
    }

    private void runBasicSimulation() {
        int frameCount = (Integer) frameCountSpinner.getValue();
        List<Integer> references = parsePageReferences(referenceInput.getText());
        PageReplacementAlgorithm algorithm = createBasicAlgorithm(selectedAlgorithm(), frameCount);
        Counter counter = new Counter();
        tableModel.setRowCount(0);

        for (int i = 0; i < references.size(); i++) {
            SimulationStep step = algorithm.accessPage(references.get(i));
            counter.apply(step);

            addRow(
                    i + 1,
                    String.valueOf(references.get(i)),
                    step,
                    formatBasicDetail(algorithm),
                    counter
            );
        }

        updateMetrics(references.size(), counter);
    }

    private PageReplacementAlgorithm createBasicAlgorithm(String algorithmName, int frameCount) {
        return switch (algorithmName) {
            case FIFO -> new FIFOAlgorithm(frameCount);
            case LRU -> new LRUAlgorithm(frameCount);
            case EVICT_COUNT -> new EvictCountAlgorithm(frameCount);
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + algorithmName);
        };
    }

    private void addRow(int stepNumber, String reference, SimulationStep step, String detail, Counter counter) {
        tableModel.addRow(new Object[]{
                stepNumber,
                reference,
                step.isPageFault() ? "PAGEFAULT" : "HIT",
                formatFrame(step.getFrameSnapshot()),
                step.getEvictedPage() == null ? "-" : step.getEvictedPage(),
                detail,
                counter.hits,
                counter.faults,
                counter.migrations
        });
    }

    private void updateMetrics(int total, Counter counter) {
        double hitRate = total == 0 ? 0.0 : (double) counter.hits / total * 100;
        double faultRate = total == 0 ? 0.0 : (double) counter.faults / total * 100;

        totalLabel.setText("References: " + total);
        hitLabel.setText("Hits: " + counter.hits);
        faultLabel.setText("Faults: " + counter.faults);
        hitRateLabel.setText(String.format("Hit Rate: %.2f%%", hitRate));
        faultRateLabel.setText(String.format("Fault Rate: %.2f%%", faultRate));
        migrationLabel.setText("Migrations: " + counter.migrations);
    }

    private String selectedAlgorithm() {
        return String.valueOf(algorithmCombo.getSelectedItem());
    }

    private List<NurReference> parseNurReferences(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Reference string is empty.");
        }

        List<NurReference> references = new ArrayList<>();
        for (String token : text.trim().split("\\s+")) {
            references.add(parseNurReference(token));
        }
        return references;
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

    private List<Integer> parsePageReferences(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Reference string is empty.");
        }

        List<Integer> references = new ArrayList<>();
        for (String token : text.trim().split("\\s+")) {
            references.add(parsePageNumber(token));
        }
        return references;
    }

    private int parsePageNumber(String token) {
        String normalized = token.trim().toUpperCase();
        if (normalized.endsWith("R") || normalized.endsWith("W")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return Integer.parseInt(normalized);
    }

    private String formatFrame(List<Integer> frames) {
        return frames.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" ", "[ ", " ]"));
    }

    private String formatNurDetail(NURAlgorithm algorithm, List<Integer> frames) {
        return frames.stream()
                .map(page -> String.format("%d(R=%d,M=%d,C=%d)",
                        page,
                        algorithm.isReferenced(page) ? 1 : 0,
                        algorithm.isModified(page) ? 1 : 0,
                        algorithm.getPageClass(page)))
                .collect(Collectors.joining("  "));
    }

    private String formatBasicDetail(PageReplacementAlgorithm algorithm) {
        if (algorithm instanceof EvictCountAlgorithm evictCountAlgorithm) {
            Map<Integer, Integer> counts = evictCountAlgorithm.getEvictCounts();
            if (counts.isEmpty()) {
                return "Evict Count: -";
            }
            return counts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", ", "Evict Count: ", ""));
        }

        return "-";
    }

    private JLabel metricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 222, 230)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return label;
    }

    private record NurReference(int pageNumber, boolean write) {}

    private static class Counter {
        private int hits;
        private int faults;
        private int migrations;

        private void apply(SimulationStep step) {
            if (step.isPageFault()) {
                faults++;
                if (step.getEvictedPage() != null) {
                    migrations++;
                }
            } else {
                hits++;
            }
        }
    }

    private static class ResultCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            if (!isSelected) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                component.setForeground(new Color(23, 32, 42));
            }

            if (column == 2) {
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected && "PAGEFAULT".equals(value)) {
                    component.setBackground(new Color(255, 224, 219));
                    component.setForeground(new Color(138, 39, 25));
                } else if (!isSelected && "HIT".equals(value)) {
                    component.setBackground(new Color(220, 236, 200));
                    component.setForeground(new Color(47, 93, 22));
                }
            } else {
                setHorizontalAlignment(column == 0 || column >= 6 ? SwingConstants.CENTER : SwingConstants.LEFT);
            }

            return component;
        }
    }
}
