package com.github.wohaopa.wrapper.ui.window;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.Tags;
import com.github.wohaopa.wrapper.ui.ModsInfoJson;
import com.github.wohaopa.wrapper.utils.WrapperLog;

public class CheckModsInfoFile extends JDialog implements ActionListener {

    private final JTextField filePathField;
    private final JTextField downloadPathField;
    private final JScrollPane fails;
    private final JButton migrateButton;
    private final JButton loadButton;
    MultiThreadedDownloader downloader = new MultiThreadedDownloader(4);

    public CheckModsInfoFile() {
        super((Frame) null);
        setTitle(Tags.Name + " - " + Tags.VERSION + " by 初夏同学");
        setSize(700, 600); // 设置窗口大小
        setLocationRelativeTo(null);
        setModal(true);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        filePathField = new JTextField(20);
        downloadPathField = new JTextField(20);
        downloadPathField.setText("mods");
        filePathField.setEditable(false);
        filePathField.setText(Config.getWrapperModListFile());

        fails = new JScrollPane();

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> chooseFile());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            dispose();
            downloader.shutdown();
        });

        loadButton = new JButton("Load");
        loadButton.addActionListener(this);

        migrateButton = new JButton("Migrate");
        migrateButton.addActionListener(e -> {
            migrateButton.setEnabled(false);
            migrateButton.setText("Migrating");
            File file = new File(Config.getWrapperModListFile());

            ModsInfoJson modsInfoJson = new ModsInfoJson(file);
            modsInfoJson.load();
            modsInfoJson.migrate(new File(downloadPathField.getText()), new File("ModsRepository"));

            migrateButton.setText("Migrate");
            migrateButton.setEnabled(true);
        });

        JPanel panel = new JPanel();
        panel.add(downloadPathField);
        panel.add(filePathField);
        panel.add(browseButton);
        panel.add(loadButton);
        panel.add(migrateButton);
        panel.add(closeButton);

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(downloader.getPane(), BorderLayout.CENTER);
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            Config.setWrapperModsList(selectedFile.getAbsolutePath());
            filePathField.setText(Config.getWrapperModListFile());
            WrapperLog.log.info("Selected file: " + selectedFile.getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        if (Config.getWrapperModListFile() != null) {
            File wrapperFile = new File(Config.getWrapperModListFile());
            ModsInfoJson modsInfoJson = new ModsInfoJson(wrapperFile);
            if (modsInfoJson.load()) {
                if (!modsInfoJson.check(new File("ModsRepository"))) {
                    modsInfoJson.saveMisMod();
                    CheckModsInfoFile dialog = new CheckModsInfoFile();
                    dialog.setVisible(true);
                }
            }
            if (Config.getModListFile() == null) {
                File file = new File("Forge-" + new File(Config.getWrapperModListFile()).getName());
                Config.setModsListFile(file.getAbsolutePath());
            }
            File forgeModsFile = new File(Config.getModListFile());
            if (!forgeModsFile.exists() || forgeModsFile.lastModified() < wrapperFile.lastModified()) {
                modsInfoJson.saveForgeModsListFile(new File("ModsRepository"), forgeModsFile);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        loadButton.setEnabled(false);
        loadButton.setText("Loading");
        File file = new File(Config.getWrapperModListFile());
        if (file.isFile()) {
            ModsInfoJson modsInfoJson = new ModsInfoJson(file);
            if (modsInfoJson.load()) {
                // 下载中
                if (!modsInfoJson.check(new File("ModsRepository"))) {
                    List<ModsInfoJson._ModsInfo> failModsInfoList = modsInfoJson.getMisModsList();
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
                            e1.printStackTrace();
                        }
                        loadButton.setEnabled(true);
                        loadButton.setText("Done");
                    }).start();
                } else {
                    loadButton.setEnabled(true);
                    loadButton.setText("Load");
                }
            } else {
                loadButton.setEnabled(true);
                loadButton.setText("Load");
            }
        } else {
            loadButton.setEnabled(true);
            loadButton.setText("Load");
        }
    }
}
