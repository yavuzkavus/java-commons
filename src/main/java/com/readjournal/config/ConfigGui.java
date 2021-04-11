package com.readjournal.config;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.readjournal.util.Utils;

public class ConfigGui extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private Config config;
	private Map<String, JComponent> compMap;
	
	private Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

	public ConfigGui(Config config, Map<String, String> labelMap) {
		super(new BorderLayout());
		this.config = config;
		
		Font font = new Font("TimesRoman", Font.BOLD, 12);
		
		Map<String, JPanel> labelPanels = new LinkedHashMap<String, JPanel>();
		Map<String, GridBagConstraints> panelContstraints = new HashMap<String, GridBagConstraints>();
		Map<String, Setting> confMap = config.getConfMap();
		this.compMap = new HashMap<String, JComponent>(confMap.size());
		for(String key : confMap.keySet()) {
			String subKey = key.indexOf('.')>0 ? key.substring(0, key.indexOf('.')) : "common";
			JPanel panel = labelPanels.get(subKey);
			if( panel==null ) {
				panel = new JPanel(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				panelContstraints.put(subKey, gbc);
				gbc.gridy = 0;
				gbc.gridx = GridBagConstraints.RELATIVE;
				gbc.insets = new Insets(4, 10, 4, 10);
				gbc.ipadx = gbc.ipady = 4;
				labelPanels.put(subKey, panel);
			}
		}

		if( labelPanels.isEmpty() )
			return;
		JPanel mainPanel;
		if( labelPanels.size()==1 ) {
			mainPanel = labelPanels.values().iterator().next();
		}
		else {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			for(String key : labelPanels.keySet()) {
				JPanel panel = labelPanels.get(key);
				panel.setBorder(
						BorderFactory.createCompoundBorder(
								emptyBorder,
								BorderFactory.createTitledBorder(labelMap.get(key))
						)
				);
				mainPanel.add(panel);
			}
		}
		
		FontMetrics metrics = new JLabel("abc").getFontMetrics(font);
		int labelWidth = 100,
			labelHeight = metrics.getHeight();
		for(Setting setting : confMap.values()) {
			int w = metrics.stringWidth(setting.getLabel() + " :") + 4;
			if( w>labelWidth )
				labelWidth = w;
		}
		
		Dimension labelSize = new Dimension(labelWidth, labelHeight);
		for(String key : confMap.keySet()) {
			Setting setting = confMap.get(key);
			String subKey = key.indexOf('.')>0 ? key.substring(0, key.indexOf('.')) : "common";
			JPanel panel = labelPanels.get(subKey);
			GridBagConstraints gbc = panelContstraints.get(subKey);

			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.FIRST_LINE_END;
			gbc.weightx = 0;
			gbc.weighty = 0;
			JLabel label = new JLabel(setting.getLabel() + " :");
			label.setFont(font);
			label.setHorizontalAlignment(SwingConstants.RIGHT);
			label.setPreferredSize(labelSize);
			panel.add(label, gbc);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.FIRST_LINE_START;
			gbc.weightx = 1;

			JComponent comp;
			if( setting.isText() ) {
				JTextField field = new JTextField(setting.getValue());
				field.setFont(font);
				
				gbc.fill = GridBagConstraints.HORIZONTAL;
				panel.add(comp = field, gbc);
			}
			else if( setting.isPassword() ) {
				JPasswordField field = new JPasswordField(setting.getValue());

				gbc.fill = GridBagConstraints.HORIZONTAL;
				panel.add(comp = field, gbc);
			}
			else if( setting.isSelect() ) {
				JComboBox<SelectOption> field = new JComboBox<SelectOption>( setting.getOptions() );
				field.setFont(font);
				
				for(SelectOption opt : setting.getOptions())
					if( opt.getValue().toString().equals(setting.getValue()) ) {
						field.setSelectedItem(opt);
						break;
					}
				gbc.fill = GridBagConstraints.HORIZONTAL;
				panel.add(comp = field, gbc);
			}
			else if( setting.isCheckBox() ) {
				JCheckBox field = new JCheckBox();
				if( "true".equals(setting.getValue()) )
					field.setSelected(true);
				panel.add(comp = field, gbc);
			}
			else if( setting.isTextArea() ) {
				JTextArea field = new JTextArea(4, 20);
				field.setFont(font);
				
				field.setText(setting.getValue());
				gbc.fill = GridBagConstraints.BOTH;
				panel.add(new JScrollPane(comp = field), gbc);
			}
			else if( setting.isRadio() ) {
				ButtonGroup group = new ButtonGroup();
				JPanel field = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
				for(SelectOption opt : setting.getOptions()) {
					JRadioButton radio = new JRadioButton(opt.getLabel());
					radio.putClientProperty("_conf_value_", opt.getValue());
					if( opt.getValue().toString().equals(setting.getValue()) )
						radio.setSelected(true);
					group.add(radio);
					field.add(radio);
				}
				panel.add(comp = field, gbc);
			}
			else {
				throw new RuntimeException("Invalid type");
			}
			compMap.put(key, comp);

			gbc.gridy++;
		}

		add(new JScrollPane(mainPanel), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
		
		JButton resetButton = new JButton("Geri Yükle", getIcon("reset.png"));
		resetButton.setPreferredSize(new Dimension(140, resetButton.getPreferredSize().height));
		resetButton.addActionListener(ae->this.resetGuiValues());
		buttonPanel.add(resetButton);
		
		JButton saveButton = new JButton("Kaydet", getIcon("save.png"));
		saveButton.setPreferredSize(new Dimension(140, saveButton.getPreferredSize().height));
		saveButton.addActionListener(ae->assignGuiValues());
		buttonPanel.add(saveButton);
		
		add(buttonPanel, BorderLayout.SOUTH);
	}
	
	@SuppressWarnings("unchecked")
	public void resetGuiValues() {
		Map<String, Setting> confMap = config.getConfMap();
		for(String key : compMap.keySet()) {
			JComponent comp = compMap.get(key);
			Setting setting = confMap.get(key);
			String value = setting.getValue();
			if( setting.isText() ) {
				((JTextField)comp).setText(value);
			}
			else if( setting.isPassword() ) {
				((JPasswordField)comp).setText(value);
			}
			else if( setting.isSelect() ) {
				JComboBox<SelectOption> combo = (JComboBox<SelectOption>)comp;
				for(int i=0, len=combo.getItemCount(); i<len; i++) {
					SelectOption child = combo.getItemAt(i);
					if( Utils.equals(child.getValue(), value) ) {
						combo.setSelectedIndex(i);
						break;
					}
				}
			}
			else if( setting.isCheckBox() ) {
				((JCheckBox)comp).setSelected("true".equals(value));
			}
			else if( setting.isTextArea() ) {
				((JTextArea)comp).setText(value);
			}
			else if( setting.isRadio() ) {
				JPanel field = (JPanel)comp;
				for(int i=0, len=field.getComponentCount(); i<len; i++) {
					JRadioButton radio = ((JRadioButton)field.getComponent(i));
					radio.setSelected( ((String)radio.getClientProperty("_conf_value_")).equals(value) );
				}
			}
		}
	}
	
	public void assignGuiValues() {
		Map<String, Setting> confMap = config.getConfMap();
		Map<String, String> changeds = new HashMap<>();
		Map<String, String> errors = new LinkedHashMap<String, String>();
		for(String key : compMap.keySet()) {
			JComponent comp = compMap.get(key);
			Setting setting = confMap.get(key);
			String value = null;
			if( setting.isText() ) {
				value = ((JTextField)comp).getText().trim();
			}
			else if( setting.isPassword() ) {
				value = new String(((JPasswordField)comp).getPassword()).trim();
			}
			else if( setting.isSelect() ) {
				@SuppressWarnings("unchecked")
				JComboBox<SelectOption> field = (JComboBox<SelectOption>)comp;
				SelectOption selectedOption = (SelectOption)field.getSelectedItem();
				value = selectedOption!=null ? selectedOption.getValue() : null;
			}
			else if( setting.isCheckBox() ) {
				boolean checked = ((JCheckBox)comp).isSelected();
				value = checked ? "true" : "false";
			}
			else if( setting.isTextArea() ) {
				value = ((JTextArea)comp).getText().trim();
			}
			else if( setting.isRadio() ) {
				JPanel field = (JPanel)comp;
				for(int i=0, len=field.getComponentCount(); i<len; i++) {
					JRadioButton radio = ((JRadioButton)field.getComponent(i));
					if( radio.isSelected() )
						value = (String)radio.getClientProperty("_conf_value_");
				}
			}
			String error = Setting.validate(setting, value);
			if( error!=null ) {
				errors.put(key, error);
				continue;
			}
			if( !Utils.equals(value, setting.getValue()) )
				changeds.put(key, value);
		}
		if( errors.size()>0 ) {
			StringBuilder message = new StringBuilder();
			for(String key : errors.keySet()) {
				message.append(confMap.get(key).getLabel()).append(": ").append(errors.get(key));
			}
			JOptionPane.showOptionDialog(
					SwingUtilities.getWindowAncestor(compMap.values().iterator().next()),
					message.toString(),
					"Ayarlar kaydedilemedi",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE,
					null,
					new Object[]{"Tamam"},
					"Tamam" );
		}
		else if( changeds.size()>0 )
			config.save(changeds);
	}
	
	public <T extends JComponent> T getComponentFor(String label) {
		return Utils.cast(compMap.get(label));
	}

	private ImageIcon getIcon(String path) {
		return new ImageIcon( getClass().getResource(path) );
	}
}
