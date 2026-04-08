package index_file_allocation;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class IndexedAllocationGUI extends JFrame {
    private final int DISK_SIZE = 64; // Small disk for simulation
    private boolean[] disk = new boolean[DISK_SIZE];
    private Map<String, List<Integer>> fileSystem = new HashMap<>();
    private Map<String, Integer> indexBlocks = new HashMap<>();
    
    private JTextArea displayArea;
    private JTextField fileNameField, fileSizeField;

    public IndexedAllocationGUI() {
        setTitle("Indexed File Allocation Simulator");
        setSize(700, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Create New File"));
        
        inputPanel.add(new JLabel("File Name:"));
        fileNameField = new JTextField();
        inputPanel.add(fileNameField);
        
        inputPanel.add(new JLabel("Number of Blocks:"));
        fileSizeField = new JTextField();
        inputPanel.add(fileSizeField);
        
        JButton createBtn = new JButton("Allocate File");
        createBtn.addActionListener(e -> allocateFile());
        inputPanel.add(createBtn);

        // Display Area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);
        
        updateDisplay("Welcome! Create a file to see Indexed Allocation in action.");
    }

    private void allocateFile() {
        String name = fileNameField.getText();
        String sizeStr = fileSizeField.getText();
        
        if (name.isEmpty() || sizeStr.isEmpty()) return;
        
        int size = Integer.parseInt(sizeStr);
        List<Integer> freeBlocks = new ArrayList<>();
        
        for (int i = 0; i < DISK_SIZE; i++) {
            if (!disk[i]) freeBlocks.add(i);
        }

        // Need size + 1 (1 for the Index Block itself)
        if (freeBlocks.size() < size + 1) {
            JOptionPane.showMessageDialog(this, "Not enough disk space!");
            return;
        }

        Collections.shuffle(freeBlocks);
        
        // Pick the first shuffled block as the Index Block
        int idxBlock = freeBlocks.remove(0);
        disk[idxBlock] = true;
        
        // Pick the next 'size' blocks as data blocks
        List<Integer> dataBlocks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int b = freeBlocks.remove(0);
            disk[b] = true;
            dataBlocks.add(b);
        }

        fileSystem.put(name, dataBlocks);
        indexBlocks.put(name, idxBlock);
        
        renderFileSystem();
    }

    private void renderFileSystem() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s | %-12s | %-20s\n", "File Name", "Index Block", "Data Blocks"));
        sb.append("------------------------------------------------------------\n");
        
        for (String name : fileSystem.keySet()) {
            sb.append(String.format("%-15s | %-12d | %s\n", 
                name, indexBlocks.get(name), fileSystem.get(name).toString()));
        }
        
        sb.append("\nDisk Visualization ([X]=Used, [.]=Free):\n");
        for (int i = 0; i < DISK_SIZE; i++) {
            sb.append(disk[i] ? "[X]" : "[.]");
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