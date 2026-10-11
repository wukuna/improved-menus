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

    ImprovedMenusPanel(ProfileStore store, MenuEngine engine)
    {
        super(false);
        this.store=store; this.engine=engine;
        setLayout(new BorderLayout(0,8));
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        top.add(new JLabel("Menu Workshop")); top.add(new JLabel("<html>Configure features in RuneLite's<br>Menu Workshop settings.</html>"));
        JPanel actions = new JPanel(new GridLayout(0,2,4,4));
        button(actions,"Profiles",this::profiles); button(actions,"Import/export",this::imports);
        JButton undo = new JButton("Undo"); undo.setMargin(new Insets(3,4,3,4));
        undo.addActionListener(e -> status.setText(store.undo() ? "Undone" : "Nothing to undo")); actions.add(undo);
        top.add(actions); top.add(status);
        for (Component child : top.getComponents()) { if (child instanceof JComponent) { ((JComponent)child).setAlignmentX(Component.LEFT_ALIGNMENT); } }
        add(top,BorderLayout.NORTH);
        preview.setEditable(false); preview.setLineWrap(true); preview.setWrapStyleWord(true); preview.setText(engine.diagnostics());
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(new JLabel("Menu preview and rule diagnostics"),BorderLayout.NORTH);
        bottom.add(new JScrollPane(preview),BorderLayout.CENTER); add(bottom,BorderLayout.CENTER);
        timer = new javax.swing.Timer(500,e -> { if (isShowing()) { preview.setText(engine.diagnostics()); } });
        timer.start();
    }
    void stop() { timer.stop(); }
    private void button(JPanel parent, String name, Runnable action)
    {
        JButton b = new JButton(name); b.setMargin(new Insets(3,4,3,4)); b.addActionListener(e -> run(action)); parent.add(b);
    }
    private void run(Runnable action)
    {
        try { action.run(); status.setText("Saved"); }
        catch (IllegalArgumentException e) { status.setText("Check settings"); JOptionPane.showMessageDialog(this,e.getMessage(),"Menu Workshop",JOptionPane.ERROR_MESSAGE); }
    }
    private void profiles()
    {
        JPanel contents = new JPanel(new GridLayout(0,1,4,4));
        JTextField name = new JTextField(); JComboBox<String> saved = new JComboBox<>(store.names().toArray(new String[0]));
        contents.add(new JLabel("Save current settings as")); contents.add(name);
        button(contents,"Save profile",() -> { store.save(name.getText()); saved.setModel(new DefaultComboBoxModel<>(store.names().toArray(new String[0]))); });
        contents.add(saved);
        button(contents,"Preview and load",() -> {
            Object selected = saved.getSelectedItem();
            if (selected == null) { throw new IllegalArgumentException("Save a profile first"); }
            Map<String,String> values = store.load(selected.toString());
            if (JOptionPane.showConfirmDialog(this,describe(values),"Load profile",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            { store.apply(values); }
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
        });
        button(controls,"Preview and apply",() -> {
            Map<String,String> values = store.previewImport(json.getText());
            if (JOptionPane.showConfirmDialog(this,describe(values),"Apply these settings?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            { store.apply(values); }
        });
        contents.add(controls,BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this,contents,"Import / export",JOptionPane.PLAIN_MESSAGE);
    }
    private static JScrollPane describe(Map<String,String> values)
    {
        StringBuilder text = new StringBuilder();
        values.forEach((key,value) -> text.append(SettingsCatalog.ALL.get(key).label).append(": ").append(value).append('\n'));
        JTextArea area = new JTextArea(text.toString(),14,24); area.setEditable(false); area.setLineWrap(true);
        return new JScrollPane(area);
    }
}
