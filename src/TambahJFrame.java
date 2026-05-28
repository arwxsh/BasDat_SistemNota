import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TambahJFrame extends JFrame {
    private JTextField tfNoNota, tfTanggal, tfNamaPel;
    private JComboBox<String> cmbBarang; 
    private JSpinner spnQty;
    private JTable tblSementara;
    private DefaultTableModel modelSementara;
    private JLabel lblTotal;

    public TambahJFrame() {
        buildUI();
        loadCombo();
    }

    private void buildUI() {
        setTitle("Tambah Nota Baru");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(620, 580);
        setLocationRelativeTo(null);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 18, 14, 18));

        root.add(sectionLabel("Data Nota"));
        root.add(Box.createVerticalStrut(6));

        JPanel pHeader = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = defaultGbc();

        tfNoNota  = new JTextField(20);
        tfTanggal = new JTextField(20);   
        tfNamaPel = new JTextField(20);

        addFormRow(pHeader, gbc, 0, "No. Nota :",       tfNoNota);
        addFormRow(pHeader, gbc, 1, "Tanggal (YYYY-MM-DD) :", tfTanggal);
        addFormRow(pHeader, gbc, 2, "Nama Pelanggan :",  tfNamaPel);

        root.add(pHeader);
        root.add(Box.createVerticalStrut(12));

        root.add(sectionLabel("Tambah Barang"));
        root.add(Box.createVerticalStrut(6));

        cmbBarang = new JComboBox<>();
        spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        JButton btnTambahBarang = new JButton("Tambah ke Daftar");
        btnTambahBarang.addActionListener(e -> tambahBarangKeTabel());

        JPanel pBarang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pBarang.add(new JLabel("Barang :"));
        pBarang.add(cmbBarang);
        pBarang.add(new JLabel("Qty :"));
        pBarang.add(spnQty);
        pBarang.add(btnTambahBarang);

        root.add(pBarang);
        root.add(Box.createVerticalStrut(10));

        root.add(sectionLabel("Daftar Barang (sementara)"));
        root.add(Box.createVerticalStrut(4));

        modelSementara = new DefaultTableModel(new String[]{"Kode Barang", "Nama Barang", "Qty", "Harga Satuan", "Subtotal"}, 0) {@Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSementara = new JTable(modelSementara);
        tblSementara.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSementara.setRowHeight(22);
        tblSementara.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(tblSementara);
        scroll.setPreferredSize(new Dimension(560, 180));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        root.add(scroll);
        root.add(Box.createVerticalStrut(6));

        JButton btnHapusBaris = new JButton("Hapus Baris Terpilih");
        btnHapusBaris.setForeground(new Color(160, 20, 20));
        btnHapusBaris.addActionListener(e -> hapusBarisTemporary());

        lblTotal = new JLabel("Total : Rp 0");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 13f));

        JPanel pMid = new JPanel(new BorderLayout());
        pMid.add(btnHapusBaris, BorderLayout.WEST);
        pMid.add(lblTotal,      BorderLayout.EAST);
        root.add(pMid);
        root.add(Box.createVerticalStrut(14));

        JButton btnSimpan = new JButton("Simpan Nota");
        btnSimpan.setBackground(new Color(34, 139, 34));
        btnSimpan.setForeground(Color.WHITE);
        btnSimpan.setFont(btnSimpan.getFont().deriveFont(Font.BOLD));
        btnSimpan.addActionListener(e -> simpanNota());

        JPanel pBot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBot.add(btnSimpan);
        root.add(pBot);

        add(new JScrollPane(root));
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

    private void tambahBarangKeTabel() {
        String selected = (String) cmbBarang.getSelectedItem();
        if (selected == null) return;

        String kode = selected.split(";")[0];
        String nama = selected.split(";")[1];
        int qty     = (int) spnQty.getValue();

        for (int i = 0; i < modelSementara.getRowCount(); i++) {
            if (modelSementara.getValueAt(i, 0).toString().equals(kode)) {
                int qtyLama = (int) modelSementara.getValueAt(i, 2);
                BigDecimal harga = (BigDecimal) modelSementara.getValueAt(i, 3);
                int qtyBaru = qtyLama + qty;
                modelSementara.setValueAt(qtyBaru, i, 2);
                modelSementara.setValueAt(harga.multiply(BigDecimal.valueOf(qtyBaru)), i, 4);
                updateTotal();
                return;
            }
        }

        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT Harga_Satuan FROM Barang WHERE Kode_Barang = ?")) {

            ps.setString(1, kode);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            BigDecimal harga    = rs.getBigDecimal("Harga_Satuan");
            BigDecimal subtotal = harga.multiply(BigDecimal.valueOf(qty));

            modelSementara.addRow(new Object[]{ kode, nama, qty, harga, subtotal });
            updateTotal();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void hapusBarisTemporary() {
        int row = tblSementara.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih barang yang ingin dihapus.");
            return;
        }
        modelSementara.removeRow(row);
        updateTotal();
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < modelSementara.getRowCount(); i++)
            total = total.add((BigDecimal) modelSementara.getValueAt(i, 4));
        lblTotal.setText("Total : Rp " + total.toPlainString());
    }

    private void simpanNota() {
        String noNota  = tfNoNota.getText().trim();
        String tanggal = tfTanggal.getText().trim();
        String namaPel = tfNamaPel.getText().trim();

        if (noNota.isEmpty() || tanggal.isEmpty() || namaPel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No. Nota, Tanggal, dan Nama Pelanggan tidak boleh kosong.",
                "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (modelSementara.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Tambahkan minimal satu barang sebelum menyimpan nota.",
                "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Object[]> details = new ArrayList<>();
        BigDecimal totalHarga  = BigDecimal.ZERO;
        for (int i = 0; i < modelSementara.getRowCount(); i++) {
            String     kode     = modelSementara.getValueAt(i, 0).toString();
            int        qty      = (int) modelSementara.getValueAt(i, 2);
            BigDecimal harga    = (BigDecimal) modelSementara.getValueAt(i, 3);
            BigDecimal subtotal = (BigDecimal) modelSementara.getValueAt(i, 4);
            details.add(new Object[]{ kode, qty, harga });
            totalHarga = totalHarga.add(subtotal);
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // transaksi

            try (PreparedStatement psNota = conn.prepareStatement("INSERT INTO Nota (No_Nota, Tanggal, Nama_Pelanggan, Total_Harga) " +"VALUES (?, ?, ?, ?)")) {
                psNota.setString(1, noNota);
                psNota.setString(2, tanggal);
                psNota.setString(3, namaPel);
                psNota.setBigDecimal(4, totalHarga);
                psNota.executeUpdate();
            }

            try (PreparedStatement psDetail = conn.prepareStatement(
                    "INSERT INTO Detail_Nota (No_Nota, Kode_Barang, Qty, Harga_Saat_Ini) " +
                    "VALUES (?, ?, ?, ?)")) {
                for (Object[] d : details) {
                    psDetail.setString(1, noNota);
                    psDetail.setString(2, (String) d[0]);
                    psDetail.setInt(3, (int) d[1]);
                    psDetail.setBigDecimal(4, (BigDecimal) d[2]);
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this,
                "Nota \"" + noNota + "\" berhasil disimpan.",
                "Sukses", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (SQLException ex) {
            showError(ex);
        }
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
