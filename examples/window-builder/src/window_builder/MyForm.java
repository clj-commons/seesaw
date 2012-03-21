package window_builder;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;

public class MyForm extends JPanel {
  private JTextField firstName;
  private JTextField lastName;
  private JLabel lblStreet;
  private JTextField textField;
  private JLabel lblCity;
  private JTextField textField_1;
  private JLabel lblState;
  private JComboBox comboBox;
  private JLabel lblState_1;
  private JTextField textField_2;

  /**
   * Create the panel.
   */
  public MyForm() {
    GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[]{72, 134, 0, 0, 0, 0, 0};
    gridBagLayout.rowHeights = new int[]{28, 28, 0, 0};
    gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
    gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
    setLayout(gridBagLayout);
    
    JLabel lblFirstName = new JLabel("First Name:");
    GridBagConstraints gbc_lblFirstName = new GridBagConstraints();
    gbc_lblFirstName.anchor = GridBagConstraints.WEST;
    gbc_lblFirstName.insets = new Insets(0, 0, 5, 5);
    gbc_lblFirstName.gridx = 0;
    gbc_lblFirstName.gridy = 0;
    add(lblFirstName, gbc_lblFirstName);
    
    firstName = new JTextField();
    firstName.setName("first-name");
    firstName.setColumns(10);
    GridBagConstraints gbc_firstName = new GridBagConstraints();
    gbc_firstName.gridwidth = 2;
    gbc_firstName.anchor = GridBagConstraints.NORTH;
    gbc_firstName.fill = GridBagConstraints.HORIZONTAL;
    gbc_firstName.insets = new Insets(0, 0, 5, 5);
    gbc_firstName.gridx = 1;
    gbc_firstName.gridy = 0;
    add(firstName, gbc_firstName);
    
    JLabel lblLastName = new JLabel("Last Name:");
    GridBagConstraints gbc_lblLastName = new GridBagConstraints();
    gbc_lblLastName.anchor = GridBagConstraints.WEST;
    gbc_lblLastName.insets = new Insets(0, 0, 5, 5);
    gbc_lblLastName.gridx = 3;
    gbc_lblLastName.gridy = 0;
    add(lblLastName, gbc_lblLastName);
    
    lastName = new JTextField();
    lastName.setName("last-name");
    lastName.setColumns(10);
    GridBagConstraints gbc_lastName = new GridBagConstraints();
    gbc_lastName.gridwidth = 2;
    gbc_lastName.fill = GridBagConstraints.HORIZONTAL;
    gbc_lastName.insets = new Insets(0, 0, 5, 0);
    gbc_lastName.anchor = GridBagConstraints.NORTH;
    gbc_lastName.gridx = 4;
    gbc_lastName.gridy = 0;
    add(lastName, gbc_lastName);
    
    lblStreet = new JLabel("Street:");
    GridBagConstraints gbc_lblStreet = new GridBagConstraints();
    gbc_lblStreet.anchor = GridBagConstraints.EAST;
    gbc_lblStreet.insets = new Insets(0, 0, 5, 5);
    gbc_lblStreet.gridx = 0;
    gbc_lblStreet.gridy = 1;
    add(lblStreet, gbc_lblStreet);
    
    textField = new JTextField();
    textField.setName("street");
    GridBagConstraints gbc_textField = new GridBagConstraints();
    gbc_textField.gridwidth = 5;
    gbc_textField.insets = new Insets(0, 0, 5, 0);
    gbc_textField.fill = GridBagConstraints.HORIZONTAL;
    gbc_textField.gridx = 1;
    gbc_textField.gridy = 1;
    add(textField, gbc_textField);
    textField.setColumns(10);
    
    lblCity = new JLabel("City:");
    GridBagConstraints gbc_lblCity = new GridBagConstraints();
    gbc_lblCity.anchor = GridBagConstraints.EAST;
    gbc_lblCity.insets = new Insets(0, 0, 0, 5);
    gbc_lblCity.gridx = 0;
    gbc_lblCity.gridy = 2;
    add(lblCity, gbc_lblCity);
    
    textField_1 = new JTextField();
    textField_1.setName("city");
    GridBagConstraints gbc_textField_1 = new GridBagConstraints();
    gbc_textField_1.insets = new Insets(0, 0, 0, 5);
    gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
    gbc_textField_1.gridx = 1;
    gbc_textField_1.gridy = 2;
    add(textField_1, gbc_textField_1);
    textField_1.setColumns(10);
    
    lblState_1 = new JLabel("State:");
    GridBagConstraints gbc_lblState_1 = new GridBagConstraints();
    gbc_lblState_1.insets = new Insets(0, 0, 0, 5);
    gbc_lblState_1.gridx = 2;
    gbc_lblState_1.gridy = 2;
    add(lblState_1, gbc_lblState_1);
    
    comboBox = new JComboBox();
    comboBox.setName("state");
    GridBagConstraints gbc_comboBox = new GridBagConstraints();
    gbc_comboBox.insets = new Insets(0, 0, 0, 5);
    gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
    gbc_comboBox.gridx = 3;
    gbc_comboBox.gridy = 2;
    add(comboBox, gbc_comboBox);
    
    lblState = new JLabel("Zip:");
    GridBagConstraints gbc_lblState = new GridBagConstraints();
    gbc_lblState.anchor = GridBagConstraints.EAST;
    gbc_lblState.insets = new Insets(0, 0, 0, 5);
    gbc_lblState.gridx = 4;
    gbc_lblState.gridy = 2;
    add(lblState, gbc_lblState);
    
    textField_2 = new JTextField();
    textField_2.setName("zip");
    GridBagConstraints gbc_textField_2 = new GridBagConstraints();
    gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
    gbc_textField_2.gridx = 5;
    gbc_textField_2.gridy = 2;
    add(textField_2, gbc_textField_2);
    textField_2.setColumns(10);

  }
}
