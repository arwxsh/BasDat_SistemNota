import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;

public class UpdateJFrame extends JFrame {
    private final String noNota;
    private JTextField tfNoNota, tfTanggal, tfNamaPel;
    private JComboBox<String> cmbBarang;
    private JSpinner spnQty;
    private JTable tblDetail;
    private DefaultTableModel modelDetail;
    private JLabel lblTotal;

    public UpdateJFrame(String noNota) {
        this.noNota = noNota;
        buildUI();
        loadHeader();
        loadCombo();
        loadDetail();
    }

    private void buildUI() {
        setTitle("Detail / Edit Nota");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(650, 600);
        setLocationRelativeTo(null);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 18, 14, 18));

        root.add(sectionLabel("Data Nota"));
        root.add(Box.createVerticalStrut(6));

        JPanel pHeader = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = defaultGbc();

        tfNoNota  = new JTextField(20);
        tfNoNota.setEditable(false);
        tfNoNota.setBackground(new Color(230, 230, 230));

        tfTanggal = new JTextField(20);
        tfNamaPel = new JTextField(20);

        addFormRow(pHeader, gbc, 0, "No. Nota :",tfNoNota);
        addFormRow(pHeader, gbc, 1, "Tanggal (YYYY-MM-DD) :",tfTanggal);
        addFormRow(pHeader, gbc, 2, "Nama Pelanggan :",tfNamaPel);

        root.add(pHeader);
        root.add(Box.createVerticalStrut(12));

        root.add(sectionLabel("Tambah Barang ke Nota"));
        root.add(Box.createVerticalStrut(6));

        cmbBarang = new JComboBox<>();
        spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        JButton btnTambahBarang = new JButton("Tambah Barang");
        btnTambahBarang.addActionListener(e -> tambahBarang());

        JPanel pBarang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pBarang.add(new JLabel("Barang :"));
        pBarang.add(cmbBarang);
        pBarang.add(new JLabel("Qty :"));
        pBarang.add(spnQty);
        pBarang.add(btnTambahBarang);

        root.add(pBarang);
        root.add(Box.createVerticalStrut(10));

        root.add(sectionLabel("Daftar Barang dalam Nota"));
        root.add(Box.createVerticalStrut(4));

        modelDetail = new DefaultTableModel( new String[]{"ID Detail", "Nama Barang", "Qty", "Harga Satuan", "Subtotal"}, 0) {@Override public boolean isCellEditable(int r, int c) { return false; } };
        tblDetail = new JTable(modelDetail);
        tblDetail.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDetail.setRowHeight(22);
        tblDetail.getTableHeader().setReorderingAllowed(false);
        tblDetail.getColumnModel().getColumn(0).setMinWidth(0);
        tblDetail.getColumnModel().getColumn(0).setMaxWidth(0);
        tblDetail.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scroll = new JScrollPane(tblDetail);
        scroll.setPreferredSize(new Dimension(580, 180));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        root.add(scroll);
        root.add(Box.createVerticalStrut(6));

        JButton btnHapusBarang = new JButton("Hapus Barang Terpilih");
        btnHapusBarang.setForeground(new Color(160, 20, 20));
        btnHapusBarang.addActionListener(e -> hapusBarang());

        lblTotal = new JLabel("Total : Rp 0");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 13f));

        JPanel pMid = new JPanel(new BorderLayout());
        pMid.add(btnHapusBarang,BorderLayout.WEST);
        pMid.add(lblTotal,BorderLayout.EAST);
        root.add(pMid);
        root.add(Box.createVerticalStrut(14));

        JButton btnSimpan = new JButton("Simpan Perubahan");
        btnSimpan.setBackground(new Color(34, 139, 34));
        btnSimpan.setForeground(Color.WHITE);
        btnSimpan.setFont(btnSimpan.getFont().deriveFont(Font.BOLD));
        btnSimpan.addActionListener(e -> simpanHeader());

        JPanel pBot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBot.add(btnSimpan);
        root.add(pBot);

        add(new JScrollPane(root));
    }

    private void loadHeader() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT No_Nota, Tanggal, Nama_Pelanggan, Total_Harga FROM Nota WHERE No_Nota = ?")) {

            ps.setString(1, noNota);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tfNoNota .setText(rs.getString("No_Nota"));
                tfTanggal.setText(rs.getString("Tanggal"));
                tfNamaPel.setText(rs.getString("Nama_Pelanggan"));
                lblTotal .setText("Total : Rp " + rs.getBigDecimal("Total_Harga").toPlainString());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadCombo() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT Kode_Barang, Nama_Barang FROM Barang ORDER BY Nama_Barang")) {

            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            while (rs.next())
                m.addElement(rs.getString("Kode_Barang") + ";" + rs.getString("Nama_Barang"));
            cmbBarang.setModel(m);

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadDetail() {
        modelDetail.setRowCount(0);
        String sql =
            "SELECT dn.ID_Detail, b.Nama_Barang, dn.Qty, dn.Harga_Saat_Ini, dn.Subtotal " +
            "FROM Detail_Nota dn " +
            "JOIN Barang b ON b.Kode_Barang = dn.Kode_Barang " +
            "WHERE dn.No_Nota = ? " +
            "ORDER BY dn.ID_Detail";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, noNota);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelDetail.addRow(new Object[]{
                    rs.getInt("ID_Detail"),
                    rs.getString("Nama_Barang"),
                    rs.getInt("Qty"),
                    rs.getBigDecimal("Harga_Saat_Ini"),
                    rs.getBigDecimal("Subtotal")
                });
            }
            refreshTotalLabel();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void tambahBarang() {
        String selected = (String) cmbBarang.getSelectedItem();
        if (selected == null) return;

        String kode = selected.split(";")[0];
        int qty  = (int) spnQty.getValue();

        try (Connection conn = DBConnection.getConnection()) {
            BigDecimal harga;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Harga_Satuan FROM Barang WHERE Kode_Barang = ?")) {
                ps.setString(1, kode);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return;
                harga = rs.getBigDecimal("Harga_Satuan");
            }

            try (PreparedStatement psCek = conn.prepareStatement(
                    "SELECT ID_Detail, Qty FROM Detail_Nota WHERE No_Nota = ? AND Kode_Barang = ?")) {
                psCek.setString(1, noNota);
                psCek.setString(2, kode);
                ResultSet rsCek = psCek.executeQuery();

                if (rsCek.next()) {
                    int qtyBaru = rsCek.getInt("Qty") + qty;
                    try (PreparedStatement psUpd = conn.prepareStatement("UPDATE Detail_Nota SET Qty = ? WHERE ID_Detail = ?")) {
                        psUpd.setInt(1, qtyBaru);
                        psUpd.setInt(2, rsCek.getInt("ID_Detail"));
                        psUpd.executeUpdate();
                    }
                } else {

                    try (PreparedStatement psIns = conn.prepareStatement( "INSERT INTO Detail_Nota (No_Nota, Kode_Barang, Qty, Harga_Saat_Ini) " +"VALUES (?, ?, ?, ?)")) {
                        psIns.setString(1, noNota);
                        psIns.setString(2, kode);
                        psIns.setInt(3, qty);
                        psIns.setBigDecimal(4, harga);
                        psIns.executeUpdate();
                    }
                }
            }

            recalcTotal(conn);
            loadDetail();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void hapusBarang() {
        int row = tblDetail.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Pilih barang yang ingin dihapus terlebih dahulu.",
                "Perhatian", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String namaBarang = modelDetail.getValueAt(row, 1).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hapus barang \"" + namaBarang + "\" dari nota ini?",
            "Konfirmasi Hapus Barang",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        int idDetail = (int) modelDetail.getValueAt(row, 0);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM Detail_Nota WHERE ID_Detail = ?")) {

            ps.setInt(1, idDetail);
            ps.executeUpdate();
            recalcTotal(conn);
            loadDetail();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void simpanHeader() {
        String tanggal = tfTanggal.getText().trim();
        String namaPel = tfNamaPel.getText().trim();

        if (tanggal.isEmpty() || namaPel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Tanggal dan Nama Pelanggan tidak boleh kosong.",
                "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE Nota SET Tanggal = ?, Nama_Pelanggan = ? WHERE No_Nota = ?")) {

            ps.setString(1, tanggal);
            ps.setString(2, namaPel);
            ps.setString(3, noNota);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "Nota berhasil diperbarui.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (SQLException ex) {
            showError(ex);
        }
    }
    private void recalcTotal(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Nota SET Total_Harga = " +
                "(SELECT ISNULL(SUM(Subtotal), 0) FROM Detail_Nota WHERE No_Nota = ?) " +
                "WHERE No_Nota = ?")) {
            ps.setString(1, noNota);
            ps.setString(2, noNota);
            ps.executeUpdate();
        }
    }

    private void refreshTotalLabel() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < modelDetail.getRowCount(); i++)
            total = total.add((BigDecimal) modelDetail.getValueAt(i, 4));
        lblTotal.setText("Total : Rp " + total.toPlainString());
    }
    private JPanel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setForeground(new Color(50, 80, 140));
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(lbl);
        return wrapper;
    }

    private GridBagConstraints defaultGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        return g;
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1.0;
        p.add(field, g);
        g.weightx = 0;
    }
    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
            "Database error:\n" + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}
