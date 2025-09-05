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
    
    public CustomMetalCheckboxUI(Color bgColor, Color checkColor) {
        this.bgColor = bgColor;
        this.checkColor = checkColor;
    }
    
    public synchronized void paint(java.awt.Graphics g, javax.swing.JComponent c) {

        Dimension size = c.getSize();
        
        Rectangle viewRect = new Rectangle(size);
        
        Insets i = c.getInsets();
        viewRect.x += i.left;
        viewRect.y += i.top;
        viewRect.width -= (i.right + viewRect.x);
        viewRect.height -= (i.bottom + viewRect.y);
        
        g.setColor(c.isEnabled() ? bgColor : Globals.textColorDisabled);
        g.fillRect(viewRect.x, viewRect.y, viewRect.width, viewRect.height);
        g.setColor(Color.BLACK);
        g.drawRect(viewRect.x, viewRect.y, viewRect.width, viewRect.height);

        if (((JCheckBox) c).isSelected()) {
            g.setColor(checkColor);
            g.fillRect(viewRect.x + viewRect.width / 4, viewRect.y + viewRect.height / 4, (int) (viewRect.width * 0.65), (int) (viewRect.height * 0.65));
        }
    }
    
    public void setBgColor(Color c) {
        this.bgColor = c;
    }

    public void setCheckColor(Color c) {
        this.checkColor = c;
    }
}
