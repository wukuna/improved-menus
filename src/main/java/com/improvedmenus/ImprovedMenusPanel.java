package com.improvedmenus;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

final class ImprovedMenusPanel extends PluginPanel
{
    private final ProfileStore store;
    private final MenuEngine engine;
    private final JTextField search = new JTextField();
    private final JPanel settings = new JPanel();
    private final JTextArea preview = new JTextArea(9,18);
    private final JLabel status = new JLabel(" ");
    private final javax.swing.Timer timer;

    ImprovedMenusPanel(ProfileStore store, MenuEngine engine)
    {
        super(false);
        this.store=store; this.engine=engine;
        setLayout(new BorderLayout(0,8));
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        top.add(new JLabel("Menu Workshop")); top.add(new JLabel("<html>Enable the features you want.<br>Hold Ctrl before opening to reveal.</html>"));
        top.add(new JLabel("Search settings")); top.add(search);
        JPanel actions = new JPanel(new GridLayout(0,2,4,4));
        button(actions,"Profiles",this::profiles); button(actions,"Import/export",this::imports);
        button(actions,"Undo",() -> { store.undo(); render(); });
        button(actions,"Reset shown",() -> {
            Map<String,String> defaults = new LinkedHashMap<>();
            for (SettingsCatalog.Setting s : SettingsCatalog.ALL.values()) { if (matches(s)) { defaults.put(s.key,s.defaultValue); } }
            store.apply(defaults); render();
        });
        top.add(actions); top.add(status);
        for (Component child : top.getComponents()) { if (child instanceof JComponent) { ((JComponent)child).setAlignmentX(Component.LEFT_ALIGNMENT); } }
        add(top,BorderLayout.NORTH);
        settings.setLayout(new BoxLayout(settings,BoxLayout.Y_AXIS));
        JScrollPane scroller = new JScrollPane(settings); scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroller,BorderLayout.CENTER);
        preview.setEditable(false); preview.setLineWrap(true); preview.setWrapStyleWord(true); preview.setText(engine.diagnostics());
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(new JLabel("Menu preview and rule diagnostics"),BorderLayout.NORTH);
        bottom.add(new JScrollPane(preview),BorderLayout.CENTER); add(bottom,BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e) { render(); }
            public void removeUpdate(DocumentEvent e) { render(); }
            public void changedUpdate(DocumentEvent e) { render(); }
        });
        timer = new javax.swing.Timer(500,e -> { if (isShowing()) { preview.setText(engine.diagnostics()); } });
        render(); timer.start();
    }
    void stop() { timer.stop(); }
    private boolean matches(SettingsCatalog.Setting s)
    {
        return (s.section + " " + s.label).toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT));
    }
    private void render()
    {
        settings.removeAll(); String lastSection = "";
        for (SettingsCatalog.Setting s : SettingsCatalog.ALL.values())
        {
            if (!matches(s)) { continue; }
            if (!lastSection.equals(s.section)) { settings.add(new JLabel(s.section)); lastSection=s.section; }
            JPanel row = new JPanel(); row.setLayout(new BoxLayout(row,BoxLayout.Y_AXIS));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (s.choices.length == 2 && s.choices[0].equals("false"))
            {
                JCheckBox box = new JCheckBox(s.label,Boolean.parseBoolean(store.value(s)));
                box.addActionListener(e -> run(() -> store.apply(Collections.singletonMap(s.key,Boolean.toString(box.isSelected())))));
                row.add(box);
            }
            else if (s.choices.length > 0)
            {
                row.add(new JLabel(s.label)); JComboBox<String> box = new JComboBox<>(s.choices);
                box.setRenderer(new DefaultListCellRenderer() {
                    @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                        String text=String.valueOf(value);
                        if (text.equals("LEFT_CLICK_OFF")) { text="Right-click only"; }
                        else if (text.equals("DISABLED")) { text="Hidden"; }
                        else if (text.equals("CONTROL")) { text="Ctrl"; }
                        else { text=text.charAt(0)+text.substring(1).toLowerCase(Locale.ROOT); }
                        return super.getListCellRendererComponent(list,text,index,selected,focus);
                    }
                });
                box.setSelectedItem(store.value(s));
                box.addActionListener(e -> run(() -> store.apply(Collections.singletonMap(s.key,(String)box.getSelectedItem())))); row.add(box);
            }
            else if (s.key.equals("minimumOptions"))
            {
                row.add(new JLabel(s.label));
                int count=1;
                try { s.validate(store.value(s)); count=Integer.parseInt(store.value(s)); } catch (IllegalArgumentException ignored) { }
                JSpinner spinner=new JSpinner(new SpinnerNumberModel(count,1,500,1));
                spinner.addChangeListener(e -> run(() -> store.apply(Collections.singletonMap(s.key,spinner.getValue().toString())))); row.add(spinner);
            }
            else if (s.key.equals("defaultPickerColor") || s.key.equals("optionCountColor"))
            {
                button(row,s.label,() -> {
                    Color color=JColorChooser.showDialog(this,s.label,SettingsCatalog.colorOrDefault(store.value(s),Color.WHITE));
                    if (color != null) { store.apply(Collections.singletonMap(s.key,String.format("#%06x",color.getRGB() & 0xffffff))); }
                });
            }
            else
            {
                row.add(new JLabel(s.label)); JTextArea area = new JTextArea(store.value(s),4,16);
                area.setLineWrap(true); area.setWrapStyleWord(true); row.add(new JScrollPane(area));
                button(row,"Validate and apply",() -> store.apply(Collections.singletonMap(s.key,area.getText())));
            }
            settings.add(row); settings.add(Box.createVerticalStrut(8));
        }
        settings.revalidate(); settings.repaint();
    }
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
            { store.apply(values); render(); }
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
            { store.apply(values); render(); }
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
