package com.improvedmenus;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

final class ImprovedMenusPanel extends PluginPanel
{
    private final ProfileStore store;
    private final MenuEngine engine;
    private final JTextArea preview = new JTextArea(9,18);
    private final JLabel status = new JLabel(" ");
    private final javax.swing.Timer timer;

    ImprovedMenusPanel(ProfileStore store, MenuEngine engine, Runnable openSettings)
    {
        super(false);
        this.store=store; this.engine=engine;
        setLayout(new BorderLayout(0,8));
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        top.add(new JLabel("Improved Right-Clicks")); top.add(new JLabel("<html>Configure features in RuneLite's<br>Improved Right-Clicks settings.</html>"));
        button(top,"Open Plugin Settings",openSettings);
        JPanel actions = new JPanel(new GridLayout(0,2,4,4));
        button(actions,"Profiles",this::profiles); button(actions,"Import/export",this::imports);
        JButton undo = new JButton("Undo"); undo.setMargin(new Insets(3,4,3,4));
        undo.addActionListener(e -> status.setText(store.undo() ? "Undone" : "Nothing to undo")); actions.add(undo);
        top.add(actions); top.add(status);
        for (Component child : top.getComponents()) { if (child instanceof JComponent) { ((JComponent)child).setAlignmentX(Component.LEFT_ALIGNMENT); } }
        add(top,BorderLayout.NORTH);
        preview.setEditable(false); preview.setLineWrap(true); preview.setWrapStyleWord(true); preview.setText(engine.diagnostics());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JLabel("<html>Log in and open a game menu<br>to inspect its options.</html>"),BorderLayout.NORTH);
        bottom.add(new JScrollPane(preview),BorderLayout.CENTER);
        bottom.setVisible(false);
        JButton diagnostics = new JButton("Show diagnostics");
        diagnostics.setAlignmentX(Component.LEFT_ALIGNMENT);
        diagnostics.addActionListener(e -> {
            boolean show = !bottom.isVisible();
            bottom.setVisible(show);
            diagnostics.setText(show ? "Hide diagnostics" : "Show diagnostics");
            revalidate(); repaint();
        });
        top.add(diagnostics);
        add(bottom,BorderLayout.CENTER);
        timer = new javax.swing.Timer(500,e -> { if (isShowing() && bottom.isVisible()) { preview.setText(engine.diagnostics()); } });
        timer.start();
    }
    void stop() { timer.stop(); }
    private void button(JPanel parent, String name, Runnable action)
    {
        JButton b = new JButton(name); b.setMargin(new Insets(3,4,3,4)); b.addActionListener(e -> run(action)); parent.add(b);
    }
    void run(Runnable action)
    {
        try { action.run(); }
        catch (IllegalArgumentException e) { status.setText("Check settings"); JOptionPane.showMessageDialog(this,e.getMessage(),"Improved Right-Clicks",JOptionPane.ERROR_MESSAGE); }
    }
    private void profiles()
    {
        JPanel contents = new JPanel(new GridLayout(0,1,4,4));
        JTextField name = new JTextField(); JComboBox<String> saved = new JComboBox<>(store.names().toArray(new String[0]));
        contents.add(new JLabel("Save current settings as")); contents.add(name);
        button(contents,"Save profile",() -> { store.save(name.getText()); status.setText("Profile saved"); saved.setModel(new DefaultComboBoxModel<>(store.names().toArray(new String[0]))); });
        contents.add(saved);
        button(contents,"Preview and load",() -> {
            Object selected = saved.getSelectedItem();
            if (selected == null) { throw new IllegalArgumentException("Save a profile first"); }
            Map<String,String> values = store.load(selected.toString());
            if (JOptionPane.showConfirmDialog(this,describe(values),"Load profile",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            { applySettings(values); }
        });
        JOptionPane.showMessageDialog(this,contents,"Profiles",JOptionPane.PLAIN_MESSAGE);
    }
    private void imports()
    {
        JPanel contents = new JPanel(new BorderLayout(0,6));
        JTextArea json = new JTextArea(store.exportSettings(store.snapshot()),14,24); json.setLineWrap(true);
        contents.add(new JLabel("Copy to export; paste to preview an import."),BorderLayout.NORTH);
        contents.add(new JScrollPane(json),BorderLayout.CENTER);
        JPanel controls = new JPanel(new GridLayout(0,1,4,4));
        JComboBox<String> source = new JComboBox<>(new String[]{"No Examine","No Misclicks In Bank","Item Option Hider","Menu Colorizer","Tool Required","Misclick Guard","No Use Player","Don't Telegrab NPCs","Mouseover Options+"});
        controls.add(source);
        button(controls,"Read existing settings",() -> {
            Map<String,String> values = store.legacyPreview((String)source.getSelectedItem());
            if (values.isEmpty()) { throw new IllegalArgumentException("No explicitly saved settings were found for this plugin"); }
            json.setText(store.exportSettings(values));
            status.setText("Ready to preview");
        });
        button(controls,"Preview and apply",() -> {
            Map<String,String> values = store.previewImport(json.getText());
            if (JOptionPane.showConfirmDialog(this,describe(values),"Apply these settings?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            { applySettings(values); }
        });
        contents.add(controls,BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this,contents,"Import / export",JOptionPane.PLAIN_MESSAGE);
    }
    void applySettings(Map<String,String> values)
    {
        Map<String,String> before = store.snapshot();
        store.apply(values);
        status.setText(before.equals(store.snapshot()) ? "No changes" : "Settings applied");
    }
    private static JScrollPane describe(Map<String,String> values)
    {
        StringBuilder text = new StringBuilder();
        values.forEach((key,value) -> text.append(SettingsCatalog.ALL.get(key).label).append(": ").append(value).append('\n'));
        JTextArea area = new JTextArea(text.toString(),14,24); area.setEditable(false); area.setLineWrap(true);
        return new JScrollPane(area);
    }
}
