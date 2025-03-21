package de.zonlykroks;

import de.zonlykroks.algorithm.sin.SinApproximationFunctions;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MathApproximationFramework extends JFrame {
    private JComboBox<String> functionComboBox;
    private JComboBox<String> referenceComboBox;
    private JList<String> approximationList;
    private DefaultListModel<String> approximationListModel;
    private JTextField startRangeField;
    private JTextField endRangeField;
    private JTextField pointsField;
    private JTextArea resultsArea;
    private JCheckBox reportWorstCheckbox;
    private JTextField iterationsField;

    private final Map<String, ApproximationFunction> availableFunctions = new HashMap<>();
    private final Map<String, Map<String, ApproximationMethod>> availableApproximations = new HashMap<>();

    private String lastSelectedFunction;
    private String lastSelectedReference;
    private List<String> lastSelectedApproximations;
    private double lastStartRange;
    private double lastEndRange;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MathApproximationFramework app = new MathApproximationFramework();
            app.setVisible(true);
        });
    }

    public MathApproximationFramework() {
        setTitle("Math Approximation Testing Framework");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 800);
        setLocationRelativeTo(null);

        initializeUI();
        registerFunctions();
        updateUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel functionPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        JPanel testPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        functionPanel.setBorder(new TitledBorder("Function Selection"));
        functionPanel.add(new JLabel("Function:"));
        functionComboBox = new JComboBox<>();
        functionPanel.add(functionComboBox);
        functionPanel.add(new JLabel("Reference Implementation:"));
        referenceComboBox = new JComboBox<>();
        functionPanel.add(referenceComboBox);

        JPanel approxPanel = new JPanel(new BorderLayout());
        approxPanel.setBorder(new TitledBorder("Approximation Methods"));
        approximationListModel = new DefaultListModel<>();
        approximationList = new JList<>(approximationListModel);
        approximationList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane approxScrollPane = new JScrollPane(approximationList);
        approxPanel.add(approxScrollPane, BorderLayout.CENTER);

        testPanel.setBorder(new TitledBorder("Test Parameters"));
        testPanel.add(new JLabel("Start Range:"));
        startRangeField = new JTextField("0.0");
        testPanel.add(startRangeField);
        testPanel.add(new JLabel("End Range:"));
        endRangeField = new JTextField(String.valueOf(2 * Math.PI));
        testPanel.add(endRangeField);
        testPanel.add(new JLabel("Test Points:"));
        pointsField = new JTextField("10000");
        testPanel.add(pointsField);
        testPanel.add(new JLabel("Report Worst Cases:"));
        reportWorstCheckbox = new JCheckBox();
        reportWorstCheckbox.setSelected(true);
        testPanel.add(reportWorstCheckbox);
        testPanel.add(new JLabel("Performance Iterations:"));
        iterationsField = new JTextField("1000000");
        testPanel.add(iterationsField);

        JButton accuracyButton = new JButton("Run Accuracy Test");
        JButton performanceButton = new JButton("Run Performance Test");
        JButton visualizeButton = new JButton("Visualize Results");
        JButton visualizeErrorButton = new JButton("Visualize Error Rates");
        buttonPanel.add(accuracyButton);
        buttonPanel.add(performanceButton);
        buttonPanel.add(visualizeButton);
        buttonPanel.add(visualizeErrorButton);

        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setPreferredSize(new Dimension(800, 250));

        controlPanel.add(functionPanel, BorderLayout.NORTH);
        controlPanel.add(approxPanel, BorderLayout.CENTER);
        controlPanel.add(testPanel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // Event listeners
        functionComboBox.addActionListener(_ -> updateApproximationList());

        accuracyButton.addActionListener(_ -> runAccuracyTest());
        performanceButton.addActionListener(_ -> runPerformanceTest());
        visualizeButton.addActionListener(_ -> visualizeFunctions());
        visualizeErrorButton.addActionListener(_ -> visualizeErrors());
    }

    private void registerFunctions() {
        registerFunction(new SinApproximationFunctions());
    }

    private void registerFunction(ApproximationFunction function) {
        String name = function.getName();
        availableFunctions.put(name, function);
        availableApproximations.put(name, new HashMap<>());

        for (ApproximationAlgorithm algorithm : function.getApproximationAlgorithms()) {
            availableApproximations.get(name).put(algorithm.getName(),
                    new ApproximationMethod(algorithm.getFunction(), algorithm.getName()));
        }
    }

    private void updateUI() {
        functionComboBox.removeAllItems();
        for (String name : availableFunctions.keySet()) {
            functionComboBox.addItem(name);
        }

        if (functionComboBox.getItemCount() > 0) {
            updateApproximationList();
        }
    }

    private void updateApproximationList() {
        String selectedFunction = (String) functionComboBox.getSelectedItem();
        if (selectedFunction == null) return;

        referenceComboBox.removeAllItems();
        for (String refName : availableFunctions.get(selectedFunction).getReferenceImplementations().keySet()) {
            referenceComboBox.addItem(refName);
        }

        approximationListModel.clear();
        for (String approxName : availableApproximations.get(selectedFunction).keySet()) {
            approximationListModel.addElement(approxName);
        }

        ApproximationFunction function = availableFunctions.get(selectedFunction);
        startRangeField.setText(String.valueOf(function.getDefaultStartRange()));
        endRangeField.setText(String.valueOf(function.getDefaultEndRange()));
    }

    private void runAccuracyTest() {
        resultsArea.setText("Running accuracy test...\n");

        try {
            String selectedFunction = (String) functionComboBox.getSelectedItem();
            String selectedReference = (String) referenceComboBox.getSelectedItem();
            double start = Double.parseDouble(startRangeField.getText());
            double end = Double.parseDouble(endRangeField.getText());
            int points = Integer.parseInt(pointsField.getText());
            boolean reportWorst = reportWorstCheckbox.isSelected();

            List<String> selectedApproximations = approximationList.getSelectedValuesList();

            if (selectedFunction == null || selectedReference == null || selectedApproximations.isEmpty()) {
                resultsArea.append("Please select function, reference, and at least one approximation method.");
                return;
            }

            lastSelectedFunction = selectedFunction;
            lastSelectedReference = selectedReference;
            lastSelectedApproximations = new ArrayList<>(selectedApproximations);
            lastStartRange = start;
            lastEndRange = end;

            resultsArea.append("Testing function: " + selectedFunction + "\n");
            resultsArea.append("Reference: " + selectedReference + "\n");
            resultsArea.append("Range: " + start + " to " + end + " with " + points + " points\n\n");

            final ApproximationTester tester = new ApproximationTester(selectedFunction);

            final ApproximationFunction function = availableFunctions.get(selectedFunction);
            tester.setReferenceFunction(
                    function.getReferenceImplementations().get(selectedReference),
                    selectedReference);

            for (String approxName : selectedApproximations) {
                resultsArea.append("Adding approximation: " + approxName + "\n");
                tester.registerApproximation(
                        availableApproximations.get(selectedFunction).get(approxName).function(),
                        approxName);
            }

            StringBuilder output = new StringBuilder();
            CustomOutputStream cos = new CustomOutputStream(output, resultsArea);
            PrintStream customOut = new PrintStream(cos);
            PrintStream oldOut = System.out;
            System.setOut(customOut);

            resultsArea.append("\n--- TEST RESULTS ---\n\n");
            tester.testRange(start, end, points, reportWorst);

            System.setOut(oldOut);

            resultsArea.setCaretPosition(0);

        } catch (NumberFormatException ex) {
            resultsArea.append("Invalid number format in one of the fields: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        } catch (Exception ex) {
            resultsArea.append("Error running test: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    private void runPerformanceTest() {
        resultsArea.setText("Running performance test...\n");

        try {
            String selectedFunction = (String) functionComboBox.getSelectedItem();
            String selectedReference = (String) referenceComboBox.getSelectedItem();
            int iterations = Integer.parseInt(iterationsField.getText());

            List<String> selectedApproximations = approximationList.getSelectedValuesList();

            if (selectedFunction == null || selectedReference == null || selectedApproximations.isEmpty()) {
                resultsArea.append("Please select function, reference, and at least one approximation method.");
                return;
            }

            resultsArea.append("Testing function: " + selectedFunction + "\n");
            resultsArea.append("Reference: " + selectedReference + "\n");
            resultsArea.append("Performance iterations: " + iterations + "\n\n");

            final ApproximationTester tester = new ApproximationTester(selectedFunction);

            final ApproximationFunction function = availableFunctions.get(selectedFunction);
            tester.setReferenceFunction(
                    function.getReferenceImplementations().get(selectedReference),
                    selectedReference);

            for (String approxName : selectedApproximations) {
                resultsArea.append("Adding approximation: " + approxName + "\n");
                tester.registerApproximation(
                        availableApproximations.get(selectedFunction).get(approxName).function(),
                        approxName);
            }

            StringBuilder output = new StringBuilder();
            CustomOutputStream cos = new CustomOutputStream(output, resultsArea);
            PrintStream customOut = new PrintStream(cos);
            PrintStream oldOut = System.out;
            System.setOut(customOut);

            resultsArea.append("\n--- TEST RESULTS ---\n\n");
            if (iterations > 0) {
                tester.testPerformance(iterations);
            } else {
                resultsArea.append("Iterations must be greater than 0\n");
            }

            System.setOut(oldOut);

            resultsArea.setCaretPosition(0);

        } catch (NumberFormatException ex) {
            resultsArea.append("Invalid number format in one of the fields: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        } catch (Exception ex) {
            resultsArea.append("Error running test: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    private void visualizeFunctions() {
        if (lastSelectedFunction == null || lastSelectedReference == null || lastSelectedApproximations == null) {
            resultsArea.setText("Please run an accuracy test first before visualizing results.");
            return;
        }

        try {
            XYSeriesCollection dataset = new XYSeriesCollection();

            ApproximationFunction function = availableFunctions.get(lastSelectedFunction);
            DoubleUnaryOperator refFunc = function.getReferenceImplementations().get(lastSelectedReference);
            XYSeries referenceSeries = new XYSeries("Reference: " + lastSelectedReference);

            Map<String, XYSeries> approxSeries = new HashMap<>();
            for (String approxName : lastSelectedApproximations) {
                approxSeries.put(approxName, new XYSeries(approxName));
            }

            int visualizationPoints = 1000;
            double step = (lastEndRange - lastStartRange) / (visualizationPoints - 1);

            for (int i = 0; i < visualizationPoints; i++) {
                double x = lastStartRange + i * step;
                double refY = refFunc.applyAsDouble(x);
                referenceSeries.add(x, refY);

                for (String approxName : lastSelectedApproximations) {
                    DoubleUnaryOperator approxFunc = availableApproximations.get(lastSelectedFunction)
                            .get(approxName).function();
                    double approxY = approxFunc.applyAsDouble(x);
                    approxSeries.get(approxName).add(x, approxY);
                }
            }

            dataset.addSeries(referenceSeries);
            for (String approxName : lastSelectedApproximations) {
                dataset.addSeries(approxSeries.get(approxName));
            }

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Function Approximation Visualization: " + lastSelectedFunction,
                    "X",
                    "Y",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            chart.getXYPlot().setRenderer(renderer);

            JFrame chartFrame = new JFrame("Function Visualization");
            chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            chartFrame.setSize(800, 600);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(760, 560));
            chartPanel.setMouseWheelEnabled(true);

            chartFrame.setContentPane(chartPanel);
            chartFrame.setLocationRelativeTo(this);
            chartFrame.setVisible(true);

            resultsArea.append("Visualization created for " + lastSelectedFunction + " with "
                    + lastSelectedApproximations.size() + " approximation methods.\n");

        } catch (Exception ex) {
            resultsArea.setText("Error creating visualization: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    private void visualizeErrors() {
        if (lastSelectedFunction == null || lastSelectedReference == null || lastSelectedApproximations == null) {
            resultsArea.setText("Please run an accuracy test first before visualizing error rates.");
            return;
        }

        try {
            XYSeriesCollection dataset = new XYSeriesCollection();

            ApproximationFunction function = availableFunctions.get(lastSelectedFunction);
            DoubleUnaryOperator refFunc = function.getReferenceImplementations().get(lastSelectedReference);

            Map<String, XYSeries> errorSeries = new HashMap<>();
            for (String approxName : lastSelectedApproximations) {
                errorSeries.put(approxName, new XYSeries("Error: " + approxName));
            }

            int visualizationPoints = 1000;
            double step = (lastEndRange - lastStartRange) / (visualizationPoints - 1);

            for (int i = 0; i < visualizationPoints; i++) {
                double x = lastStartRange + i * step;
                double refY = refFunc.applyAsDouble(x);

                for (String approxName : lastSelectedApproximations) {
                    DoubleUnaryOperator approxFunc = availableApproximations.get(lastSelectedFunction)
                            .get(approxName).function();
                    double approxY = approxFunc.applyAsDouble(x);
                    double error = Math.abs(approxY - refY);
                    errorSeries.get(approxName).add(x, error);
                }
            }

            for (String approxName : lastSelectedApproximations) {
                dataset.addSeries(errorSeries.get(approxName));
            }

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Approximation Error Visualization: " + lastSelectedFunction,
                    "X",
                    "Absolute Error",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            JFrame chartFrame = new JFrame("Error Visualization");
            chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            chartFrame.setSize(800, 600);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(760, 560));
            chartPanel.setMouseWheelEnabled(true);

            chartFrame.setContentPane(chartPanel);
            chartFrame.setLocationRelativeTo(this);
            chartFrame.setVisible(true);

            resultsArea.append("Error visualization created for " + lastSelectedFunction + " with "
                    + lastSelectedApproximations.size() + " approximation methods.\n");

        } catch (Exception ex) {
            resultsArea.setText("Error creating error visualization: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    /**
     * Custom output stream that writes to both a StringBuilder and updates a JTextArea
     */
    private static class CustomOutputStream extends OutputStream {
        private final StringBuilder stringBuilder;
        private final JTextArea textArea;

        public CustomOutputStream(StringBuilder sb, JTextArea ta) {
            this.stringBuilder = sb;
            this.textArea = ta;
        }

        @Override
        public void write(int b) {
            char c = (char) b;
            stringBuilder.append(c);
            SwingUtilities.invokeLater(() -> {
                textArea.append(String.valueOf(c));
                // Auto-scroll to the end
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }
}