package com.adashrod.scannerdaemon.plugins;

import com.adashrod.scannerdaemon.Plugin;
import com.adashrod.scannerdaemon.PluginApi;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;

/**
 * A plugin that simply shows a GUI dialog on load so that the user can see that the daemon has started
 */
public class StartUpGui extends Plugin {
    @Override
    public void onLoad(final PluginApi pluginApi) {
        SwingUtilities.invokeLater(() -> {
            final JFrame notificationDialog = new JFrame("BluRay Scanner Daemon");
            final JLabel label = new JLabel("daemon started");
            final JButton okButton = new JButton(new AbstractAction("OK") {
                @Override
                public void actionPerformed(final ActionEvent e) { notificationDialog.setVisible(false); }
            });
            notificationDialog.getContentPane().setLayout(new BoxLayout(notificationDialog.getContentPane(), BoxLayout.Y_AXIS));
            final JPanel topPanel = new JPanel(), bottomPanel = new JPanel();
            topPanel.add(label);
            bottomPanel.add(okButton);
            notificationDialog.add(topPanel);
            notificationDialog.add(bottomPanel);

            notificationDialog.setVisible(true);
            notificationDialog.setPreferredSize(new Dimension(325, 100));
            notificationDialog.pack();
        });
    }

    @Override
    public void afterScan(final File scannedFile, final Collection<File> generatedFiles) {}
}
