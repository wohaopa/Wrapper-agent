package com.github.wohaopa.wrapper.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.ModsInfoJson;
import com.github.wohaopa.wrapper.ModsVersion;
import com.github.wohaopa.wrapper.Tags;

public class MainGUI extends JDialog {

    private static final File lock = new File("wrapper.lock");

    private final MultiThreadedDownloader downloader = new MultiThreadedDownloader(4);

    EmptyBorder emptyBorder = new EmptyBorder(1, 1, 1, 1);

    public MainGUI(Frame frame) {
        super(frame);
        setTitle(Tags.Name + " - " + Tags.VERSION + " by 初夏同学");
        setModal(true);
        setLayout(new BorderLayout());
        setSize(560, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // 创建一个主面板
        JPanel panel = new JPanel();
        panel.setLayout(null);

        Map<String, JPanel> panelMap = new HashMap<>();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBounds(0, 40, getWidth(), getHeight() - 40);

        panel.add(mainPanel);

        {
            panelMap.put("主页", getMainPanel());
            panelMap.put("设置", getSettingsPanel());
            panelMap.put("高级", getAdvPanel());
            panelMap.put("启动", getLaunchPanel());
            panelMap.put("其他", getOthersPanel());

        }
        { // 导航栏
            JPanel component0 = new JPanel();
            component0.setLayout(null);

            JButton[] component1 = new JButton[] { new JButton("主页"), new JButton("设置"), new JButton("高级"),
                new JButton("启动"), new JButton("其他") };

            ActionListener action = e -> {
                JButton button = ((JButton) e.getSource());
                mainPanel.removeAll();
                mainPanel.add(panelMap.get(button.getText()));
                mainPanel.revalidate();
                mainPanel.repaint();
                button.setEnabled(false);
                for (int i = 0; i < component1.length; i++) {
                    if (button != component1[i]) {
                        component1[i].setEnabled(true);
                    }
                }
            };

            for (int i = 0; i < component1.length; i++) {
                component1[i].setBorder(emptyBorder);
                component1[i].setBounds(81 * i, 5, 80, 30);
                component1[i].addActionListener(action);
                component0.add(component1[i]);
            }

            component1[0].setEnabled(false);
            mainPanel.add(panelMap.get(component1[0].getText()));

            int width = 81 * component1.length;
            component0.setBounds((getWidth() - width) / 2, 0, width, 40);
            panel.add(component0);
        }
        // // 添加主面板到窗口
        add(panel);
    }

    // 设置的字段

    private JPanel getMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, getWidth(), getHeight() - 40);
        {
            Component component1 = new JLabel("环境变量");
            component1.setBounds(0, 0, 200, 20);

            JPanel component2 = new JPanel();
            component2.setLayout(null);

            AtomicInteger y = new AtomicInteger();
            System.getProperties()
                .forEach((o, o2) -> {
                    Component component4 = new JTextField(o.toString());
                    Component component5 = new JTextField(o2.toString());
                    component4.setBounds(0, y.get(), 95, 20);
                    component5.setBounds(100, y.get(), 320, 20);
                    component2.add(component4);
                    component2.add(component5);
                    y.addAndGet(22);
                });
            // component2.setBounds(0,0,200,y.get());

            JScrollPane component3 = new JScrollPane(component2);
            component3.setBounds(5, 25, getWidth() - 25, panel.getHeight() - 90);
            panel.add(component1);
            panel.add(component3);
        }

        panel.add(new JLabel("主页"));
        return panel;
    }

    private final JTextField configTextField = new JTextField();
    private final JTextField mainModsTextField = new JTextField();
    private final JTextField modsListFileTextField = new JTextField();
    private final JTextField wrapperModsListTextField = new JTextField();
    private final JList<String> extraMods = new JList<>();
    private final JList<String> settings = new JList<>();

    private JPanel getSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, getWidth(), getHeight() - 40);

        {
            extraMods.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            extraMods.setModel(new DefaultListModel<>());
            extraMods.addMouseListener(new MyMouseAdapter());
            extraMods.setDragEnabled(true);
            extraMods.setDropMode(DropMode.INSERT);
            extraMods.setTransferHandler(new ListTransferHandler());
        }
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
        }
        // 左边
        {
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
            component3.setBounds(210 - 20 * 3 - 2, 0, 20, 20);
            component4.setBounds(210 - 20 * 2 - 2, 0, 20, 20);
            component5.setBounds(210 - 20, 0, 20, 20);
            component2.setBounds(10, 25, 200, panel.getHeight() - 70);

            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(component5);
        }
        // 右边
        {
            configTextField.setToolTipText("重定向配置文件夹的地址（不建议修改）");
            mainModsTextField.setToolTipText("重定向配置主模组文件夹的地址（部分需要解压资源的模组会在此解压，如果模组在其他目录不能工作可以放置到此处）");
            modsListFileTextField.setToolTipText("forge的模组清单描述表（可以设置为空，如果设置Wrapper清单，这此处的文件将被覆盖）");
            wrapperModsListTextField.setToolTipText("Wrapper的模组清单描述表，可以根据清单下载模组文件");

            Component component1 = new JLabel("config目录:");
            Component component2 = new JLabel("主mods目录:");
            Component component3 = new JLabel("Forge模组清单:");
            Component component4 = new JLabel("Wrapper清单:");

            component1.setBounds(220, 10, 90, 20);
            component2.setBounds(220, 35, 90, 20);
            component3.setBounds(220, 60, 90, 20);
            component4.setBounds(220, 85, 90, 20);

            configTextField.setBounds(310, 10, 230, 20);
            mainModsTextField.setBounds(310, 35, 230, 20);
            modsListFileTextField.setBounds(310, 60, 230, 20);
            wrapperModsListTextField.setBounds(310, 85, 230, 20);

            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(configTextField);
            panel.add(mainModsTextField);
            panel.add(modsListFileTextField);
            panel.add(wrapperModsListTextField);
        }
        // 按钮控制
        {
            JButton component1 = new JButton("应用");
            component1.setToolTipText("将设置内容保存到内存中");
            JButton component2 = new JButton("保存");
            component2.setToolTipText("将配置文件保存到硬盘中");
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

            component1.setBounds(220, 110, 70, 20);
            component2.setBounds(220 + 70, 110, 70, 20);
            component7.setBounds(220 + 70 * 2, 110, 70, 20);
            component8.setBounds(220 + 70 * 3, 105, 30, 30);
            panel.add(component1);
            panel.add(component2);
            panel.add(component7);
            panel.add(component8);
        }
        // 额外模组
        {

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

            component5.setBounds(220, 140, 320, 20);
            component6.setBounds(220, 165, 320, 230);
            component7.setBounds(220 + 320 - 20 * 2, 140, 20, 20);
            component8.setBounds(220 + 320 - 20, 140, 20, 20);

            panel.add(component5);
            panel.add(component6);
            panel.add(component7);
            panel.add(component8);
        }

        return panel;
    }

    private final JTextArea logTextArea = new JTextArea();
    private final JList<String> modsNameList = new JList<>();
    private final JList<String> modsVersionList = new JList<>();
    private ModsVersion modsVersion;
    private String currentMod = null;
    CustomListCellRenderer modsNameListCellRenderer = new CustomListCellRenderer(true);
    CustomListCellRenderer modsVersionListCellRenderer = new CustomListCellRenderer(false);

    private JPanel getAdvPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, getWidth(), getHeight() - 40);
        JTextField textField = new JTextField();
        modsNameList.setCellRenderer(modsNameListCellRenderer);
        modsVersionList.setCellRenderer(modsVersionListCellRenderer);

        textField.setToolTipText("这里的配置将不会同步到设置界面中！");
        {
            JButton component3 = new JButton("加载");
            component3.setToolTipText("加载Wrapper模组清单文件");
            JButton component4 = new JButton("检查");
            component4.setToolTipText("根据模组清单文件检查仓库中存在的模组情况");
            JButton component5 = new JButton("下载");
            component5.setToolTipText("下载缺失的模组");
            JButton component6 = new JButton("迁移");
            component6.setToolTipText("将下载好的模组迁移到仓库");
            JButton component7 = new JButton("刷新");
            component7.setToolTipText("从远程地址获取模组版本（这一步可能比较慢）");
            JButton component8 = new JButton("选择");
            component8.setToolTipText("将选中的模组版本保存到内存中的清单中（会从指定仓库下载模组的版本详细信息）");
            JButton component9 = new JButton("保存");
            component9.setToolTipText("保存内存中的清单到文件");

            component3.setBorder(emptyBorder);
            component4.setBorder(emptyBorder);
            component5.setBorder(emptyBorder);
            component6.setBorder(emptyBorder);
            component7.setBorder(emptyBorder);
            component8.setBorder(emptyBorder);
            component9.setBorder(emptyBorder);

            component3.addActionListener(e -> {
                String wrapperFile = textField.getText();
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
                    if (modsInfoJson.check(Tags.modsRepository)) {
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
                    logTextArea.append("请加载描述文件！\n");
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
                        logTextArea.append("下载完成！请检查完成情况\n");
                        logTextArea.append("正在迁移下载完成的模组\n");
                        modsInfoJson.migrate(Tags.downloadDir, Tags.modsRepository);
                        logTextArea.append("迁移完成！\n");
                    }).start();

                } else {
                    logTextArea.append("请先检查模组！\n");
                }

            });
            component6.addActionListener(e -> {
                if (!modsInfoLoaded) {
                    logTextArea.append("请加载描述文件！\n");
                    return;
                }
                if (modsInfoChecked) {

                    JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    int returnValue = fileChooser.showOpenDialog(null);

                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        logTextArea.append("选择需要迁移的目录：");
                        logTextArea.append(selectedFile.getAbsolutePath());
                        logTextArea.append("\n开始迁移...");

                        modsInfoJson.migrate(selectedFile, Tags.modsRepository);
                        logTextArea.append("迁移完成！");
                    }
                } else {
                    logTextArea.append("请先检查模组！\n");
                }
            });
            component7.addActionListener(e -> {
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

                    UIUtility.addAllToList(strings, modsNameList);
                } else {
                    logTextArea.append(Tags.modsVersionsPath);
                    logTextArea.append("下载失败，无法获得模组版本信息\n");
                    return;
                }

                if (modsInfoChecked) {
                    Set<String> modsName = new HashSet<>();
                    Set<String> modsVersion = new HashSet<>();
                    for (ModsInfoJson._ModsInfo modsInfo : modsInfoJson.getMisModsList()) {
                        modsName.add(modsInfo.uid);
                        modsVersion.add(modsInfo.uid + modsInfo.id.split(":")[2]);
                    }
                    modsNameListCellRenderer.setRed(modsName);
                    modsVersionListCellRenderer.setRed(modsVersion);
                }
                if (modsInfoLoaded) {
                    Set<String> modsName = new HashSet<>();
                    Set<String> modsVersion = new HashSet<>();
                    for (ModsInfoJson._ModsInfo modsInfo : modsInfoJson.getAllMods()) {
                        modsName.add(modsInfo.uid);
                        modsVersion.add(modsInfo.uid + modsInfo.id.split(":")[2]);
                    }
                    modsNameListCellRenderer.setGreen(modsName);
                    modsVersionListCellRenderer.setGreen(modsVersion);
                }

            });
            component8.addActionListener(e -> {
                if (!modsInfoLoaded) {
                    logTextArea.append("请加载描述文件！\n");
                    return;
                }
                String version = modsVersionList.getSelectedValue();
                if (version == null) {
                    logTextArea.append("请选择模组版本！\n");
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
                modsInfo = modversion.getModsInfo(mod, version);
                if (modsInfo == null) {
                    logTextArea.append("无法下载到指定版本的详细信息！");
                    logTextArea.append(version);
                    logTextArea.append("\n");
                } else {
                    modsInfoJson.replace(modsInfo);
                    logTextArea.append("选择成功，已经放入内存。请点击保存！");
                    logTextArea.append(version);
                    logTextArea.append("\n");
                }

            });
            component9.addActionListener(e -> {
                if (!modsInfoLoaded) {
                    logTextArea.append("请加载描述文件！\n");
                    return;
                }
                modsInfoJson.save();
            });

            component3.setBounds(5, 5, 70, 20);
            component4.setBounds(5 + 72, 5, 70, 20);
            component5.setBounds(5 + 72 * 2, 5, 70, 20);
            component6.setBounds(5 + 72 * 3, 5, 70, 20);
            component7.setBounds(5 + 72 * 4 + 33, 5, 70, 20);
            component8.setBounds(5 + 72 * 5 + 33, 5, 70, 20);
            component9.setBounds(5 + 72 * 6 + 33, 5, 70, 20);

            panel.add(component3);
            panel.add(component4);
            panel.add(component5);
            panel.add(component6);
            panel.add(component7);
            panel.add(component8);
            panel.add(component9);
        }

        {
            JLabel jLabeljLabel = new JLabel("Wrapper清单:");

            JButton button = new JButton("...");
            wrapperModsListTextField.addActionListener(e -> textField.setText(wrapperModsListTextField.getText()));

            button.setBorder(emptyBorder);
            button.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int returnValue = fileChooser.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    Config.setWrapperModsList(selectedFile.getAbsolutePath());
                    textField.setText(Config.getWrapperModListFile());
                    logTextArea.append("选择文件：");
                    logTextArea.append(selectedFile.getAbsolutePath());
                    logTextArea.append("\n");
                }
            });
            jLabeljLabel.setBounds(5, 30, 90, 20);
            textField.setBounds(5 + 90, 30, 175 + 185 - 95 - 40, 20);
            button.setBounds(175 + 185 - 95 - 40 + 5 + 90 + 5, 30, 38, 20);

            panel.add(jLabeljLabel);
            panel.add(textField);
            panel.add(button);
        }

        // 控制台
        {
            Component component1 = new JLabel("下载列表");
            Component component2 = new JScrollPane(downloader.getPane());
            Component component3 = new JLabel("日志");
            Component component4 = new JScrollPane(logTextArea);
            JButton component5 = new JButton("代理");
            JButton component6 = new JButton("测试");
            logTextArea.setEditable(false);
            component5.setBorder(emptyBorder);
            component6.setBorder(emptyBorder);
            component5.addActionListener(e -> {
                String hostAndPort = JOptionPane
                    .showInputDialog(null, "代理地址，请不要带有协议名称（如http，socket等）", "127.0.0.1:7890");
                if (hostAndPort == null || hostAndPort.isEmpty()) return;
                String[] strings = hostAndPort.split(":");
                if (strings.length == 2) {
                    downloader.setProxy(strings[0], Integer.parseInt(strings[1]));
                } else {
                    logTextArea.append("无效代理地址：");
                    logTextArea.append(hostAndPort);
                    logTextArea.append("\n");
                }
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

            component1.setBounds(185 * 2, 30, 170, 20);
            component2.setBounds(185 * 2, 30 + 25, 170, 155);
            component3.setBounds(185 * 2, 30 + 25 + 155, 170, 20);
            component4.setBounds(185 * 2, 30 + 50 + 155, 170, 155);
            component5.setBounds(185 * 2 + 170 - 46 * 2 - 1, 30, 46, 20);
            component6.setBounds(185 * 2 + 170 - 46, 30, 46, 20);
            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
            panel.add(component5);
            panel.add(component6);
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
                    UIUtility.addAllToList(modsVersion.getVersions(modsName), modsVersionList);
                }
            });

            Component component1 = new JLabel("模组列表");
            Component component2 = new JScrollPane(modsNameList);
            Component component3 = new JLabel("版本");
            Component component4 = new JScrollPane(modsVersionList);
            component1.setBounds(5, 30 + 30, 175, 20);
            component2.setBounds(5, 30 + 22 + 30, 175, 340);
            component3.setBounds(185, 30 + 30, 175, 20);
            component4.setBounds(185, 30 + 22 + 30, 175, 340);
            panel.add(component1);
            panel.add(component2);
            panel.add(component3);
            panel.add(component4);
        }

        return panel;
    }

    private JPanel getLaunchPanel() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("启动"));
        return panel;
    }

    private JPanel getOthersPanel() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("其他"));
        return panel;
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

        DefaultListModel<String> defaultListModel = (DefaultListModel<String>) extraMods.getModel();
        defaultListModel.clear();
        if (configItem.extra_mods != null) for (String s : configItem.extra_mods) {
            defaultListModel.addElement(s);
        }

    }

    private void download(File file, String url) {
        download(file, url, null, null);
    }

    private void download(File file, String url, CountDownLatch latch, Function<String, Void> callback) {
        downloader.addDownloadTask(url, file, latch, callback);
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

    static ModsInfoJson modsInfoJson;
    static boolean modsInfoLoaded = false;
    static boolean modsInfoChecked = false;

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
        FlatDarkLaf.setup();
        JDialog dialog = new MainGUI(null);
        dialog.setVisible(true);
    }

    class CustomListCellRenderer extends DefaultListCellRenderer {

        boolean isName = true;

        public CustomListCellRenderer(boolean isName) {
            this.isName = isName;
        }

        Set<String> red = new HashSet<>();
        Set<String> green = new HashSet<>();

        public void setRed(Set<String> red) {
            this.red = red;
        }

        public void setGreen(Set<String> green) {
            this.green = green;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
            Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (isName) if (red.contains(value)) {
                renderer.setForeground(Color.RED);
            } else if (green.contains(value)) {
                renderer.setForeground(Color.GREEN);
            } else {
                renderer.setForeground(list.getForeground());
            }
            else if (currentMod != null) {
                value = currentMod + value;
                if (red.contains(value)) {
                    renderer.setForeground(Color.RED);
                } else if (green.contains(value)) {
                    renderer.setForeground(Color.GREEN);
                } else {
                    renderer.setForeground(list.getForeground());
                }

            }
            return renderer;
        }
    }
}

class MyMouseAdapter extends MouseAdapter {

    public void mouseClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            JList<String> list = (JList<String>) evt.getSource();
            int index = list.locationToIndex(evt.getPoint());
            if (index != -1) {
                String selectedValue = list.getModel()
                    .getElementAt(index);

                String newValue = JOptionPane.showInputDialog(null, "重命名：", selectedValue);

                if (newValue != null && !newValue.trim()
                    .isEmpty()) {
                    DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
                    model.set(index, newValue);
                }
            }
        }
    }
}

class ListTransferHandler extends TransferHandler {

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
