import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;

public class MainJFrame extends JFrame {

    private JTextField tfCari;
    private JButton btnCari, btnReset;
    private JTable table;
    private JButton btnTambah, btnEdit, btnHapus;
    private JLabel lblStatus;
    public MainJFrame() {
        buildUI();
        loadTable(null);
    }

    private void buildUI() {
        setTitle("Sistem Manajemen Nota");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(780, 480);
        setLocationRelativeTo(null);

        JLabel lblCari = new JLabel("Cari No. Nota :");
        tfCari   = new JTextField(25);
        btnCari  = new JButton("Cari");
        btnReset = new JButton("Reset");

        btnCari .addActionListener(e -> loadTable(tfCari.getText().trim()));
        btnReset.addActionListener(e -> { tfCari.setText(""); loadTable(null); });
        tfCari.addActionListener(e -> loadTable(tfCari.getText().trim()));

        JPanel pTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pTop.setBorder(new EmptyBorder(6, 12, 0, 12));
        pTop.add(lblCari);
        pTop.add(tfCari);
        pTop.add(btnCari);
        pTop.add(btnReset);

        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(4, 12, 4, 12));

        btnTambah = new JButton("Tambah");
        btnEdit = new JButton("Detail / Edit");
        btnHapus = new JButton("Hapus");
        lblStatus = new JLabel(" ");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.ITALIC));
        lblStatus.setForeground(Color.GRAY);

        btnHapus.setForeground(new Color(180, 30, 30));

        btnTambah.addActionListener(e -> bukaFormTambah());
        btnEdit.addActionListener(e -> bukaFormEdit());
        btnHapus.addActionListener(e -> hapusNota());

        JPanel pBot = new JPanel(new BorderLayout());
        pBot.setBorder(new EmptyBorder(4, 12, 10, 12));

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pBtns.add(btnTambah);
        pBtns.add(btnEdit);
        pBtns.add(btnHapus);

        pBot.add(pBtns,BorderLayout.WEST);
        pBot.add(lblStatus,BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(pTop,BorderLayout.NORTH);
        add(scroll,BorderLayout.CENTER);
        add(pBot,BorderLayout.SOUTH);
    }

    private void loadTable(String filter) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"No. Nota", "Tanggal", "Nama Pelanggan", "Total Harga (Rp)"}, 0) {@Override public boolean isCellEditable(int r, int c) { return false; }};

        String sql = (filter == null || filter.isEmpty())
            ? "SELECT No_Nota, Tanggal, Nama_Pelanggan, Total_Harga FROM Nota ORDER BY Tanggal DESC, No_Nota"
            : "SELECT No_Nota, Tanggal, Nama_Pelanggan, Total_Harga FROM Nota " +
              "WHERE No_Nota LIKE ? ORDER BY Tanggal DESC, No_Nota";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filter != null && !filter.isEmpty())
                ps.setString(1, "%" + filter + "%");

            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("No_Nota"),
                    rs.getString("Tanggal"),
                    rs.getString("Nama_Pelanggan"),
                    rs.getBigDecimal("Total_Harga")
                });
                count++;
            }

            DefaultTableCellRenderer rRight = new DefaultTableCellRenderer();
            rRight.setHorizontalAlignment(SwingConstants.RIGHT);
            table.setModel(model);
            table.getColumnModel().getColumn(3).setCellRenderer(rRight);
            lblStatus.setText(count + " nota ditemukan");

            if (filter != null && !filter.isEmpty() && count == 0)
                JOptionPane.showMessageDialog(this,
                    "Nota dengan nomor \"" + filter + "\" tidak ditemukan.",
                    "Tidak Ditemukan", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void bukaFormTambah() {
        TambahJFrame form = new TambahJFrame();
        form.setVisible(true);
        form.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                loadTable(tfCari.getText().trim().isEmpty() ? null : tfCari.getText().trim());
            }
        });
    }

    private void bukaFormEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Pilih nota yang ingin diedit terlebih dahulu.",
                "Perhatian", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String noNota = table.getValueAt(row, 0).toString();
        UpdateJFrame form = new UpdateJFrame(noNota);
        form.setVisible(true);
        form.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                loadTable(tfCari.getText().trim().isEmpty() ? null : tfCari.getText().trim());
            }
        });
    }

    private void hapusNota() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Pilih nota yang ingin dihapus terlebih dahulu.",
                "Perhatian", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String noNota = table.getValueAt(row, 0).toString();
        String tanggal = table.getValueAt(row, 1).toString();
        String pelanggan = table.getValueAt(row, 2).toString();
        String total = table.getValueAt(row, 3).toString();

        String pesan =
            "<html><b>Hapus nota berikut?</b><br><br>" +
            "<table cellpadding='3'>" +
            "<tr><td>No. Nota</td><td>&nbsp;<b>" +noNota+ "</b></td></tr>" +
            "<tr><td>Tanggal</td><td>&nbsp;"+ tanggal+ "</td></tr>" +
            "<tr><td>Pelanggan</td><td>&nbsp;"+ pelanggan+ "</td></tr>" +
            "<tr><td>Total</td><td>&nbsp;Rp "+ total+ "</td></tr>" +
            "</table><br>" +
            "<font color='red'>Semua detail barang pada nota ini juga akan dihapus.</font>" +
            "</html>";

        int pilih = JOptionPane.showConfirmDialog(this,
            pesan, "Konfirmasi Hapus Nota",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (pilih != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM Nota WHERE No_Nota = ?")) {

            ps.setString(1, noNota);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "Nota \"" + noNota + "\" berhasil dihapus.",
                "Sukses", JOptionPane.INFORMATION_MESSAGE);

            loadTable(tfCari.getText().trim().isEmpty() ? null : tfCari.getText().trim());

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
            "Database error:\n" + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainJFrame().setVisible(true));
    }
}
