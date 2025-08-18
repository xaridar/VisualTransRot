package util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class CustomListCellRendererAccent extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        list.setSelectionBackground(Globals.accentColorLight);
        list.setSelectionForeground(Globals.accentColorDark);
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText((value == "") ? "​" : value.toString());
        return this;
    }
}
