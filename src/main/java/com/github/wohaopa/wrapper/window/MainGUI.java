package com.github.wohaopa.wrapper.window;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.ModsInfoJson;
import com.github.wohaopa.wrapper.ModsVersion;
import com.github.wohaopa.wrapper.Tags;

public class MainGUI extends JDialog {

    private static final File lock = new File("wrapper.lock");

    private final JList<String> settings = new JList<>();

    private final JTextField configTextField = new JTextField();
    private final JTextField mainModsTextField = new JTextField();
    private final JTextField modsListFileTextField = new JTextField();
    private final JTextField wrapperModsListTextField = new JTextField();
    private final JList<String> extraMods = new JList<>();

    private final JList<String> modsNameList = new JList<>();
    private final JList<String> modsVersionList = new JList<>();

    private final JTextArea logTextArea = new JTextArea();

    static ModsInfoJson modsInfoJson;
    static boolean modsInfoLoaded = false;
    static boolean modsInfoChecked = false;

    ModsVersion modsVersion;
    String currentMod = null;

    private final MultiThreadedDownloader downloader = new MultiThreadedDownloader(4);

    public MainGUI(Frame frame) {
        super(frame);
        setTitle(Tags.Name + " - " + Tags.VERSION + " by 初夏同学");
        setModal(true);
        setLayout(new BorderLayout());
        setSize(960, 670);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // 创建一个主面板
        JPanel panel = new JPanel();
        panel.setLayout(null);

        extraMods.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        extraMods.setModel(new DefaultListModel<>());
        extraMods.addMouseListener(new MyMouseAdapter());
        extraMods.setDragEnabled(true);
        extraMods.setDropMode(DropMode.INSERT);
        extraMods.setTransferHandler(new ListTransferHandler());

        EmptyBorder emptyBorder = new EmptyBorder(1, 1, 1, 1);
        // 最左边的所有设置
        {
            settings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            DefaultListModel<String> listModel = new DefaultListModel<>();
            settings.setModel(listModel);
            settings.addMouseListener(new MyMouseAdapter());

            settings.addListSelectionListener(e -> updateSettings(settings.getSelectedValue()));
            settings.getModel()
                .addListDataListener(new ListDataListener() {

                    @Override
                    public void intervalAdded(ListDataEvent e) {
                        updateSettings(settings.getSelectedValue());
                    }

                    @Override
                    public void intervalRemoved(ListDataEvent e) {}

                    @Override
                    public void contentsChanged(ListDataEvent e) {
                        updateSettings(settings.getSelectedValue());
                    }
                });

            for (String setting : Config.getAllSettings()) {
                listModel.addElement(setting);
            }

            settings.setSelectedValue(Config.getActiveSetting(), true);

            Component component1 = new JLabel("设置选项");
            Component component2 = new JScrollPane(settings);
            JButton component3 = new JButton("+");
            component3.setToolTipText("添加新的配置项");
            JButton component4 = new JButton("-");
            component4.setToolTipText("删除选中的配置项");
            JButton component5 = new JButton("↻");
            component5.setToolTipText("从文件中重新加载配置文件");
            component3.setBorder(emptyBorder);
            component4.setBorder(emptyBorder);
            component5.setBorder(emptyBorder);

            component3.addActionListener(e -> ((DefaultListModel<String>) settings.getModel()).add(0, "New Setting"));
            component4.addActionListener(e -> {
                String setting = settings.getSelectedValue();
                ((DefaultListModel<String>) settings.getModel()).remove(settings.getSelectedIndex());
                Config.removeSetting(setting);
                settings.setSelectedIndex(0);
            });
            component5.setEnabled(false);

            component1.setBounds(10, 0, 135, 20);
            component2.setBounds(10, 25, 200, 500);
            component3.setBounds(200 - 20 * 3 - 2, 0, 20, 20);
            component4.setBounds(200 - 20 * 2 - 2, 0, 20, 20);
            component5.setBounds(200 - 20, 0, 20, 20);

            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(component5);
        }

        // 中间的详细信息
        {

            configTextField.setToolTipText("重定向配置文件夹的地址（不建议修改）");
            mainModsTextField.setToolTipText("重定向配置主模组文件夹的地址（部分需要解压资源的模组会在此解压，如果模组在其他目录不能工作可以放置到此处）");
            modsListFileTextField.setToolTipText("forge的模组清单描述表（可以设置为空，如果设置Wrapper清单，这此处的文件将被覆盖）");
            wrapperModsListTextField.setToolTipText("Wrapper的模组清单描述表，可以根据清单下载模组文件");

            Component component1 = new JLabel("config目录:");
            Component component2 = new JLabel("主mods目录:");
            Component component3 = new JLabel("Forge模组清单:");
            Component component4 = new JLabel("Wrapper清单:");

            component1.setBounds(220, 10, 100, 20);
            component2.setBounds(220, 35, 100, 20);
            component3.setBounds(220, 60, 100, 20);
            component4.setBounds(220, 85, 100, 20);

            configTextField.setBounds(330, 10, 400, 20);
            mainModsTextField.setBounds(330, 35, 400, 20);
            modsListFileTextField.setBounds(330, 60, 400, 20);
            wrapperModsListTextField.setBounds(330, 85, 400, 20);

            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(configTextField);
            panel.add(mainModsTextField);
            panel.add(modsListFileTextField);
            panel.add(wrapperModsListTextField);

            Component component5 = new JLabel("额外模组加载文件地址");
            Component component6 = new JScrollPane(extraMods);
            JButton component7 = new JButton("+");
            JButton component8 = new JButton("-");

            component7.setToolTipText("新建一个额外模组加载目录");
            component8.setToolTipText("删除选中的额外模组加载目录");

            component7.setBorder(emptyBorder);
            component8.setBorder(emptyBorder);

            component7.addActionListener(e -> ((DefaultListModel<String>) extraMods.getModel()).add(0, "New"));
            component8.addActionListener(e -> {
                if (extraMods.getSelectedValue() != null)
                    ((DefaultListModel<String>) extraMods.getModel()).remove(extraMods.getSelectedIndex());
            });

            component5.setBounds(740, 0, 130, 20);
            component6.setBounds(740, 25, 200, 200);
            component7.setBounds(740 + 154, 0, 20, 20);
            component8.setBounds(740 + 175, 0, 20, 20);

            panel.add(component5);
            panel.add(component6);
            panel.add(component7);
            panel.add(component8);
        }

        // 按钮控制
        {
            JButton component1 = new JButton("应用");
            component1.setToolTipText("将设置内容保存到内存中");
            JButton component2 = new JButton("保存");
            component2.setToolTipText("将配置文件保存到硬盘中");
            JButton component3 = new JButton("加载");
            component3.setToolTipText("加载Wrapper模组清单文件");
            JButton component4 = new JButton("检查");
            component4.setToolTipText("根据模组清单文件检查仓库中存在的模组情况");
            JButton component5 = new JButton("下载");
            component5.setToolTipText("下载缺失的模组");
            JButton component6 = new JButton("迁移");
            component6.setToolTipText("将下载好的模组迁移到仓库");
            JButton component7 = new JButton("关闭");
            component7.setToolTipText("关闭并以选中的配置启动游戏");
            JCheckBox component8 = new JCheckBox();
            component8.setToolTipText("下次启动不再显示，若要再次显示，请删除与配置文件同级的wrapper.lock文件");

            component8.setSelected(true);

            component1.addActionListener(e -> applySettings(settings.getSelectedValue()));
            component2.addActionListener(e -> {
                String setting = settings.getSelectedValue();
                if (setting != null) Config.setConfig(setting);
                Config.saveConfig();
            });
            component3.addActionListener(e -> {
                String wrapperFile = wrapperModsListTextField.getText();
                if (wrapperFile != null && !wrapperFile.trim()
                    .isEmpty()) {
                    File forgeWrapperFile = new File(wrapperFile);
                    if (forgeWrapperFile.exists()) {
                        component3.setText("加载中");
                        component3.setEnabled(false);
                        modsInfoJson = new ModsInfoJson(forgeWrapperFile);
                        modsInfoLoaded = false;
                        modsInfoChecked = false;
                        if (modsInfoJson.load()) {
                            modsInfoLoaded = true;
                            logTextArea.append("Wrapper清单文件：");
                            logTextArea.append(wrapperFile);
                            logTextArea.append("加载成功！\n");
                        } else {
                            logTextArea.append("Wrapper清单文件：");
                            logTextArea.append(wrapperFile);
                            logTextArea.append("加载失败！\n");
                        }
                        component3.setText("加载");
                        component3.setEnabled(true);
                    } else {
                        logTextArea.append("Wrapper清单文件：");
                        logTextArea.append(wrapperFile);
                        logTextArea.append("不存在！\n");
                    }
                }
            });
            component4.addActionListener(e -> {
                if (modsInfoLoaded) {
                    if (modsInfoJson.check(new File("ModsRepository"))) {
                        logTextArea.append("Wrapper清单文件检查成功，所有清单中描述的模组全部存在！\n");
                    } else {
                        logTextArea.append("Wrapper清单文件检查失败，缺失一个或多个模组！\n详细信息已经保存到了以Mis_mods_为前缀的json文件中\n");
                        modsInfoJson.saveMisMod();
                    }
                    modsInfoChecked = true;
                } else {
                    logTextArea.append("Wrapper清单文件未加载，请先加载\n");
                }
            });
            component5.addActionListener(e -> {
                if (!modsInfoLoaded) {
                    logTextArea.append("请加载描述文件！");
                    return;
                }
                if (modsInfoChecked) {
                    List<ModsInfoJson._ModsInfo> misModsList = modsInfoJson.getMisModsList();
                    CountDownLatch latch = new CountDownLatch(misModsList.size());
                    for (ModsInfoJson._ModsInfo modsInfo : misModsList) {
                        download(new File(Tags.downloadDir, modsInfo.filename), modsInfo.url, latch, null);
                    }
                    new Thread(() -> {
                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        logTextArea.append("下载完成！请检查完成情况");
                        logTextArea.append("正在迁移下载完成的模组");
                        modsInfoJson.migrate(Tags.downloadDir, Tags.modsRepository);
                        logTextArea.append("迁移完成！");
                    }).start();

                } else {
                    logTextArea.append("请先检查模组！");
                }

            });
            component6.addActionListener(e -> {

            });
            component7.addActionListener(e -> {
                downloader.shutdown();
                Config.setConfig(settings.getSelectedValue());
                if (component8.isSelected()) {
                    try {
                        lock.createNewFile();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                dispose();
            });

            component6.setEnabled(false);

            component1.setBounds(220, 110, 70, 20);
            component2.setBounds(220 + 70, 110, 70, 20);
            component3.setBounds(220 + 70 * 2, 110, 70, 20);
            component4.setBounds(220 + 70 * 3, 110, 70, 20);
            component5.setBounds(220 + 70 * 4, 110, 70, 20);
            component6.setBounds(220 + 70 * 5, 110, 70, 20);
            component7.setBounds(220 + 70 * 6, 110, 70, 20);
            component8.setBounds(220 + 70 * 7, 105, 30, 30);
            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(component5);
            panel.add(component6);
            panel.add(component7);
            panel.add(component8);
        }

        // 模组选择
        {

            modsNameList.setModel(new DefaultListModel<>());
            modsVersionList.setModel(new DefaultListModel<>());
            modsNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            modsVersionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            modsNameList.addListSelectionListener(e -> {
                String modsName = modsNameList.getSelectedValue();
                if (modsName != null && !modsName.equals(currentMod)) {
                    currentMod = modsName;
                    addAllToList(modsVersion.getVersions(modsName), modsVersionList);
                }
            });

            Component component1 = new JLabel("模组列表");
            Component component2 = new JScrollPane(modsNameList);
            Component component3 = new JLabel("版本");
            Component component4 = new JScrollPane(modsVersionList);
            component1.setBounds(220, 140, 200, 20);
            component2.setBounds(220, 165, 200, 360);
            component3.setBounds(430, 140, 200, 20);
            component4.setBounds(430, 165, 200, 360);
            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
        }

        // 按钮控制
        {
            JButton component1 = new JButton("刷新");
            component1.setToolTipText("从远程地址获取模组版本（这一步可能比较慢）");
            JButton component2 = new JButton("选择");
            component2.setToolTipText("将选中的模组版本保存到内存中的清单中（会从指定仓库下载模组的版本详细信息）");
            JButton component3 = new JButton("保存");
            component3.setToolTipText("保存内存中的清单到文件");

            component1.addActionListener(e -> {
                File versionsJson = new File(Tags.downloadDir, Tags.modsVersionsPath);
                if (versionsJson.exists()) {
                    long current = System.currentTimeMillis();
                    if (current - versionsJson.lastModified() > 24 * 60 * 60 * 1000) { // 大于一天
                        download(versionsJson, Tags.wrapperRepo + Tags.modsVersionsPath);
                    }
                } else {
                    download(versionsJson, Tags.wrapperRepo + Tags.modsVersionsPath);
                }

                modsVersion = new ModsVersion(versionsJson);
                if (modsVersion.load()) {
                    List<String> strings = new ArrayList<>(modsVersion.getMods());
                    Collections.sort(strings);

                    addAllToList(strings, modsNameList);
                } else {
                    logTextArea.append(Tags.modsVersionsPath);
                    logTextArea.append("下载失败，无法获得模组版本信息");
                }

            });
            component2.addActionListener(e -> {
                if (!modsInfoLoaded) {
                    logTextArea.append("请加载描述文件！");
                    return;
                }
                String version = modsVersionList.getSelectedValue();
                if (version == null) {
                    logTextArea.append("请选择模组版本！");
                    return;
                }

                String mod = modsNameList.getSelectedValue();
                String path = modsVersion.getPath(mod);
                File file = new File(Tags.downloadDir, path);

                ModsInfoJson._ModsInfo modsInfo = null;
                ModsInfoJson modversion = new ModsInfoJson(file);
                int n = 5;
                while (!modversion.load() && n-- > 0 && modsInfo == null) {
                    file.delete();
                    CountDownLatch latch = new CountDownLatch(1);
                    download(file, Tags.wrapperRepo + path, latch, null);
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    modsInfo = modversion.getModsInfo(mod, version);
                }

                if (modsInfo == null) {
                    logTextArea.append("无法下载到指定版本的详细信息！");
                    logTextArea.append(version);
                    logTextArea.append("\n");
                } else {
                    modsInfoJson.replace(modsInfo);
                }

            });
            component3.addActionListener(e -> {
                if (!modsInfoLoaded) {
                    logTextArea.append("请加载描述文件！");
                    return;
                }
                modsInfoJson.save();
            });

            component1.setBounds(640, 150, 90, 20);
            component2.setBounds(640, 150 + 25, 90, 20);
            component3.setBounds(640, 150 + 50, 90, 20);
            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
        }

        // 控制台
        {
            Component component1 = new JLabel("下载列表");
            Component component2 = new JScrollPane(downloader.getPane());
            Component component3 = new JLabel("日志");
            Component component4 = new JScrollPane(logTextArea);
            component1.setBounds(640, 230, 300, 20);
            component2.setBounds(640, 230 + 25, 300, 120);
            component3.setBounds(640, 230 + 25 + 120, 300, 20);
            component4.setBounds(640, 230 + 50 + 120, 300, 120);
            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
        }

        // 代理与替换设置

        {

            Component component1 = new JLabel("地址:");
            JTextField component2 = new JTextField("127.0.0.1");
            Component component3 = new JLabel("端口:");
            JTextField component4 = new JTextField("7890");
            JButton component5 = new JButton("设置");
            JButton component6 = new JButton("测试");
            component1.setBounds(10, 530, 50, 20);
            component2.setBounds(10 + 50 + 10, 530, 200, 20);
            component3.setBounds(10, 555, 50, 20);
            component4.setBounds(10 + 50 + 10, 555, 200, 20);
            component5.setBounds(10 + 50 + 20 + 200, 530, 60, 20);
            component6.setBounds(10 + 50 + 20 + 200, 555, 60, 20);
            component5.addActionListener(e -> {
                String host = component2.getText();
                String port = component4.getText();
                downloader.setProxy(host, Integer.parseInt(port));
            });
            component6.addActionListener(e -> {
                CountDownLatch l = new CountDownLatch(1);
                download(new File(Tags.downloadDir, "download_test"), Tags.wrapperRepo + "README.md", l, s -> {
                    logTextArea.append("链接失败：" + s + "\n");
                    return null;
                });

                try {
                    l.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                logTextArea.append("测试结束，如果没有提示失败就是成功\n");
            });

            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(component5);
            panel.add(component6);
        }

        // 添加主面板到窗口
        add(panel);
    }

    private void download(File file, String url) {
        download(file, url, null, null);
    }

    private void download(File file, String url, CountDownLatch latch, Function<String, Void> callback) {
        downloader.addDownloadTask(url, file, latch, callback);
    }

    private void addAllToList(Collection<String> list, JList<String> jList) {
        DefaultListModel<String> modsNameListModel = (DefaultListModel<String>) jList.getModel();
        modsNameListModel.clear();
        for (String s : list) {
            modsNameListModel.addElement(s);
        }
    }

    private void applySettings(String setting) {
        Config._ConfigItem configItem = Config.getSetting(setting);
        if (configItem == null) throw new RuntimeException("ConfigItem is null!");

        Config.setConfigDIr(configItem, configTextField.getText());
        Config.setMainModsDir(configItem, mainModsTextField.getText());

        Config.setModsListFile(configItem, modsListFileTextField.getText());
        Config.setWrapperModsList(configItem, wrapperModsListTextField.getText());

        DefaultListModel<String> defaultListModel = (DefaultListModel<String>) extraMods.getModel();
        List<String> list = new ArrayList<>(defaultListModel.getSize());
        for (int i = 0; i < defaultListModel.getSize(); i++) {
            list.add(defaultListModel.getElementAt(i));
        }
        Config.setExtraModsDirs(configItem, list);
    }

    private void updateSettings(String setting) {
        if (setting == null) return;
        Config._ConfigItem configItem = Config.getSetting(setting);
        if (configItem == null) { // new or rename
            if (settings.getModel()
                .getSize()
                != Config.getAllSettings()
                    .size()) { // new
                configItem = Config.newSetting(setting);
            } else { // rename
                String newValue = null;
                Set<String> set = new HashSet<>(Config.getAllSettings());
                for (int i = 0; i < settings.getModel()
                    .getSize(); i++) {
                    String s = settings.getModel()
                        .getElementAt(i);
                    if (set.contains(s)) set.remove(s);
                    else if (newValue != null) throw new RuntimeException("重复键！" + s + " 和 " + newValue);
                    else newValue = s;
                }
                if (set.size() != 1) throw new RuntimeException("重复键！");
                String oldValue = set.toArray(new String[0])[0];
                configItem = Config.renameSetting(newValue, oldValue);
            }
        }

        configTextField.setText(configItem.config);
        mainModsTextField.setText(configItem.main_mods);
        modsListFileTextField.setText(configItem.modsListFile);
        wrapperModsListTextField.setText(configItem.wrapperModsList);
        {
            DefaultListModel<String> defaultListModel = (DefaultListModel<String>) extraMods.getModel();
            defaultListModel.clear();
            if (configItem.extra_mods != null) for (String s : configItem.extra_mods) {
                defaultListModel.addElement(s);
            }
        }

    }

    public static boolean checkWrapperModsFile() {
        String filePath = Config.getWrapperModListFile();
        if (filePath == null || filePath.isEmpty()) return true;
        File file = new File(filePath);
        if (!file.isFile()) return true;
        modsInfoJson = new ModsInfoJson(file);
        modsInfoChecked = false;
        modsInfoLoaded = false;
        if (!modsInfoJson.load()) return false;
        modsInfoLoaded = true;
        modsInfoChecked = true;
        return modsInfoJson.check(Tags.modsRepository);
    }

    public static void main(String[] args) {
        if (lock.exists() && checkWrapperModsFile()) return;
        JDialog dialog = new MainGUI(null);
        dialog.setVisible(true);
    }

    private static class MyMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                JList<String> list = (JList<String>) evt.getSource();
                int index = list.locationToIndex(evt.getPoint());
                if (index != -1) {
                    String selectedValue = list.getModel()
                        .getElementAt(index);

                    String newValue = JOptionPane.showInputDialog(null, "Edit Item", selectedValue);

                    if (newValue != null && !newValue.trim()
                        .isEmpty()) {
                        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
                        model.set(index, newValue);
                    }
                }
            }
        }
    }

    private static class ListTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> list = (JList<?>) c;
            Object value = list.getSelectedValue();
            return new StringSelection(value.toString());
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            JList<String> list = (JList<String>) support.getComponent();
            DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
            int index = dl.getIndex();

            try {
                String data = (String) support.getTransferable()
                    .getTransferData(DataFlavor.stringFlavor);
                if (dl.isInsert()) {
                    model.add(index, data);
                } else {
                    model.set(index, data);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            if (action == MOVE) {
                JList<?> list = (JList<?>) c;
                DefaultListModel<?> model = (DefaultListModel<?>) list.getModel();
                int index = list.getSelectedIndex();
                model.remove(index);
            }
        }
    }

}
