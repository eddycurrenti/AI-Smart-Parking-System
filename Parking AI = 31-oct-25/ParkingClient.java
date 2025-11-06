// ============================================================
// ParkingClient.java — AI Smart Parking Client (Readable Dialogs Edition)
// ============================================================

import com.google.gson.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class ParkingClient extends JFrame {
    private static final String BASE_URL = "http://127.0.0.1:5000";
    private DefaultListModel<SpotEntry> listModel = new DefaultListModel<>();
    private JList<SpotEntry> list;
    private JLabel imgLabel, statusLabel, banner, summary;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ParkingClient() {
        super("AI Smart Parking Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // === Colors ===
        Color darkBg = new Color(30, 34, 40);
        Color panelDark = new Color(40, 44, 52);
        Color lightText = new Color(230, 230, 230);
        Color accentGreen = new Color(80, 200, 120);
        Color accentRed = new Color(230, 100, 100);
        Color accentBlue = new Color(70, 140, 200);
        Color lightBg = new Color(240, 240, 240);
        Color darkText = new Color(30, 30, 30);

        // === Apply global UI tweaks ===
        UIManager.put("Panel.background", panelDark);
        UIManager.put("List.background", darkBg);
        UIManager.put("List.foreground", lightText);
        UIManager.put("Label.foreground", lightText);
        UIManager.put("Button.background", new Color(70, 80, 90));
        UIManager.put("Button.foreground", lightText);
        UIManager.put("ScrollPane.border", new LineBorder(new Color(70, 80, 90)));

        // === Fix JOptionPane dark theme text colors ===
        UIManager.put("OptionPane.background", darkBg);
        UIManager.put("Panel.background", darkBg);
        UIManager.put("OptionPane.messageForeground", lightText);
        UIManager.put("OptionPane.foreground", lightText);
        UIManager.put("OptionPane.border", new LineBorder(Color.DARK_GRAY, 1));
        UIManager.put("TextField.background", new Color(50, 55, 65));
        UIManager.put("TextField.foreground", lightText);
        UIManager.put("ComboBox.background", new Color(50, 55, 65));
        UIManager.put("ComboBox.foreground", lightText);
        UIManager.put("Button.background", new Color(70, 80, 90));
        UIManager.put("Button.foreground", lightText);
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 13));

        // === TOP BAR ===
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(10, 15, 10, 15));
        top.setBackground(lightBg);

        summary = new JLabel("Free: 0   Occupied: 0");
        summary.setFont(new Font("Segoe UI", Font.BOLD, 16));
        summary.setForeground(darkText);

        banner = new JLabel("PARKING LOT FULL", SwingConstants.CENTER);
        banner.setFont(new Font("Segoe UI", Font.BOLD, 18));
        banner.setForeground(accentRed);
        banner.setVisible(false);

        top.add(summary, BorderLayout.WEST);
        top.add(banner, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // === LEFT PANEL ===
        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setPreferredSize(new Dimension(330, 0));
        left.setBorder(new EmptyBorder(10, 10, 10, 10));
        left.setBackground(panelDark);

        JLabel listTitle = new JLabel("Parking Spots");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        listTitle.setForeground(lightText);
        left.add(listTitle, BorderLayout.NORTH);

        list = new JList<>(listModel);
        list.setFont(new Font("Consolas", Font.PLAIN, 14));
        list.setCellRenderer(new SpotRenderer());
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(new LineBorder(new Color(70, 80, 90)));
        left.add(scroll, BorderLayout.CENTER);

        // === CONTROL BUTTONS ===
        JPanel controls = new JPanel(new GridLayout(4, 1, 8, 8));
        controls.setBackground(panelDark);

        JButton ask = createButton("Ask (Recommend)", accentBlue);
        JTextField idField = new JTextField();
        idField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        idField.setBackground(new Color(50, 55, 65));
        idField.setForeground(lightText);
        idField.setBorder(new LineBorder(new Color(80, 90, 100), 1, true));
        JButton occupy = createButton("Occupy", accentGreen);
        JButton free = createButton("Free", accentRed);

        controls.add(ask);
        controls.add(idField);
        controls.add(occupy);
        controls.add(free);
        left.add(controls, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        // === RIGHT PANEL (Image View) ===
        imgLabel = new JLabel("Waiting for camera feed...", SwingConstants.CENTER);
        imgLabel.setForeground(lightText);
        imgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        JScrollPane imgScroll = new JScrollPane(imgLabel);
        imgScroll.setBorder(null);
        add(imgScroll, BorderLayout.CENTER);

        // === STATUS BAR ===
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Color.BLACK);
        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 13));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // === Actions ===
        ask.addActionListener(e -> new Thread(() -> showCarTypeDialog()).start());
        occupy.addActionListener(e -> updateSpot(idField.getText().trim(), true));
        free.addActionListener(e -> updateSpot(idField.getText().trim(), false));

        new Timer(5000, e -> loadStatus()).start();
        setVisible(true);
        loadStatus();
    }

    // === Create Buttons ===
    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(new EmptyBorder(8, 8, 8, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new LineBorder(color.darker(), 1, true));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private void showCarTypeDialog() {
        String[] options = {"Compact / Small Car", "SUV / Large Car", "MUV / Van", "Sedan / Medium Car", "Luxury / Premium"};
        UIManager.put("OptionPane.background", new Color(30, 34, 40));
        UIManager.put("Panel.background", new Color(30, 34, 40));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("ComboBox.background", new Color(40, 44, 52));
        UIManager.put("ComboBox.foreground", Color.WHITE);
        UIManager.put("Button.background", new Color(70, 80, 90));
        UIManager.put("Button.foreground", Color.WHITE);

        String choice = (String) JOptionPane.showInputDialog(
                this, "Select Car Type:", "Car Type Recommendation",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) return;

        String type = switch (choice.split(" ")[0].toLowerCase()) {
            case "compact" -> "compact";
            case "suv" -> "suv";
            case "muv" -> "muv";
            case "sedan" -> "sedan";
            default -> "luxury";
        };
        askRecommend(type);
    }

    private void askRecommend(String type) {
        try {
            JsonObject rec = getJson(BASE_URL + "/recommend?type=" + type);
            if (rec.has("message")) {
                JOptionPane.showMessageDialog(this, rec.get("message").getAsString());
                return;
            }
            String id = rec.get("id").getAsString();
            String ctype = rec.get("car_type").getAsString();

            UIManager.put("OptionPane.background", new Color(30, 34, 40));
            UIManager.put("Panel.background", new Color(30, 34, 40));
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            UIManager.put("Button.background", new Color(70, 80, 90));
            UIManager.put("Button.foreground", Color.WHITE);

            JOptionPane.showMessageDialog(this,
                    "Recommended: " + id + "\nType: " + ctype,
                    "Recommendation", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateSpot(String id, boolean occupy) {
        if (id.equalsIgnoreCase("spot_1") && occupy) {
            JOptionPane.showMessageDialog(this, "spot_1 is reserved for handicapped drivers only!");
            return;
        }
        try {
            JsonObject p = new JsonObject();
            p.addProperty("id", id);
            p.addProperty("confirm", occupy);
            postJson(BASE_URL + "/confirm", p);
            loadStatus();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void loadStatus() {
        try {
            JsonObject d = getJson(BASE_URL + "/status");
            listModel.clear();
            int free = 0, occ = 0;
            for (JsonElement el : d.getAsJsonArray("spots")) {
                JsonObject s = el.getAsJsonObject();
                String id = s.get("id").getAsString();
                String st = s.get("state").getAsString();
                listModel.addElement(new SpotEntry(id, st, id.equalsIgnoreCase("spot_1")));
                if (st.equalsIgnoreCase("free")) free++; else occ++;
            }
            summary.setText("Free: " + free + "   Occupied: " + occ);
            banner.setVisible(d.has("is_full") && d.get("is_full").getAsBoolean());
            if (d.has("image")) {
                byte[] b = Base64.getDecoder().decode(d.get("image").getAsString());
                Image img = ImageIO.read(new ByteArrayInputStream(b));
                imgLabel.setIcon(new ImageIcon(img.getScaledInstance(800, -1, Image.SCALE_SMOOTH)));
            }
            statusLabel.setText("Updated at " + new java.util.Date().toString());
        } catch (Exception e) {
            statusLabel.setText("Failed: " + e.getMessage());
        }
    }

    private JsonObject getJson(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.connect();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return gson.fromJson(sb.toString(), JsonObject.class);
        }
    }

    private void postJson(String url, JsonObject payload) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) {
            os.write(gson.toJson(payload).getBytes());
        }
        c.getResponseCode();
    }

    static class SpotEntry {
        String id, state; boolean handicap;
        SpotEntry(String i, String s, boolean h){ id=i; state=s; handicap=h; }
        public String toString(){ return id + " - " + state.toUpperCase(); }
    }

    static class SpotRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
            JLabel L=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);
            if(v instanceof SpotEntry e){
                if(e.handicap)L.setForeground(new Color(80,150,255));
                else if(e.state.equalsIgnoreCase("free"))L.setForeground(new Color(80,220,120));
                else L.setForeground(new Color(255,100,100));
                L.setBackground(s ? new Color(50, 55, 65) : new Color(30,34,40));
            }
            return L;
        }
    }

    public static void main(String[] a){ SwingUtilities.invokeLater(ParkingClient::new); }
}
