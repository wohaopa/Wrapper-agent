package com.github.wohaopa.wrapper.window;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.ModsInfoJson;
import com.github.wohaopa.wrapper.MultiThreadedDownloader;
import com.github.wohaopa.wrapper.Tags;

public class CheckModsInfoFile extends JDialog {

    private JTextField filePathField;
    private JTextField downloadPathField;
    private JButton browseButton;
    private JScrollPane fails;
    MultiThreadedDownloader downloader = new MultiThreadedDownloader(4);

    public CheckModsInfoFile() {
        super((Frame) null);
        setTitle(Tags.Name + " - " + Tags.VERSION + " by 初夏同学");
        setSize(700, 600); // 设置窗口大小
        setLocationRelativeTo(null);
        setModal(true);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // 创建文件路径文本框
        filePathField = new JTextField(20);
        downloadPathField = new JTextField(20);
        downloadPathField.setText("mods");
        filePathField.setEditable(false); // 设置为不可编辑
        filePathField.setText(Config.getWrapperModListFile() != null ? Config.getWrapperModListFile() : "");

        fails = new JScrollPane();

        // 创建浏览按钮
        browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            chooseFile(); // 调用文件选择方法
        });

        // 创建浏览按钮
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            dispose();
            downloader.shutdown();
        });

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> {
            if (!filePathField.getText()
                .isEmpty()) {
                File file = new File(filePathField.getText());
                if (file.isFile()) {
                    loadButton.setEnabled(false);
                    loadButton.setText("Loading");
                    List<ModsInfoJson._ModsInfo> modsInfoList = ModsInfoJson.load(file);
                    List<ModsInfoJson._ModsInfo> failModsInfoList = ModsInfoJson
                        .verifyFiles(new File("ModsRepository"), modsInfoList);

                    CountDownLatch latch = new CountDownLatch(failModsInfoList.size());

                    for (ModsInfoJson._ModsInfo modsInfo : failModsInfoList) {
                        downloader.addDownloadTask(
                            modsInfo.url,
                            new File(downloadPathField.getText(), modsInfo.filename),
                            latch,
                            s -> {
                                fails.add(new JLabel("URL: " + s));
                                return null;
                            });
                    }

                    new Thread(() -> {
                        try {
                            latch.await();
                        } catch (InterruptedException e1) {
                            System.err.println("Main thread interrupted: " + e1.getMessage());
                        }
                        loadButton.setEnabled(true);
                        loadButton.setText("Done");
                        JFrame jFrame = new JFrame("Fail!");
                        jFrame.add(fails);
                        jFrame.setVisible(true);

                    }).start();

                }
            }
        });

        JButton migrateButton = new JButton("Migrate");
        migrateButton.addActionListener(e -> {
            File file = new File(filePathField.getText());
            if (file.isFile()) {
                migrateButton.setEnabled(false);
                migrateButton.setText("Migrating");
                List<ModsInfoJson._ModsInfo> modsInfoList = ModsInfoJson.load(file);
                File jsonFile = new File(file.getParentFile(), "Forge-" + file.getName());
                ModsInfoJson
                    .migrate(new File(downloadPathField.getText()), new File("ModsRepository"), modsInfoList, jsonFile);
                Config.setWrapperModsList(jsonFile.getAbsolutePath());
                migrateButton.setText("Migrate");
                migrateButton.setEnabled(true);
            }
        });

        // 创建一个面板来放置文本框和按钮
        JPanel panel = new JPanel();
        panel.add(downloadPathField);
        panel.add(filePathField);
        panel.add(browseButton);
        panel.add(loadButton);
        panel.add(migrateButton);
        panel.add(closeButton);

        // 将面板添加到框架的内容窗格
        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(downloader.getPane(), BorderLayout.CENTER);
    }

    // 选择文件的方法
    private void chooseFile() {
        // 创建文件选择器
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY); // 只允许选择文件

        // 显示文件选择对话框
        int returnValue = fileChooser.showOpenDialog(this);

        // 如果用户选择了一个文件
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile(); // 获取选中文件
            filePathField.setText(selectedFile.getAbsolutePath()); // 设置文本框内容为文件路径
            System.out.println("Selected file: " + selectedFile.getAbsolutePath()); // 输出文件路径到控制台
        }
    }

    public static void main(String[] args) {
        if (Config.getWrapperModListFile() != null) {
            List<ModsInfoJson._ModsInfo> modsInfoList = ModsInfoJson.load(new File(Config.getWrapperModListFile()));
            List<ModsInfoJson._ModsInfo> failModsInfoList = ModsInfoJson
                .verifyFiles(new File("ModsRepository"), modsInfoList);
            if (!failModsInfoList.isEmpty()) {
                CheckModsInfoFile dialog = new CheckModsInfoFile();
                dialog.setVisible(true);
            }
        }
    }
}
