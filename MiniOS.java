import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Resource Manager to track and manage system resources
class ResourceManager {
    private static final int MAX_THREADS = 10;
    private Map<Integer, Thread> runningThreads = new HashMap<>();
    private AtomicInteger threadIdCounter = new AtomicInteger(1);
    private JTextArea logArea;
    
    public ResourceManager(JTextArea logArea) {
        this.logArea = logArea;
    }
    
    public synchronized int startProcess(Thread thread, String processName) {
        if (runningThreads.size() >= MAX_THREADS) {
            log("ERROR: Maximum thread limit reached! Cannot start " + processName);
            return -1;
        }
        
        int threadId = threadIdCounter.getAndIncrement();
        thread.setName(processName + "-" + threadId);
        runningThreads.put(threadId, thread);
        thread.start();
        
        log("Process started: " + thread.getName() + " (ID: " + threadId + ")");
        log("System resources: " + runningThreads.size() + "/" + MAX_THREADS + " threads in use");
        
        return threadId;
    }
    
    public synchronized void terminateProcess(int threadId) {
        Thread thread = runningThreads.get(threadId);
        if (thread != null) {
            thread.interrupt();
            runningThreads.remove(threadId);
            log("Process terminated: " + thread.getName() + " (ID: " + threadId + ")");
            log("System resources: " + runningThreads.size() + "/" + MAX_THREADS + " threads in use");
        }
    }
    
    public synchronized void listRunningProcesses() {
        log("===== RUNNING PROCESSES =====");
        if (runningThreads.isEmpty()) {
            log("No active processes");
        } else {
            for (Map.Entry<Integer, Thread> entry : runningThreads.entrySet()) {
                Thread thread = entry.getValue();
                log("ID: " + entry.getKey() + " | Name: " + thread.getName() + " | State: " + thread.getState());
            }
        }
        log("============================");
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Auto-scroll to bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

// Calculator module
class Calculator {
    public int add(int a, int b) { return a + b; }
    public int sub(int a, int b) { return a - b; }
    public int mul(int a, int b) { return a * b; }
    public int div(int a, int b) {
        if (b == 0) {
            System.out.println("Error: Division by zero!");
            return 0;
        }
        return a / b;
    }
}

// Prime number checker using Calculator
class PrimeCheckerThread extends Thread {
    private JLabel resultLabel;
    private int number;
    private Calculator calculator;

    public PrimeCheckerThread(JLabel resultLabel, int number, Calculator calculator) {
        this.resultLabel = resultLabel;
        this.number = number;
        this.calculator = calculator;
    }

    public void run() {
        try {
            Thread.sleep(500); // Simulate processing time
            
            boolean isPrime = true;
            if (number <= 1) isPrime = false;
            
            for (int i = 2; i <= number / 2; i++) {
                if (Thread.interrupted()) {
                    return; // Check if thread has been interrupted
                }
                
                if (number % i == 0) {
                    isPrime = false;
                    break;
                }
                
                Thread.sleep(10); // Small delay to demonstrate CPU usage
            }
            
            final boolean finalIsPrime = isPrime;
            SwingUtilities.invokeLater(() -> {
                resultLabel.setText(number + (finalIsPrime ? " is Prime" : " is NOT Prime"));
            });
            
        } catch (InterruptedException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + " was interrupted");
        }
    }
}

// Palindrome checker thread
class PalindromeCheckerThread extends Thread {
    private JLabel resultLabel;
    private String input;

    public PalindromeCheckerThread(JLabel resultLabel, String input) {
        this.resultLabel = resultLabel;
        this.input = input;
    }

    public void run() {
        try {
            Thread.sleep(300); // Simulate processing time
            
            String reversed = new StringBuilder(input).reverse().toString();
            boolean isPalindrome = input.equalsIgnoreCase(reversed);
            
            SwingUtilities.invokeLater(() -> {
                resultLabel.setText(input + (isPalindrome ? " is a Palindrome" : " is NOT a Palindrome"));
            });
            
        } catch (InterruptedException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + " was interrupted");
        }
    }
}

// Calculator operation thread
class CalculatorThread extends Thread {
    private JLabel resultLabel;
    private int a, b;
    private String operation;
    private Calculator calculator;

    public CalculatorThread(JLabel resultLabel, int a, int b, String operation, Calculator calculator) {
        this.resultLabel = resultLabel;
        this.a = a;
        this.b = b;
        this.operation = operation;
        this.calculator = calculator;
    }

    public void run() {
        try {
            Thread.sleep(200); // Simulate processing time
            
            int result = 0;
            switch (operation) {
                case "+": result = calculator.add(a, b); break;
                case "-": result = calculator.sub(a, b); break;
                case "*": result = calculator.mul(a, b); break;
                case "/": result = calculator.div(a, b); break;
            }
            
            final int finalResult = result;
            SwingUtilities.invokeLater(() -> {
                resultLabel.setText("Result: " + finalResult);
            });
            
        } catch (InterruptedException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + " was interrupted");
        }
    }
}

public class MiniOS extends JFrame {
    private ResourceManager resourceManager;
    private Calculator calculator;
    private JTextArea logArea;
    
    public MiniOS() {
        calculator = new Calculator();
        
        setTitle("Mini OS");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create split pane for main UI and system log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        
        // Top panel for applications
        JPanel topPanel = new JPanel(new GridLayout(4, 1, 20, 20));
        
        // Bottom panel for system log
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel logLabel = new JLabel("System Log");
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        
        bottomPanel.add(logLabel, BorderLayout.NORTH);
        bottomPanel.add(logScrollPane, BorderLayout.CENTER);
        
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        
        add(splitPane);
        
        // Initialize resource manager
        resourceManager = new ResourceManager(logArea);
        
        // Create system control buttons
        JPanel systemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton processBtn = new JButton("🖥️ View Processes");
        JButton memoryBtn = new JButton("📊 Memory Usage");
        systemPanel.add(processBtn);
        systemPanel.add(memoryBtn);
        
        processBtn.addActionListener(e -> resourceManager.listRunningProcesses());
        memoryBtn.addActionListener(e -> {
            Runtime rt = Runtime.getRuntime();
            long total = rt.totalMemory() / 1024 / 1024;
            long free = rt.freeMemory() / 1024 / 1024;
            long used = total - free;
            logArea.append("===== MEMORY USAGE =====\n");
            logArea.append("Total Memory: " + total + " MB\n");
            logArea.append("Used Memory: " + used + " MB\n");
            logArea.append("Free Memory: " + free + " MB\n");
            logArea.append("=======================\n");
        });
        
        // Create application buttons
        JButton primeBtn = new JButton("🧮 Prime Checker");
        JButton palBtn = new JButton("🔁 Palindrome Checker");
        JButton calcBtn = new JButton("➗ Calculator");
        
        primeBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        palBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        calcBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        
        topPanel.add(systemPanel);
        topPanel.add(primeBtn);
        topPanel.add(palBtn);
        topPanel.add(calcBtn);
        
        primeBtn.addActionListener(e -> openPrimeChecker());
        palBtn.addActionListener(e -> openPalindromeChecker());
        calcBtn.addActionListener(e -> openCalculator());
        
        logArea.append("MiniOS started successfully\n");
        logArea.append("System initialized with max " + 10 + " concurrent threads\n");
    }

    private void openPrimeChecker() {
        JFrame frame = new JFrame("Prime Checker");
        frame.setSize(400, 250);
        frame.setLayout(new FlowLayout());
        frame.setLocationRelativeTo(null);
        
        JTextField numberField = new JTextField(10);
        JButton checkBtn = new JButton("Check");
        JLabel result = new JLabel("                                         ");
        JLabel processIdLabel = new JLabel("Process ID: None");
        JButton killBtn = new JButton("Kill Process");
        killBtn.setEnabled(false);
        
        frame.add(new JLabel("Enter number:"));
        frame.add(numberField);
        frame.add(checkBtn);
        frame.add(result);
        frame.add(processIdLabel);
        frame.add(killBtn);
        
        final int[] currentThreadId = {-1};
        
        checkBtn.addActionListener(e -> {
            try {
                int num = Integer.parseInt(numberField.getText());
                result.setText("Checking if " + num + " is prime...");
                
                // Terminate previous thread if exists
                if (currentThreadId[0] != -1) {
                    resourceManager.terminateProcess(currentThreadId[0]);
                    currentThreadId[0] = -1;
                    killBtn.setEnabled(false);
                }
                
                // Create and start a new thread
                PrimeCheckerThread thread = new PrimeCheckerThread(result, num, calculator);
                int threadId = resourceManager.startProcess(thread, "PrimeChecker");
                
                if (threadId != -1) {
                    currentThreadId[0] = threadId;
                    processIdLabel.setText("Process ID: " + threadId);
                    killBtn.setEnabled(true);
                }
                
            } catch (NumberFormatException ex) {
                result.setText("Invalid input.");
            }
        });
        
        killBtn.addActionListener(e -> {
            if (currentThreadId[0] != -1) {
                resourceManager.terminateProcess(currentThreadId[0]);
                result.setText("Process terminated");
                currentThreadId[0] = -1;
                processIdLabel.setText("Process ID: None");
                killBtn.setEnabled(false);
            }
        });
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentThreadId[0] != -1) {
                    resourceManager.terminateProcess(currentThreadId[0]);
                }
            }
        });
        
        frame.setVisible(true);
    }

    private void openPalindromeChecker() {
        JFrame frame = new JFrame("Palindrome Checker");
        frame.setSize(400, 250);
        frame.setLayout(new FlowLayout());
        frame.setLocationRelativeTo(null);
        
        JTextField textField = new JTextField(15);
        JButton checkBtn = new JButton("Check");
        JLabel result = new JLabel("                                         ");
        JLabel processIdLabel = new JLabel("Process ID: None");
        JButton killBtn = new JButton("Kill Process");
        killBtn.setEnabled(false);
        
        frame.add(new JLabel("Enter string:"));
        frame.add(textField);
        frame.add(checkBtn);
        frame.add(result);
        frame.add(processIdLabel);
        frame.add(killBtn);
        
        final int[] currentThreadId = {-1};
        
        checkBtn.addActionListener(e -> {
            String input = textField.getText();
            result.setText("Checking if " + input + " is a palindrome...");
            
            // Terminate previous thread if exists
            if (currentThreadId[0] != -1) {
                resourceManager.terminateProcess(currentThreadId[0]);
                currentThreadId[0] = -1;
                killBtn.setEnabled(false);
            }
            
            // Create and start a new thread
            PalindromeCheckerThread thread = new PalindromeCheckerThread(result, input);
            int threadId = resourceManager.startProcess(thread, "PalindromeChecker");
            
            if (threadId != -1) {
                currentThreadId[0] = threadId;
                processIdLabel.setText("Process ID: " + threadId);
                killBtn.setEnabled(true);
            }
        });
        
        killBtn.addActionListener(e -> {
            if (currentThreadId[0] != -1) {
                resourceManager.terminateProcess(currentThreadId[0]);
                result.setText("Process terminated");
                currentThreadId[0] = -1;
                processIdLabel.setText("Process ID: None");
                killBtn.setEnabled(false);
            }
        });
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentThreadId[0] != -1) {
                    resourceManager.terminateProcess(currentThreadId[0]);
                }
            }
        });
        
        frame.setVisible(true);
    }

    private void openCalculator() {
        JFrame frame = new JFrame("Calculator");
        frame.setSize(400, 300);
        frame.setLayout(new FlowLayout());
        frame.setLocationRelativeTo(null);
        
        JTextField num1 = new JTextField(8);
        JTextField num2 = new JTextField(8);
        JComboBox<String> opBox = new JComboBox<>(new String[]{"+", "-", "*", "/"});
        JButton calcBtn = new JButton("Calculate");
        JLabel result = new JLabel("                                         ");
        JLabel processIdLabel = new JLabel("Process ID: None");
        JButton killBtn = new JButton("Kill Process");
        killBtn.setEnabled(false);
        
        frame.add(new JLabel("Num 1:")); frame.add(num1);
        frame.add(new JLabel("Operation:")); frame.add(opBox);
        frame.add(new JLabel("Num 2:")); frame.add(num2);
        frame.add(calcBtn);
        frame.add(result);
        frame.add(processIdLabel);
        frame.add(killBtn);
        
        final int[] currentThreadId = {-1};
        
        calcBtn.addActionListener(e -> {
            try {
                int a = Integer.parseInt(num1.getText());
                int b = Integer.parseInt(num2.getText());
                String op = (String) opBox.getSelectedItem();
                
                result.setText("Calculating " + a + " " + op + " " + b + "...");
                
                // Terminate previous thread if exists
                if (currentThreadId[0] != -1) {
                    resourceManager.terminateProcess(currentThreadId[0]);
                    currentThreadId[0] = -1;
                    killBtn.setEnabled(false);
                }
                
                // Create and start a new thread
                CalculatorThread thread = new CalculatorThread(result, a, b, op, calculator);
                int threadId = resourceManager.startProcess(thread, "Calculator");
                
                if (threadId != -1) {
                    currentThreadId[0] = threadId;
                    processIdLabel.setText("Process ID: " + threadId);
                    killBtn.setEnabled(true);
                }
                
            } catch (Exception ex) {
                result.setText("Invalid input.");
            }
        });
        
        killBtn.addActionListener(e -> {
            if (currentThreadId[0] != -1) {
                resourceManager.terminateProcess(currentThreadId[0]);
                result.setText("Process terminated");
                currentThreadId[0] = -1;
                processIdLabel.setText("Process ID: None");
                killBtn.setEnabled(false);
            }
        });
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentThreadId[0] != -1) {
                    resourceManager.terminateProcess(currentThreadId[0]);
                }
            }
        });
        
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MiniOS().setVisible(true));
    }
}