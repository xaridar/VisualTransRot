package config;

import java.awt.event.ActionListener;

public class MenuOption {
    public MenuOption[] suboptions;
    public String name;
    public ActionListener listener;
    public int mnemonic;
    public int mnemonicIndex;

    public MenuOption(String name, ActionListener listener, int mnemonic, int mnemonicIndex) {
        this.name = name;
        this.listener = listener;
        this.mnemonic = mnemonic;
        this.mnemonicIndex = mnemonicIndex;
    }

    public MenuOption(String name, ActionListener listener, int mnemonic) {
        this(name, listener, mnemonic, -1);
    }

    public MenuOption(String name, int mnemonic, MenuOption... suboptions) {
        this(name, null, mnemonic, -1);
        this.suboptions = suboptions;
    }
}
