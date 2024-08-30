package com.github.wohaopa.wrapper.window;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.Tags;

public class MainGUI extends JDialog {

    private static final File lock = new File("wrapper.lock");

    public MainGUI(Frame frame) {
        super(frame);
        setTitle(Tags.Name + " - " + Tags.VERSION + " by 初夏同学");
        setModal(true);
        setLayout(new FlowLayout());
        setSize(300, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // 创建一个主面板
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // 使用垂直布局

        // 添加标签和下拉框部分
        JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("选择启动配置：");

        Vector<String> v = new Vector<>(Config.config.settings.keySet());

        JComboBox<String> comboBox = new JComboBox<>(v);
        comboBox.setEditable(false);

        JButton addButton = new JButton("+");

        comboBoxPanel.add(label);
        comboBoxPanel.add(comboBox);
        comboBoxPanel.add(addButton);
        panel.add(comboBoxPanel);

        JPanel listAndInputPanel;
        JList<String> list;
        JTextField textField;
        JTextField textField2;
        {
            listAndInputPanel = new JPanel(new GridBagLayout()); // 使用GridLayout布局
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5); // 设置组件间的间距

            JLabel listLabel = new JLabel("额外mods目录：");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            listAndInputPanel.add(listLabel, gbc);

            list = new JList<>();
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 单选模式
            JScrollPane listScrollPane = new JScrollPane(list);
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            listAndInputPanel.add(listScrollPane, gbc);

            // 添加文本输入框部分
            JLabel textFieldLabel = new JLabel("config目录：");
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            listAndInputPanel.add(textFieldLabel, gbc);

            textField = new JTextField("config");
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            listAndInputPanel.add(textField, gbc);

            JLabel textFieldLabel2 = new JLabel("主mods目录：");
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            listAndInputPanel.add(textFieldLabel2, gbc);

            textField2 = new JTextField("mods");
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            listAndInputPanel.add(textField2, gbc);
        }
        panel.add(listAndInputPanel);

        JPanel ctlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox checkBox;
        JButton okButton;
        JButton cancelButton;
        {
            checkBox = new JCheckBox("下次不再显示", true);
            okButton = new JButton("确定");
            okButton.setToolTipText("保存修改并启动");
            cancelButton = new JButton("取消");
            cancelButton.setToolTipText("不保存修改并启动");

            ctlPanel.add(checkBox);
            ctlPanel.add(okButton);
            ctlPanel.add(cancelButton);
        }

        panel.add(ctlPanel);

        // 添加主面板到窗口
        add(panel);

        // 设置按钮监听事件
        okButton.addActionListener(e -> {
            // 处理确定按钮点击事件
            Config.config.active = (String) comboBox.getSelectedItem();
            Config.setConfig();
            if (checkBox.isSelected()) {
                try {
                    lock.createNewFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            this.dispose();
        });
        addButton.addActionListener(e -> comboBox.addItem("NewSetting"));
        cancelButton.addActionListener(e -> this.dispose());
        comboBox.addActionListener(e -> {
            String selected = (String) comboBox.getSelectedItem();

            Config._ConfigItem configItem = Config.config.settings.get(selected);
            if (configItem == null) {
                configItem = new Config._ConfigItem();
                configItem.config = "config";
                configItem.main_mods = "mods";
                configItem.extra_mods = new Vector<>();
                Config.config.settings.put(selected, configItem);
            }
            textField.setText(configItem.config);
            textField2.setText(configItem.main_mods);
            list.setListData(new Vector<>(configItem.extra_mods));

        });

        comboBox.setSelectedIndex(v.indexOf(Config.config.active));

    }

    public static void main(String[] args) {
        if (lock.exists()) return;
        JDialog dialog = new MainGUI(null);
        dialog.setVisible(true);
    }
}
