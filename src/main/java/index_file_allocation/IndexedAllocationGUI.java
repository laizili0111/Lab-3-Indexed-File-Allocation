package index_file_allocation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class IndexedAllocationGUI extends JFrame {
    private final int DISK_SIZE = 64;
    private boolean[] disk = new boolean[DISK_SIZE];
    private Map<String, Integer> directory = new HashMap<>(); 
    private Map<Integer, List<Integer>> physicalDisk = new HashMap<>(); 
    
    private JTextArea displayArea;
    private JTextField fileNameField, fileSizeField;

    public IndexedAllocationGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        setTitle("Indexed File Allocation Simulator");
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
            BorderFactory.createTitledBorder(null, " Allocation Controls ", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("SansSerif", Font.BOLD, 14)),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        inputPanel.add(new JLabel("File Name:"));
        fileNameField = new JTextField();
        inputPanel.add(fileNameField);
        
        inputPanel.add(new JLabel("Number of Blocks:"));
        fileSizeField = new JTextField();
        inputPanel.add(fileSizeField);
        
        JButton createBtn = new JButton("Allocate File");
        createBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        createBtn.setBackground(new Color(70, 130, 180));
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
        JLabel legendLabel = new JLabel("LEGEND: [ I ] = Index Block  |  [ D ] = Data Block  |  [ . ] = Free Block");
        legendLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        legendLabel.setHorizontalAlignment(JLabel.CENTER);
        legendLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Adding components to main panel
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(legendLabel, BorderLayout.SOUTH);
        
        updateDisplay("Welcome! Enter file details to begin allocation.");
    }

    private void allocateFile() {
        String name = fileNameField.getText();
        String sizeStr = fileSizeField.getText();
        
        if (name.isEmpty() || sizeStr.isEmpty()) return;
        
        try {
            int size = Integer.parseInt(sizeStr);
            List<Integer> freeBlocks = new ArrayList<>();
            for (int i = 0; i < DISK_SIZE; i++) {
                if (!disk[i]) freeBlocks.add(i);
            }

            // Need size + 1 (1 for the Index Block itself)
            if (freeBlocks.size() < size + 1) {
                JOptionPane.showMessageDialog(this, "Error: Not enough disk space!");
                return;
            }

            Collections.shuffle(freeBlocks);

            // Pick a random block to be the Index Block
            int indexBlockAddr = freeBlocks.remove(0);
            disk[indexBlockAddr] = true;
            
            // Pick the next blocks as data blocks
            List<Integer> dataPointers = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int b = freeBlocks.remove(0);
                disk[b] = true;
                dataPointers.add(b);
            }

            directory.put(name, indexBlockAddr); // Store Name -> Index Address in the Directory
            physicalDisk.put(indexBlockAddr, dataPointers); // Store Index Address -> Data Pointers in the "Physical" Disk Map
            
            renderFileSystem();
            fileNameField.setText("");
            fileSizeField.setText("");
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Size must be a number!");
        }
    }

    private void renderFileSystem() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s | %-12s | %-20s\n", "FILE NAME", "INDEX BLOCK ADDR", "DATA POINTERS"));
        sb.append("============================================================\n");
        
        // Store all index and data blocks for the visualization
        Set<Integer> allIndexBlocks = new HashSet<>(directory.values());
        Set<Integer> allDataBlocks = new HashSet<>();
        for (List<Integer> list : physicalDisk.values()) allDataBlocks.addAll(list);

        for (String fileName : directory.keySet()) {
            // Step 1: Get the Index Address from the Directory
            int idxAddr = directory.get(fileName);
            // Step 2: Use that Index Address to find the Pointers
            List<Integer> pointers = physicalDisk.get(idxAddr);
            sb.append(String.format("%-15s | %-12d | %s\n", fileName, idxAddr, pointers.toString()));
        }
        
        sb.append("\nPHYSICAL DISK MAP:\n");
        sb.append("------------------------------------------------------------\n");
        for (int i = 0; i < DISK_SIZE; i++) {
            if (allIndexBlocks.contains(i)) sb.append("[ I ] "); // Index Block
            else if (allDataBlocks.contains(i)) sb.append("[ D ] "); // Data Block
            else sb.append("[ . ] "); // Free Block
            
            if ((i + 1) % 8 == 0) sb.append("\n");
        }
        
        updateDisplay(sb.toString());
    }

    private void updateDisplay(String text) {
        displayArea.setText(text);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IndexedAllocationGUI().setVisible(true));
    }
}