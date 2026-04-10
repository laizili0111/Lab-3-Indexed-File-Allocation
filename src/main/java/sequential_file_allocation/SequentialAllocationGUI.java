package sequential_file_allocation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class SequentialAllocationGUI extends JFrame {
    private final int DISK_SIZE = 64; // For simulation simplicity, we use a small disk size of 64 blocks
    private boolean[] disk = new boolean[DISK_SIZE];
    
    // Directory stores: File Name -> (Start Block, Length)
    private Map<String, FileEntry> directory = new HashMap<>(); 
    
    private JTextArea displayArea;
    private JTextField fileNameField, fileSizeField;

    // Helper class to store sequential file metadata
    static class FileEntry {
        int start;
        int length;
        FileEntry(int start, int length) {
            this.start = start;
            this.length = length;
        }
    }

    public SequentialAllocationGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        setTitle("Sequential File Allocation Simulator");
        setSize(750, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 242, 245));
        setContentPane(mainPanel);

        // --- 1. Top Panel: Input Controls ---
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, " Sequential Allocation Controls ", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("SansSerif", Font.BOLD, 14)),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        inputPanel.add(new JLabel("File Name:"));
        fileNameField = new JTextField();
        inputPanel.add(fileNameField);
        
        inputPanel.add(new JLabel("File Size (Blocks):"));
        fileSizeField = new JTextField();
        inputPanel.add(fileSizeField);
        
        JButton createBtn = new JButton("Allocate File");
        createBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        createBtn.setForeground(Color.BLACK);
        createBtn.addActionListener(e -> allocateFile());
        inputPanel.add(createBtn);

        // --- 2. Center Panel: Output Display ---
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        displayArea.setBackground(new Color(252, 252, 252));
        
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" File System Status "));

        // --- 3. Bottom Panel: Legend ---
        JLabel legendLabel = new JLabel("LEGEND: [ S ] = Start Block  |  [ X ] = Allocated Block  |  [ . ] = Free Block");
        legendLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        legendLabel.setHorizontalAlignment(JLabel.CENTER);
        legendLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(legendLabel, BorderLayout.SOUTH);
        
        updateDisplay("Welcome! Sequential Allocation requires contiguous disk space.");
    }

    private void allocateFile() {
        String name = fileNameField.getText();
        String sizeStr = fileSizeField.getText();
        
        if (name.isEmpty() || sizeStr.isEmpty()) return;
        if (directory.containsKey(name)) { // Check for duplicate file name
            JOptionPane.showMessageDialog(this, "Error: File '" + name + "' already exists!");
            return;
        }

        try {
            int length = Integer.parseInt(sizeStr);
            int startBlock = findContiguousSpace(length);

            if (startBlock == -1) {
                JOptionPane.showMessageDialog(this, "Error: No contiguous space of size " + length + " found!");
                return;
            }

            // Allocate blocks sequentially
            for (int i = startBlock; i < startBlock + length; i++) {
                disk[i] = true;
            }

            directory.put(name, new FileEntry(startBlock, length));
            
            renderFileSystem();
            fileNameField.setText("");
            fileSizeField.setText("");
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Size must be a number!");
        }
    }

    // Algorithm to find the first available contiguous hole / consecutive free disk spaces of required length
    private int findContiguousSpace(int required) {
        int count = 0;
        for (int i = 0; i < DISK_SIZE; i++) {
            if (!disk[i]) {
                count++;
                if (count == required) return i - (required - 1);
            } else {
                count = 0;
            }
        }
        return -1; // No sufficient contiguous space
    }

    private void renderFileSystem() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s | %-12s | %-12s | %-15s\n", "FILE NAME", "START BLOCK", "LENGTH", "BLOCK RANGE"));
        sb.append("============================================================\n");
        
        for (String fileName : directory.keySet()) {
            FileEntry entry = directory.get(fileName);
            int endBlock = entry.start + entry.length - 1;
            sb.append(String.format("%-15s | %-12d | %-12d | %d to %d\n", 
                fileName, entry.start, entry.length, entry.start, endBlock));
        }
        
        sb.append("\nPHYSICAL DISK MAP (Contiguous Layout):\n");
        sb.append("------------------------------------------------------------\n");
        
        // Identify start blocks for visualization
        Set<Integer> startBlocks = new HashSet<>();
        for (FileEntry e : directory.values()) startBlocks.add(e.start);

        for (int i = 0; i < DISK_SIZE; i++) {
            if (startBlocks.contains(i)) sb.append("[ S ] "); // Start of a file
            else if (disk[i]) sb.append("[ X ] ");           // Part of a file
            else sb.append("[ . ] ");                        // Free
            
            if ((i + 1) % 8 == 0) sb.append("\n");
        }
        
        updateDisplay(sb.toString());
    }

    private void updateDisplay(String text) {
        displayArea.setText(text);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SequentialAllocationGUI().setVisible(true));
    }
}