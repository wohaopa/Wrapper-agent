package com.github.wohaopa.wrapper.ui.window;

import java.util.Collection;

import javax.swing.DefaultListModel;
import javax.swing.JList;

public class UIUtility {

    public static void addAllToList(Collection<String> list, JList<String> jList) {
        DefaultListModel<String> modsNameListModel = (DefaultListModel<String>) jList.getModel();
        modsNameListModel.clear();
        for (String s : list) {
            modsNameListModel.addElement(s);
        }
    }
}
