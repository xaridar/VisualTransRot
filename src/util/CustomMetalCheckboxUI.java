package util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.plaf.metal.MetalCheckBoxUI;

public class CustomMetalCheckboxUI extends MetalCheckBoxUI {

    private Color bgColor;
    private Color checkColor;
    private Color checkColorDisabled;

    public CustomMetalCheckboxUI(Color bgColor, Color checkColor, Color checkColorDisabled) {
        this.bgColor = bgColor;
        this.checkColor = checkColor;
        this.checkColorDisabled = checkColorDisabled;
    }

    public synchronized void paint(java.awt.Graphics g, javax.swing.JComponent c) {

        Dimension size = c.getSize();
        
        Rectangle viewRect = new Rectangle(size);
        
        Insets i = c.getInsets();
        viewRect.x += i.left;
        viewRect.y += i.top;
        viewRect.width -= (i.right + viewRect.x);
        viewRect.height -= (i.bottom + viewRect.y);


        g.setColor(((JCheckBox) c).isSelected() && c.isEnabled() ? checkColor : ((JCheckBox) c).isSelected() ? checkColorDisabled : c.isEnabled() ? bgColor : Globals.textColorDisabled);
        g.fillRect(viewRect.x, viewRect.y, viewRect.width, viewRect.height);
        g.setColor(Color.BLACK);
        g.drawRect(viewRect.x, viewRect.y, viewRect.width, viewRect.height);
    }
    
    public void setBgColor(Color c) {
        this.bgColor = c;
    }

    public void setCheckColor(Color c) {
        this.checkColor = c;
    }

    public void setCheckColorDisabled(Color c) {
        this.checkColorDisabled = c;
    }
}
