package util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class CustomListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        list.setSelectionBackground(Globals.textColor);
        list.setSelectionForeground(Globals.bgColorDark);
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        return this;
    }
}
