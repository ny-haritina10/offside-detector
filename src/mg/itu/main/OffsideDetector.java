package mg.itu.main;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class OffsideDetector extends JFrame {

    private BufferedImage currentImage;
    private JPanel imagePanel;
    private List<Point> redTeam = new ArrayList<>();
    private List<Point> blueTeam = new ArrayList<>();
    private Point ball;
    private int redDefenderLine = -1;
    private int blueDefenderLine = -1;
    private Set<Point> offsidePlayers = new HashSet<>();
    private JRadioButton redPossessionButton;
    private JRadioButton bluePossessionButton;
    
    public OffsideDetector() {
        setTitle("Football Offside Detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeUI();
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // Create main panel
        imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentImage != null) {
                    g.drawImage(currentImage, 0, 0, this.getWidth(), this.getHeight(), null);
                    drawAnalysis(g);
                }
            }
        };
        imagePanel.setPreferredSize(new Dimension(800, 600));
        
        // Create control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        
        // Create possession control panel
        JPanel possessionPanel = new JPanel();
        possessionPanel.setBorder(BorderFactory.createTitledBorder("Ball Possession"));
        redPossessionButton = new JRadioButton("Red Team");
        bluePossessionButton = new JRadioButton("Blue Team");
        
        // Group the radio buttons
        ButtonGroup possessionGroup = new ButtonGroup();
        possessionGroup.add(redPossessionButton);
        possessionGroup.add(bluePossessionButton);
        
        // Select red team by default
        redPossessionButton.setSelected(true);
        
        possessionPanel.add(redPossessionButton);
        possessionPanel.add(bluePossessionButton);
        
        // Create buttons
        JButton loadButton = new JButton("Load Image");
        JButton analyzeButton = new JButton("Analyze Offside");
        
        loadButton.addActionListener(e -> loadImage());
        analyzeButton.addActionListener(e -> analyzeOffside());
        
        controlPanel.add(possessionPanel);
        controlPanel.add(loadButton);
        controlPanel.add(analyzeButton);
        
        add(imagePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Image files", "jpg", "jpeg", "png", "gif");
        chooser.setFileFilter(filter);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                currentImage = javax.imageio.ImageIO.read(chooser.getSelectedFile());
                detectPlayers();
                imagePanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading image: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void detectPlayers() {
        redTeam.clear();
        blueTeam.clear();

        ball = null;
        
        offsidePlayers.clear();
        redDefenderLine = -1;
        blueDefenderLine = -1;
        
        Map<Point, Integer> redPixelCounts = new HashMap<>();
        Map<Point, Integer> bluePixelCounts = new HashMap<>();
        
        // First pass: collect all colored pixels
        for (int y = 0; y < currentImage.getHeight(); y++) {
            for (int x = 0; x < currentImage.getWidth(); x++) {
                Color color = new Color(currentImage.getRGB(x, y));
                
                if (isRed(color)) {
                    Point p = new Point(x, y);
                    redPixelCounts.put(p, redPixelCounts.getOrDefault(p, 0) + 1);
                }
                else if (isBlue(color)) {
                    Point p = new Point(x, y);
                    bluePixelCounts.put(p, bluePixelCounts.getOrDefault(p, 0) + 1);
                }
                else if (isBlack(color)) {
                    ball = new Point(x, y);
                }
            }
        }
        
        // Convert to lists and consolidate
        redTeam = consolidatePoints(new ArrayList<>(redPixelCounts.keySet()));
        blueTeam = consolidatePoints(new ArrayList<>(bluePixelCounts.keySet()));
        
        // Debug output
        System.out.println("Red team count: " + redTeam.size());
        System.out.println("Blue team count: " + blueTeam.size());
    }
    
    private boolean isRed(Color color) {
        return color.getRed() > 200 && 
               color.getGreen() < 100 && 
               color.getBlue() < 100 &&
               (color.getRed() - color.getGreen()) > 100 &&
               (color.getRed() - color.getBlue()) > 100;
    }
    
    private boolean isBlue(Color color) {
        return color.getBlue() > 200 && 
               color.getRed() < 100 && 
               color.getGreen() < 100 &&
               (color.getBlue() - color.getRed()) > 100 &&
               (color.getBlue() - color.getGreen()) > 100;
    }
    
    private boolean isBlack(Color color) {
        return color.getRed() < 50 && color.getGreen() < 50 && color.getBlue() < 50;
    }
    
    private List<Point> consolidatePoints(List<Point> points) {
        List<Point> consolidated = new ArrayList<>();
        Set<Point> processed = new HashSet<>();
        
        // Increase the clustering distance for better player detection
        final int CLUSTER_DISTANCE = 50;  
        
        for (Point p : points) {
            if (!processed.contains(p)) {
                List<Point> cluster = new ArrayList<>();
                // Find all points that belong to this cluster
                for (Point other : points) {
                    if (distance(p, other) < CLUSTER_DISTANCE) {
                        cluster.add(other);
                        processed.add(other);
                    }
                }
                // Only add clusters that are large enough to be players
                if (cluster.size() > 5) {  // Minimum cluster size to be considered a player
                    Point center = calculateCenter(cluster);
                    consolidated.add(center);
                }
            }
        }
        
        // If we still have too many points, take the 11 most significant clusters
        if (consolidated.size() > 11) {
            consolidated.sort((p1, p2) -> {
                // Sort by cluster size (you might need to keep track of cluster sizes)
                return Integer.compare(p2.x, p1.x);  // Simple fallback to position-based sorting
            });
            return consolidated.subList(0, 11);
        }
        
        return consolidated;
    }
    
    private double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }
    
    private Point calculateCenter(List<Point> points) {
        int sumX = 0, sumY = 0;
        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }
        return new Point(sumX / points.size(), sumY / points.size());
    }
    
    private void analyzeOffside() {
        if (currentImage == null || redTeam.isEmpty() || blueTeam.isEmpty() || ball == null) {
            JOptionPane.showMessageDialog(this, 
                "Please load an image and ensure players and ball are detected.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        // Clear previous analysis
        offsidePlayers.clear();
    
        // Determine attacking and defending teams based on radio button selection
        List<Point> attackingTeam;
        List<Point> defendingTeam;
        boolean isRedAttacking = redPossessionButton.isSelected();
    
        if (isRedAttacking) {
            attackingTeam = redTeam;
            defendingTeam = blueTeam;
        } else {
            attackingTeam = blueTeam;
            defendingTeam = redTeam;
        }
    
        // Find the second-last defender's position
        List<Point> sortedDefenders = new ArrayList<>(defendingTeam);
        if (isRedAttacking) {
            // Sort defenders from right to left for red attacking
            sortedDefenders.sort((p1, p2) -> Integer.compare(p2.x, p1.x));
        } else {
            // Sort defenders from left to right for blue attacking
            sortedDefenders.sort((p1, p2) -> Integer.compare(p1.x, p2.x));
        }
    
        // Need at least two defenders to establish offside line
        if (sortedDefenders.size() < 2) {
            JOptionPane.showMessageDialog(this, 
                "Not enough defenders detected to establish offside line.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        // Get the second-last defender's X position
        int offsideLine = sortedDefenders.get(sortedDefenders.size() - 2).x;
    
        // Update defender lines for visualization
        if (isRedAttacking) {
            blueDefenderLine = offsideLine;
            redDefenderLine = -1;  // Clear attacking team's line
        } else {
            redDefenderLine = offsideLine;
            blueDefenderLine = -1;  // Clear attacking team's line
        }
    
        // Check for offside positions
        for (Point attacker : attackingTeam) {
            if (isRedAttacking) {
                // Red attacking right to left
                // < < 
                if (attacker.x < offsideLine && attacker.x < ball.x) {
                    offsidePlayers.add(attacker);
                }
            } else {
                // Blue attacking left to right
                // > > 
                if (attacker.x > offsideLine && attacker.x > ball.x) {
                    offsidePlayers.add(attacker);
                }
            }
        }
    
        // Repaint to show the analysis
        imagePanel.repaint();
    
        // Show results
        String message = String.format("%d player(s) in offside position", offsidePlayers.size());
        JOptionPane.showMessageDialog(this, message, "Offside Analysis", JOptionPane.INFORMATION_MESSAGE);
    }

    private void drawAnalysis(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw players
        // for (Point p : redTeam) {
        //     g2d.setColor(Color.RED);
        //     g2d.setStroke(new BasicStroke(2));
        //     g2d.drawOval(p.x - 5, p.y - 5, 10, 10);
        // }
        
        // for (Point p : blueTeam) {
        //     g2d.setColor(Color.BLUE);
        //     g2d.setStroke(new BasicStroke(2));
        //     g2d.drawOval(p.x - 5, p.y - 5, 10, 10);
        // }
        
        // // Draw ball
        // if (ball != null) {
        //     g2d.setColor(Color.BLACK);
        //     g2d.fillOval(ball.x - 3, ball.y - 3, 6, 6);
        // }
        
        // Draw offside lines if they exist
        if (redDefenderLine >= 0) {
            // Draw red team's offside line
            g2d.setColor(new Color(255, 0, 0, 128));  // Semi-transparent red
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2d.drawLine(redDefenderLine, 0, redDefenderLine, currentImage.getHeight());
        }
        
        if (blueDefenderLine >= 0) {
            // Draw blue team's offside line
            g2d.setColor(new Color(0, 0, 255, 128));  // Semi-transparent blue
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2d.drawLine(blueDefenderLine, 0, blueDefenderLine, currentImage.getHeight());
        }
        
        // Mark offside players
        for (Point p : offsidePlayers) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(3));
            // Draw an X over offside players
            g2d.drawLine(p.x - 8, p.y - 8, p.x + 8, p.y + 8);
            g2d.drawLine(p.x - 8, p.y + 8, p.x + 8, p.y - 8);
        }
    }
        
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new OffsideDetector().setVisible(true);
        });
    }
}