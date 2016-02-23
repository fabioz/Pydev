/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class UIUtils {

    public static void execLoop(JComponent editor, Frame parent, boolean modal) {
        execLoop(editor, parent, modal, 800, 600);
    }

    public static void execLoop(JComponent editor, Frame parent, boolean modal, int w, int h) {
        JDialog dialog = new JDialog(parent, modal);

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(editor);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        dialog.setSize(w, h);
        centerWindow(dialog);
        dialog.setVisible(true);

    }

    public static void execLoop(JComponent editor, boolean modal) {
        execLoop(editor, new JFrame(), modal);
    }

    /**
     * Addes the given editor to a jframe and halts until it is closed)
     */
    public static void execLoop(JComponent editor) {
        execLoop(editor, true);

    }

    /**
     * Shows some string in a text field.
     */
    public static void showString(String strToShow) {
        JTextArea field = new JTextArea();
        field.setText(strToShow);
        execLoop(field);
    }

    /**
     * Shows the given exception (currently in a text field)
     */
    public static void showException(Exception e) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(byteOut);
        e.printStackTrace(out);
        String strToShow = new String(byteOut.toByteArray());
        UIUtils.showString(strToShow);
    }

    /**
     * @return true if it was confirmed and false otherwise
     */
    public static boolean showStringAndConfirm(String strToShow) {
        final JDialog dialog = new JDialog(new JFrame(), true);
        dialog.setLayout(new GridBagLayout());

        //text area
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea field = new JTextArea();
        field.setText(strToShow);
        panel.add(field, BorderLayout.CENTER);

        GridBagConstraints g = new GridBagConstraints();
        g.weightx = 1;
        g.weighty = 0.9;
        g.fill = GridBagConstraints.BOTH;
        dialog.add(panel, g);
        final Boolean[] confirmed = new Boolean[] { false };

        //ok and cancel buttons
        panel = new JPanel(new GridLayout(0, 2));
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed[0] = true;
                dialog.setVisible(false);
            }
        });
        panel.add(button);

        button = new JButton("Cancel");
        panel.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

        g = new GridBagConstraints();
        g.weightx = 1;
        g.fill = GridBagConstraints.BOTH;
        g.gridy = 1;
        dialog.add(panel, g);

        dialog.pack();
        dialog.setSize(1024, 900);
        centerWindow(dialog);
        dialog.setVisible(true);
        return confirmed[0];
    }

    public static void centerWindow(Window component) {

        //Get the screen size
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        //Calculate the frame location
        int x = (screenSize.width - component.getWidth()) / 2;
        int y = (screenSize.height - component.getHeight()) / 2;

        //Set the new frame location
        component.setLocation(x, y);
    }

}
