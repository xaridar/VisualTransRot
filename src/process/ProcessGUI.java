package process;

import util.Globals;

import javax.sound.sampled.Port;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProcessGUI extends JFrame {

    private static ProcessGUI Instance;

    public static ProcessGUI getInstance() {
        if (Instance == null) Instance = new ProcessGUI();
        return Instance;
    }

    private List<ProcessStruct> lastPSs = new ArrayList<>();

    private final JTable table;
    private final DefaultTableModel tModel;
    private final JButton endTaskBtn;

    private ProcessGUI() {
        super("Process Monitor");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBackground(Globals.menuBgColorLight);

        tModel = new DefaultTableModel(new String[]{"PID", "Process Name", "Started At", "Elapsed Time", "Status", "Details"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(0, (a, b) -> {
            Long n1 = Long.valueOf(a.toString());
            Long n2 = Long.valueOf(b.toString());
            return n1.compareTo(n2);
        });
        table.setRowSorter(sorter);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.setRowHeight(30);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Globals.settingsFontNoBold);
        table.setGridColor(Globals.accentColorDark);
        table.getTableHeader().setFont(Globals.settingsFont);
        table.getTableHeader().setBackground(Globals.bgColorDark);

        TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Globals.accentColorDark));
            return label;
            }
        });

        TableCellRenderer lastColRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setForeground(Globals.linkColor);
            return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            int finalI = i;
            table.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c;
                if (finalI == table.getColumnCount() - 1) c = lastColRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                else c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                if (isSelected) return c;
                if (lastPSs.get(table.convertRowIndexToModel(row)).getStatus() == ProcessStruct.ProcessStatus.ALIVE) {
                    c.setBackground(Globals.accentColorLight);
                } else {
                    c.setBackground(Globals.bgColor);
                }
                return c;
                }
            });
        }
        endTaskBtn = new JButton("End Task");
        endTaskBtn.addActionListener(e -> {
            if (table.getSelectedRow() == -1) return;
            ProcessStruct ps = lastPSs.get(table.convertRowIndexToModel(table.getSelectedRow()));
            if (ps.getStatus() == ProcessStruct.ProcessStatus.ALIVE) {
                // kills active process
                ps.destroyIfAlive();
            } else {
                // dismisses old process
                try {
                    ProcessManager.getInstance().dismissProcess(ps);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            updateGUI();
        });
        endTaskBtn.setEnabled(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row == -1) return;
                int column = table.columnAtPoint(e.getPoint());
                int modelRow = table.convertRowIndexToModel(row);
                ProcessStruct ps = lastPSs.get(modelRow);
                endTaskBtn.setText(ps.getStatus() == ProcessStruct.ProcessStatus.ALIVE ? "End Task" : "Dismiss Process");
                endTaskBtn.setEnabled(true);
                if (column == table.getColumnCount() - 1) ProcessSubframe.openProcessFrame(ps);
            }
        });

        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
            int column = table.columnAtPoint(e.getPoint());

            if (column == table.getColumnCount() - 1) table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            else table.setCursor(Cursor.getDefaultCursor());
            }
        });

        // hard-coded widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(190);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);
        table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredSize().width, 250));

        JScrollPane sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setColumnHeaderView(table.getTableHeader());
        sp.setBorder(null);
        sp.setOpaque(false);
        contentPane.add(sp);

        JPanel endTaskPanel = new JPanel(new BorderLayout());
        endTaskPanel.setOpaque(false);
        JButton dismissAllBtn = new JButton("Dismiss All Completed Processes");
        dismissAllBtn.addActionListener(e -> {
            lastPSs.stream().filter(ps -> ps.getStatus() != ProcessStruct.ProcessStatus.ALIVE).forEach(ps -> {
                try {
                    ProcessManager.getInstance().dismissProcess(ps);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });
            endTaskBtn.setEnabled(false);
            updateGUI();
        });
        endTaskPanel.add(dismissAllBtn, BorderLayout.WEST);

        endTaskPanel.add(endTaskBtn, BorderLayout.EAST);
        endTaskPanel.setBorder(BorderFactory.createEmptyBorder(24, 12, 12, 12));

        contentPane.add(endTaskPanel);

        setContentPane(contentPane);

        updateGUI();

        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
        setAlwaysOnTop(true);

        // Close window on ESC
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void updateGUI() {
        List<ProcessStruct> processes = new ArrayList<>(ProcessManager.getInstance().getProcesses());
        List<Long> newPIDs = processes.stream().map(ProcessStruct::getPid).collect(Collectors.toList());
        processes.sort((ps1, ps2) -> {
            if (ps1.getStatus() != ps2.getStatus()) {
                return ps1.getStatus().ordinal() - ps2.getStatus().ordinal();
            }
            return -ps1.getStartTime().compareTo(ps2.getStartTime());
        });

        for (int i = 0, outerI = 0; i < processes.size(); i++, outerI++) {
            ProcessStruct ps = processes.get(i);
            if (!lastPSs.stream().map(ProcessStruct::getPid).collect(Collectors.toList()).contains(ps.getPid())) {
                // new PID
                tModel.addRow(new Object[]{ps.getPid(), ps.getName(), ps.getStartStr(), ps.getExecTime(), ps.getStatus(), "<html><u>View logs & other details</u></html>"});
                outerI--;
            } else {
                if (!(tModel.getValueAt(outerI, 0)).equals(ps.getPid())) tModel.setValueAt(ps.getPid(), outerI, 0);
                if (!(tModel.getValueAt(outerI, 1)).equals(ps.getName())) tModel.setValueAt(ps.getName(), outerI, 1);
                if (!(tModel.getValueAt(outerI, 2)).equals(ps.getStartStr())) tModel.setValueAt(ps.getStartStr(), outerI, 2);
                if (!(tModel.getValueAt(outerI, 3)).equals(ps.getExecTime())) {
                    tModel.setValueAt(ps.getExecTime(), outerI, 3);
                }
                if (!(tModel.getValueAt(outerI, 4)).equals(ps.getStatus())) tModel.setValueAt(ps.getStatus(), outerI, 4);
            }
        }
        AtomicInteger c = new AtomicInteger();
        lastPSs.stream().filter(ps -> !newPIDs.contains(ps.getPid())).forEach(ps -> {
            int i = lastPSs.indexOf(ps);
            tModel.removeRow(i - c.get());
            c.getAndIncrement();
        });

        lastPSs = new ArrayList<>(processes);
        table.repaint();
        table.revalidate();
    }

    @Override
    public void setVisible(boolean b) {
        if (b) setExtendedState(JFrame.NORMAL);
        super.setVisible(b);
    }
}