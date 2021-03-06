/* Athena/Aegis Encrypted Chat Platform
 * BugReportInterface.java: Allows users to submit bug reports and feature requests.
 *
 * Copyright (C) 2010  OlympuSoft
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Toolkit;

/**
 * Submit a bug report or feature request to Aegis
 * @author OlmypuSoft
 */
public class BugReportInterface extends JPanel {

	/**
	 * 
	 */
	//Define components
	private JFrame submitBugJFrame;
	private JPanel contentPane, generalInformationJPanel, loginInformationJPanel;
	private JLabel descriptionJLabel = new JLabel("* Brief Description:");
	private JLabel recreationJLabel = new JLabel("* How can we recreate this bug?");
	private JLabel expectedJLabel = new JLabel("What did you expect to happen?");
	private JLabel actualJLabel = new JLabel("What actually happened?");
	private JTextField descriptionJTextField = new JTextField();
	private JTextArea recreationJTextArea;
	private JTextArea expectedJTextArea;
	private JTextArea actualJTextArea;
	private JButton confirmJButton = new JButton("Confirm");
	private JButton cancelJButton = new JButton("Cancel");
	private JButton clearJButton = new JButton("Clear");
	private Border blackline;
	private TitledBorder generalTitledBorder;
	private RSAPublicKeySpec pub;
	private RSAPrivateKeySpec priv;
	private BigInteger publicMod;
	private BigInteger publicExp;
	private BigInteger privateMod;
	private BigInteger privateExp;
	private BigInteger privateModBigInteger;
	private BigInteger privateExpBigInteger;
	private Color goGreen = new Color(51, 153, 51);

	BugReportInterface() {
		//Create the Main Frame
		submitBugJFrame = new JFrame("Submit Bug Report/Feature Request");
		submitBugJFrame.setSize(500, 560);
		submitBugJFrame.setResizable(false);
		submitBugJFrame.setLocationRelativeTo(CommunicationInterface.imContentFrame);

		//Create the content Pane
		contentPane = new JPanel();
		contentPane.setLayout(null);
		contentPane.setVisible(true);

		submitBugJFrame.setIconImage(Toolkit.getDefaultToolkit().getImage("images/logosmall.png"));

		//Initalize borders
		blackline = BorderFactory.createLineBorder(Color.black);
		generalTitledBorder = BorderFactory.createTitledBorder(
				blackline, "Submit a Bug Report/Feature Request");

		//Username Input
		descriptionJTextField = new JTextField();
		descriptionJLabel.setBounds(15, 20, 150, 25);
		descriptionJTextField.setBounds(15, 40, 470, 25);

		recreationJLabel.setBounds(15, 80, 400, 25);
		recreationJTextArea = new JTextArea();
		JScrollPane recreationSP = new JScrollPane(recreationJTextArea);
		recreationSP.setBounds(15, 100, 470, 100);
		recreationSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		expectedJLabel.setBounds(15, 210, 400, 25);
		expectedJTextArea = new JTextArea();
		JScrollPane expectedSP = new JScrollPane(expectedJTextArea);
		expectedSP.setBounds(15, 230, 470, 100);
		expectedSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		actualJLabel.setBounds(15, 340, 400, 25);
		actualJTextArea = new JTextArea();
		JScrollPane actualSP = new JScrollPane(actualJTextArea);
		actualSP.setBounds(15, 360, 470, 100);
		actualSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);







		//Confirm and Cancel JButtons
		confirmJButton.setBounds(280, 490, 100, 25);
		cancelJButton.setBounds(385, 490, 100, 25);
		clearJButton.setBounds(10, 490, 100, 25);


		clearJButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				recreationJTextArea.setText("");
				descriptionJTextField.setText("");
				expectedJTextArea.setText("");
				actualJTextArea.setText("");
			}
		});

		//ActionListener to make the connect menu item connect
		confirmJButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (!(descriptionJTextField.getText().equals("")) && !(recreationJTextArea.getText().equals(""))) {
					sendInfoToAegis(descriptionJTextField.getText(), recreationJTextArea.getText(), expectedJTextArea.getText(), actualJTextArea.getText());
					JOptionPane.showMessageDialog(null, "Thank you for submitting this report. It has been added to our database", "Report Submitted", JOptionPane.INFORMATION_MESSAGE);
					submitBugJFrame.dispose();

				} else {
					JOptionPane.showMessageDialog(null, "Please fill in all required fields.", "Submission Error", JOptionPane.ERROR_MESSAGE);
				}

			}
		});

		cancelJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				submitBugJFrame.dispose();
			}
		});

		//Add all the components to the contentPane
		contentPane.add(descriptionJLabel);
		contentPane.add(descriptionJTextField);
		contentPane.add(recreationJLabel);
		contentPane.add(recreationSP);
		contentPane.add(cancelJButton);
		contentPane.add(expectedJLabel);
		contentPane.add(expectedSP);
		contentPane.add(actualJLabel);
		contentPane.add(actualSP);
		contentPane.add(confirmJButton);
		contentPane.add(clearJButton);

		//Make sure we can see damn thing
		contentPane.setVisible(true);
		contentPane.setBorder(generalTitledBorder);

		//Let the Frame know what's up
		submitBugJFrame.setContentPane(contentPane);
		submitBugJFrame.setVisible(true);
	}

	/**
	 * Compile the report information and send it to Aegis
	 * @param titles Summary of bug/feature
	 * @param recreates Recreation steps for bug
	 * @param expecteds Expected outcome of action
	 * @param actuals The bug
	 */
	public void sendInfoToAegis(String titles, String recreates, String expecteds, String actuals) {

		//Get a connection
		//Client.connect();

		//Give me back my filet of DataOutputStream + DataInputStream
		DataOutputStream dout = Athena.returnDOUT();
		//DataInputStream din = Client.returnDIN();


		try {
			Athena.systemMessage("10");
			dout.writeUTF(Athena.encryptServerPublic(titles));
			dout.writeUTF(Athena.encryptServerPublic(recreates));
			dout.writeUTF(Athena.encryptServerPublic(expecteds));
			dout.writeUTF(Athena.encryptServerPublic(actuals));
			//Close the connection
			//dout.close();
			//Client.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
