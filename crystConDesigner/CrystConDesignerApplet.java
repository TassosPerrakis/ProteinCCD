package crystConDesigner;

import java.applet.AppletContext;
import java.awt.*;
import java.awt.event.*;

import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.net.*;
import java.io.*;

public class CrystConDesignerApplet extends JApplet implements MouseListener {

	private String smartGraphicsJobId;
	private JTextComponent inputSequence;
	private JTextComponent secondaryStruct;
	private JTextComponent secondaryStructLabel;
	private JTable oligos;
	private JTable proteins;
	private JTextField oligoName;
	private JSpinner temperature;
	private JSpinner numberOfBases;
	private JRadioButton useTemp;
	private JRadioButton useNumb;
	private JTextField forwardOverhang;
	private JTextField reverseOverhang;
	
	private JButton smartOutputButton;
	private String proteinSequence;
	private Container content;
	private JDialog waitPopup;
	private JTextArea waitPopupText;

	private JRadioButton markStart;
	private JRadioButton markStop;

	private JCheckBox aligned;
	private Font font;
	
	private int startRonn, stopRonn, startIupred, stopIupred, 
		startGlobPlot, stopGlobPlot,startDisembl,stopDisembl;

	JMenu optionMenu = new JMenu("Predictions");
	private String[] optionNames;
	private char[] optionSettings;
	
	private boolean restDone = false;
	Preferences userPrefs;
	
	public void init() {
		try {
			userPrefs = Preferences.userNodeForPackage (getClass ());
		}catch (Throwable t) {
			System.out.println("User preferences not available");
		}
		
		// Execute a job on the event-dispatching thread:
		// creating this applet's GUI.
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					createGUI();
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	public void createGUI() {
		
		String lengthMethod="T";
		String forward = "cagggacccggt";
		String reverse = "cgaggagaagcccggtta";
		double selectedTemperature = 65.0;
		int selectedNumberOfBases = 20;
		String oligoPrefix = "";
		
		if (userPrefs != null) {
			lengthMethod = userPrefs.get("lengthMethod","T");
			selectedTemperature = userPrefs.getDouble("temperature",65.0);
			selectedNumberOfBases = userPrefs.getInt("numberOfBases",20);
			forward = userPrefs.get("forward","cagggacccggt");
			reverse = userPrefs.get("reverse","cgaggagaagcccggtta");
			oligoPrefix = userPrefs.get("oligoPrefix","");
		}
			
		// GUI font needs to monospaced to keep sequences and annotations
		// aligned
		font = new Font("Monospaced", Font.PLAIN, 12);

		// The input sequence box
		inputSequence = createTextComponent(4);

		// The secondary structure etc output box
		secondaryStruct = createTextComponent(15);
		secondaryStruct.addMouseListener(this);
		secondaryStructLabel = createTextComponent(15, 20);
		secondaryStructLabel.setEditable(false);

		// The generated oligos go into a table
		DefaultTableModel model = new DefaultTableModel();
		oligos = new JTable(model);
		((DefaultTableCellRenderer) oligos.getTableHeader()
				.getDefaultRenderer())
				.setHorizontalAlignment(SwingConstants.LEFT);

		model.addColumn("Name");
		model.addColumn("Oligo");

		oligos.setColumnSelectionAllowed(true);
		oligos.getColumnModel().getColumn(0).setPreferredWidth(200);
		oligos.getColumnModel().getColumn(1).setPreferredWidth(700);
		oligos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		oligos.setFont(font);

		// The corresponig protein sequences also go in a table
		model = new DefaultTableModel();
		proteins = new JTable(model);
		((DefaultTableCellRenderer) proteins.getTableHeader()
				.getDefaultRenderer())
				.setHorizontalAlignment(SwingConstants.LEFT);
		model.addColumn("Name");
		model.addColumn("Sequences");

		proteins.setColumnSelectionAllowed(true);
		proteins.getColumnModel().getColumn(0).setPreferredWidth(200);
		proteins.getColumnModel().getColumn(1).setPreferredWidth(700);
		proteins.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		proteins.setFont(font);

		
		// The selection of oligos by temperature or number of base pairs
		SpinnerNumberModel tempList = new SpinnerNumberModel(selectedTemperature, 30.0, 100.0,
				1.0);
		temperature = new JSpinner(tempList);
		temperature.setToolTipText("Tm");

		tempList = new SpinnerNumberModel(selectedNumberOfBases, 0, 40, 1);
		numberOfBases = new JSpinner(tempList);
		numberOfBases.setToolTipText("Number of bases");
		
		// The overhangs to be added
		forwardOverhang = new JTextField(forward);
		forwardOverhang.setToolTipText("Forward overhang for the oligos");
		reverseOverhang = new JTextField();
		reverseOverhang.setToolTipText("Reverse overhang for the oligos");
		reverseOverhang.setText(reverse);
		
		oligoName = new JTextField();
		oligoName.setText(oligoPrefix);
		oligoName.setColumns(10);
 
		// Now we need to lay out the components
		// Just do it here
		content = getContentPane();
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		JPanel subPanel = new JPanel(new GridBagLayout());

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.ipadx = 6;
		c.ipady = 2;
		SwitchTNAction switchTN = new SwitchTNAction();
		useTemp = new JRadioButton(switchTN);
		useTemp.setText(" T:");
		useTemp.setToolTipText("Use Tm to determine oligo length");
		subPanel.add(useTemp, c);
		c.gridx = 1;
		subPanel.add(temperature, c);
		c.gridx = 2;
		useNumb = new JRadioButton(switchTN);
		useNumb.setText(" N:");
		useNumb.setToolTipText("Use fixed number of bases for oligo length");
		subPanel.add(useNumb, c);
		c.gridx = 3;
		subPanel.add(numberOfBases, c);

		if (lengthMethod.matches("T")) {
			useTemp.setSelected(true);
			numberOfBases.setEnabled(false);
		}else if (lengthMethod.matches("N")) {
			useNumb.setSelected(true);
			temperature.setEnabled(false);
		}
		
		
		ButtonGroup group = new ButtonGroup();
		group.add(useTemp);
		group.add(useNumb);

		c.gridx = 4;
		subPanel.add(new JLabel(" FW Overhang"), c);
		c.weightx = 1;
		c.gridx = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		subPanel.add(forwardOverhang, c);
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 6;
		subPanel.add(new JLabel(" RV Overhang"), c);
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 7;
		subPanel.add(reverseOverhang, c);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 0.0;
		panel.add(subPanel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new JSeparator());

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(new JLabel(" Input DNA sequence:"), c);

		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(createToolBar1(), c);
		
		inputSequence.setToolTipText("Paste or load your DNA sequence here");
		JScrollPane scrollPane1 = new JScrollPane(inputSequence);
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 0.05;
		panel.add(scrollPane1, c);

		JPanel predictPanel = new JPanel(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		predictPanel.add(new JLabel(" Output predictions"), c);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		predictPanel.add(createToolBar2(), c);

		JScrollPane scrollLeft = new JScrollPane(secondaryStructLabel,
				JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JScrollPane scrollRight = new JScrollPane(secondaryStruct);
		secondaryStructLabel.setToolTipText("This is where the predictions will be displayed");
		secondaryStruct.setToolTipText("This is where the predictions will be displayed");
		// Couple the Scrolling of the left window to the right bar
		JScrollBar leftBar = scrollLeft.getVerticalScrollBar();
		JScrollBar rightBar = scrollRight.getVerticalScrollBar();

		MyChangeListener listener = new MyChangeListener(leftBar.getModel());
		rightBar.getModel().addChangeListener(listener);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				scrollLeft, scrollRight);

		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1.0;
		c.ipadx = 10;
		c.weighty = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;

		predictPanel.add(split, c);

		JPanel outputPanel = new JPanel(new GridBagLayout());

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		outputPanel.add(new JLabel(" Oligos"), c);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		outputPanel.add(createToolBar3(), c);

		JScrollPane scrollPane3 = new JScrollPane(oligos);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		outputPanel.add(scrollPane3, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		outputPanel.add(new JLabel(" Sequences"), c);

		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		outputPanel.add(createToolBar4(), c);

		JScrollPane scrollPane5 = new JScrollPane(proteins);
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		outputPanel.add(scrollPane5, c);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				predictPanel, outputPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(250);
		splitPane.setResizeWeight(1.0);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;

		panel.add(splitPane, c);
		content.add(panel);

		setJMenuBar(createMenuBar());

		setSize(1000, 700);
	
	}

	

	// Creates the top toolbar for the input sequence
	protected JToolBar createToolBar1() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setLayout(new FlowLayout(FlowLayout.LEADING));
		bar.add(new PostAction()).setToolTipText("Submit the sequence to the selected prediction servers (see 'Options')");

		bar.addSeparator();
		// Add simple actions for opening & saving.
		bar.add(new OpenAction(inputSequence)).setToolTipText("Load a DNA sequence from file");
		bar.addSeparator();
		bar.add(new GetLastSequenceAction()).setToolTipText("Get last used DNA sequence");
		bar.addSeparator();		
		bar.add(new SaveAction(inputSequence)).setToolTipText("Save input sequence");
		bar.addSeparator();
		bar.add(new ClearAction(inputSequence)).setToolTipText("Clear input sequence");
		return bar;
	}

	// Creates the toolbar for the sec. structure output/ oligo selection
	protected JToolBar createToolBar2() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setLayout(new FlowLayout(FlowLayout.LEADING));
		markStart = new JRadioButton("Mark start");
		markStart.setToolTipText("Start marking 'start' points in the protein sequence");
		bar.add(markStart);
		bar.addSeparator();
		markStop = new JRadioButton("Mark stop");
		markStop.setToolTipText("Start marking 'stop' points in the protein sequence");
		bar.add(markStop);
		ButtonGroup group = new ButtonGroup();
		group.add(markStart);
		group.add(markStop);

		bar.addSeparator();
		JButton undoMark = new JButton(new UndoMarkAction());
		undoMark.setToolTipText("Clear last marked point in the protein sequence");
		bar.add(undoMark);
	
		bar.addSeparator();
		JButton clearStart = new JButton(new ClearStartAction());
		clearStart.setToolTipText("Clear all marked 'start' points in the protein sequence");
		bar.add(clearStart);
		bar.addSeparator();
								
		JButton clearStop = new JButton(new ClearStopAction());
		clearStop.setToolTipText("Clear all marked 'stop' points in the protein sequence");
		bar.add(clearStop);
		bar.addSeparator();
		smartOutputButton = new JButton(new SmartOutputAction());
		smartOutputButton.setToolTipText("Open the graphical output from the SMART server in a new window (if pop-ups are allowed)");		
		smartOutputButton.setEnabled(false);
		bar.add(smartOutputButton);
		bar.addSeparator();
		bar.add(new SaveAction(secondaryStructLabel,secondaryStruct)).setToolTipText("Save prediction results");
		return bar;
	}

	// Create the toolbar for gettting the oligos, based on the markers
	protected JToolBar createToolBar3() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setLayout(new FlowLayout(FlowLayout.LEADING));
		bar.add(new JLabel("Name:"));
		oligoName.setToolTipText("Prefix for the oligo names");
		bar.add(oligoName);
		bar.addSeparator();
		bar.add(new GetOligosAction()).setToolTipText("Construct the oligos (first set some 'start' and 'stop' points)");
		bar.addSeparator();
		bar.add(new SaveAction((JTable) oligos)).setToolTipText("Save oligos");
		bar.addSeparator();
		bar.add(new ClearAction((JTable) oligos)).setToolTipText("Clear oligos");
		bar.addSeparator();
		aligned = new JCheckBox("Keep sequences aligned", false);
		aligned.setToolTipText("Tick (before pressing 'Get') to keep resulting sequences aligned in bottom window");
		bar.add(aligned);
		return bar;
	}

	// Create toolbar for the protein constructs
	protected JToolBar createToolBar4() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.setLayout(new FlowLayout(FlowLayout.LEADING));
		bar.add(new SaveAction((JTable) proteins)).setToolTipText("Save protein sequences");
		bar.addSeparator();
		bar.add(new ClearAction((JTable) proteins)).setToolTipText("Clear protein sequences");
		return bar;
	}

	// Create the JTextComponent subclass.
	protected JTextComponent createTextComponent() {
		JTextArea ta = new JTextArea();
		ta.setLineWrap(false);
		ta.setFont(font);
		return ta;
	}

	protected JTextComponent createTextComponent(int rows) {
		JTextArea ta = new JTextArea();
		ta.setLineWrap(false);
		ta.setFont(font);
		ta.setRows(rows);
		return ta;
	}

	protected JTextComponent createTextComponent(int rows, int columns) {
		JTextArea ta = new JTextArea();
		ta.setLineWrap(false);
		ta.setFont(font);
		ta.setRows(rows);
		ta.setColumns(columns);
		return ta;
	}

	// Call all the requested web servers and process their output
	class GetPredictions extends Thread {

		String output = null;
		String sequence = "";
		String dnaSequence = "";
		String line = null;

		BufferedReader urlReader;

		public GetPredictions() {

		}

		public void run() {

			restDone = false;
			proteinSequence = translate();
			if (proteinSequence.length()<2) {
				waitPopupText.append("Input does not seem to be a DNA sequence\n");
				return;
		}
		if (proteinSequence.length()<10) {
			waitPopupText.append("DNA sequence codes for  less than 10 amino acids	\n");
			return;
		}
		// Not pretty, but will do for now
			String[] urlNames = { "TEXT",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_sopma.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_hnn.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_mlr.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_dpm.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_dsc.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_gor4.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_phd.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_preda.pl",
					"http://npsa-prabi.ibcp.fr/cgi-bin/secpred_simpa96.pl",
					"SEPARATOR", "TEXT", 
					"http://www.strubi.ox.ac.uk/RONN",
					"http://iupred.enzim.hu/pred.php", 
					"http://dis.embl.de/cgiDict.py",
					"http://globplot.embl.de/cgiDict.py",
					"SEPARATOR", "TEXT",
					"http://npsa-prabi.ibcp.fr/cgi-bin/primanal_lupas.pl",
					"http://phobius.sbc.su.se/cgi-bin/predict.pl",
					"SEPARATOR", "TEXT",
					"http://smart.embl-heidelberg.de/smart/show_motifs.pl",	
					"globplot",
					"http://www.tuat.ac.jp/~domserv/cgi-bin/DLP-SVM.cgi"};

			String[][] paramNames = new String[25][];
			String[][] paramValues = new String[25][];

			paramNames[1] = new String[] { "notice", "ali_width", "states" };
			paramValues[1] = new String[] { proteinSequence, "10000", "4" };
			paramNames[2] = new String[] { "notice", "ali_width" };
			paramValues[2] = new String[] { proteinSequence, "10000" };
			paramNames[3] = new String[] { "notice", "ali_width" };
			paramValues[3] = new String[] { proteinSequence, "10000" };
			paramNames[4] = new String[] { "notice", "ali_width" };
			paramValues[4] = new String[] { proteinSequence, "10000" };
			paramNames[5] = new String[] { "notice", "ali_width" };
			paramValues[5] = new String[] { proteinSequence, "10000" };
			paramNames[6] = new String[] { "notice", "ali_width" };
			paramValues[6] = new String[] { proteinSequence, "10000" };
			paramNames[7] = new String[] { "notice", "ali_width" };
			paramValues[7] = new String[] { proteinSequence, "10000" };
			paramNames[8] = new String[] { "notice", "ali_width",
					"predatorssmat" };
			paramValues[8] = new String[] { proteinSequence, "10000", "dssp" };
			paramNames[9] = new String[] { "notice", "ali_width" };
			paramValues[9] = new String[] { proteinSequence, "10000" };

			paramNames[12] = new String[] { "sequence", "display_probs" };
			paramValues[12] = new String[] { proteinSequence, "y" };
			paramNames[13] = new String[] { "seq", "type", "output" };
			paramValues[13] = new String[] { proteinSequence, "long", "data" };
			paramNames[14] = new String[] { "key","sequence_string"};
			paramValues[14] = new String[] { "process",proteinSequence};
			paramNames[15] = new String[] { "key","sequence_string"};
			paramValues[15] = new String[] { "process",proteinSequence };

			paramNames[18] = new String[] { "notice", "matrix", "weight",
					"format", "ali_width" };
			paramValues[18] = new String[] { proteinSequence, "2", "N", "a",
					"10000" };

			paramNames[19] = new String[] {"protseq","format"};
			paramValues[19] = new String[] {proteinSequence,"nog"};


			paramNames[22] = new String[] { "SEQUENCE", "INCLUDE_BLAST",
					"DO_PFAM", "INCLUDE_SIGNALP", "DO_PROSPERO", "DO_DISEMBL",
					"DO_SMART", "TEXTONLY" };
			paramValues[22] = new String[] { proteinSequence, "INCLUDE_BLAST",
					"DO_PFAM", "INCLUDE_SIGNALP", "DO_PROSPERO", "DO_DISEMBL",
					"Sequence smart", "1" };
			
			paramNames[24] = new String[] { "sequence", "check_all",
					"check_long", "check_short", "check_joint",
					"threshold_all", "threshold_long", "threshold_short",
					"offset_all", "offset_long", "offset_short", "rank_all",
					"rank_long", "rank_short", "rank_joint", "Predict" };
			paramValues[24] = new String[] { proteinSequence, "on", "on", "on",
					"on", "-0.3", "0.5", "-0.5", "60", "60", "60", "2", "2",
					"2", "2", "Predict" };

			boolean startedSmart = false;
			StringBuffer globdoms = null;
			
			int i = 22;
			// First get SMART runId and start SMART wait thread
			JMenuItem option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Submitting " + optionNames[i] + " job\n");

				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				Pattern flag = Pattern.compile("<h2>Job");
				Pattern full = Pattern.compile("queue full");
				try {
					READ: while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							String words[] = line.split(" ", 0);
							String jobId = words[1];
							urlReader.close();
							SmartOutput smartOutput = new SmartOutput(jobId);
							smartOutput.start();
							startedSmart = true;
							break READ;
						} else if (full.matcher(line).find()) {
							waitPopupText
									.append("*** Smart processing queue full. No SMART output ***\n");
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
				// Generate HTML page with graphics and all
				// set the textonly from the end of the paramlist to 0
				// This page can then be inspected by the user if he wants
				paramNames[22] = new String[] { "SEQUENCE", "INCLUDE_BLAST",
						"DO_PFAM", "INCLUDE_SIGNALP", "DO_PROSPERO",
						"DO_DISEMBL", "DO_SMART" };
				paramValues[22] = new String[] { proteinSequence,
						"INCLUDE_BLAST", "DO_PFAM", "INCLUDE_SIGNALP",
						"DO_PROSPERO", "DO_DISEMBL", "Sequence smart" };

				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				flag = Pattern.compile("<h2>Job");
				try {
					READ: while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							String words[] = line.split(" ", 0);
							smartGraphicsJobId = words[1];
							urlReader.close();
							break READ;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}

			}

			Pattern flag = Pattern.compile("  \\|         \\|         \\|  ");
			Pattern fontOpen = Pattern.compile("<FONT COLOR=\\w+>");
			Pattern fontClose = Pattern.compile("</FONT>");

			// Call and process the ibcp servers. Luckily they have unifor
			// output
			i = 0;
			for (i = 1; i < 10; i++) {
				option = optionMenu.getItem(i);
				System.out.println(optionNames[i] + " " + urlNames[i]);
				optionSettings[i]='0';
				if (option.isSelected()) {
					optionSettings[i]='1';
					System.out.println("SELECTED");

					waitPopupText.append("Waiting for " + optionNames[i]
							+ "...");

					urlReader = openURL(urlNames[i], paramNames[i],
							paramValues[i]);

					try {
						READ: while ((line = urlReader.readLine()) != null) {
							if (flag.matcher(line).find()) {
								urlReader.readLine();
								output = urlReader.readLine();
								output = fontOpen.matcher(output)
										.replaceAll("");
								output = fontClose.matcher(output).replaceAll(
										"");
								output = output.replace('c', '-');
								output = output.replace('C', '-');
								secondaryStruct.setText(secondaryStruct
										.getText()
										+ "\n" + output);
								secondaryStructLabel
										.setText(secondaryStructLabel.getText()
												+ "\nSecStruc "
												+ optionNames[i]);
								break READ;
							}
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
					waitPopupText.append("    Done\n");
				}
			}

			// The RONN disorder server
			i = 12;
			startRonn = 0;
			stopRonn = 0;
			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				flag = Pattern.compile("</font></pre><P><pre>");
				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);
				try {
					output = "";
					boolean stop = false;

					READ: while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							flag = Pattern.compile("Back to");
							while ((line = urlReader.readLine()) != null) {
								if (flag.matcher(line).find()) {
									stop = true;
									String words[] = line.split("</pre>");
									line = words[0];
								}
								String words[] = line.split("\\s+", 0);
								// if (Double.valueOf(words[1])>0.5) {
								// output += "*";
								// }else{
								// output += " ";
								// }
								double d = Double.valueOf(words[1])
										.doubleValue();
								int score = (int) Math.floor(10.0 * d);
								if (score > 0) {
									output += score;
								} else {
									output += "-";
								}
								if (stop) {
									break READ;
								}
							}
						}
					}
					startRonn = secondaryStruct.getText().length() + 1;
					secondaryStruct.setText(secondaryStruct.getText() + "\n"
							+ output);
					stopRonn = startRonn + output.length();
					secondaryStructLabel.setText(secondaryStructLabel.getText()
							+ "\nDisorder " + optionNames[i]);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				waitPopupText.append("    Done\n");
			}

			// The IUPred disorder server
			i = 13;
			startIupred = 0;
			stopIupred = 0;
			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				flag = Pattern.compile("Disorder Tendency");
				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				try {
					output = "";
					READ: while ((line = urlReader.readLine()) != null) {

						if (flag.matcher(line).find()) {
							Pattern end = Pattern.compile("</Table>");
							flag = Pattern.compile("</tr>");
							Pattern nan = Pattern.compile("[^0-9.]");
							while ((line = urlReader.readLine()) != null) {
								// System.out.println("line: " + line);
								if (end.matcher(line).find()) {
									break READ;
								}
								if (!flag.matcher(line).find()) {
									line = urlReader.readLine();
								}
								line = nan.matcher(line).replaceAll("");
								
								double d = Double.valueOf(line).doubleValue();
								int score = (int) Math.floor(10.0 * d);
								if (score > 0) {
									output += score;
								} else {
									output += "-";
								}
							}
						}
					}
					startIupred = secondaryStruct.getText().length() + 1;
					secondaryStruct.setText(secondaryStruct.getText() + "\n"
							+ output);
					stopIupred = startIupred + output.length();
					secondaryStructLabel.setText(secondaryStructLabel.getText()
							+ "\nDisorder " + optionNames[i]);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				waitPopupText.append("    Done\n");
			}

			// The disembl disorder server
			i = 14;
			startDisembl = 0;
			stopDisembl = 0;

			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				flag = Pattern.compile("_LOOPS");
				Pattern flag2 = Pattern.compile("_HOTLOOPS");
				Pattern flag3 = Pattern.compile("_REM");
				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				StringBuffer disorder = new StringBuffer(proteinSequence);
				int l = proteinSequence.length();
				for (int k = 0; k < l; k++) {
					disorder.setCharAt(k, '-');
				}
			
				try {
					while ((line = urlReader.readLine()) != null) {
//						if (flag.matcher(line).find() || flag2.matcher(line).find() || flag3.matcher(line).find()) {
// Just report the HotLoops from disEMBL
						if (flag2.matcher(line).find() ) {
							Pattern endFlag = Pattern.compile("<");
							boolean done = false;
							while ((line = urlReader.readLine()) != null) {
								if (endFlag.matcher(line).find()) {
									line = line.split("<")[0];
									done=true;
								}
								String words[] = line.split(",");
								for (int ii=0; ii < words.length; ii++) {
									String words2[] = words[ii].split("-");
									int start = Integer.valueOf(words2[0].trim()).intValue() - 1;
									int stop = Integer.valueOf(words2[1].trim()).intValue();
									for (int k = start; k < stop; k++) {
										disorder.setCharAt(k, 'd');
									}
								}
								if (done)
									break;
							}
						}
					}
					startDisembl = secondaryStruct.getText().length() + 1;
					secondaryStruct.setText(secondaryStruct.getText() + "\n"
							+ disorder);
					stopDisembl = startDisembl + disorder.length();

					secondaryStructLabel.setText(secondaryStructLabel.getText()
							+ "\nDisorder " + optionNames[i]);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				waitPopupText.append("    Done\n");
			}

			// The GlobPlot disorder server
			i = 15;
			startGlobPlot = 0;
			stopGlobPlot = 0;

			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected() || optionMenu.getItem(22).isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				flag = Pattern.compile("_Disorder");
				Pattern flag2 = Pattern.compile("_GlobDoms");
				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				StringBuffer disorder = new StringBuffer(proteinSequence);
				int l = proteinSequence.length();
				for (int k = 0; k < l; k++) {
					disorder.setCharAt(k, '-');
				}
				globdoms = new StringBuffer(proteinSequence);
				l = proteinSequence.length();
				for (int k = 0; k < l; k++) {
					globdoms.setCharAt(k, '-');
				}

				
				try {
					while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							Pattern endFlag = Pattern.compile("<");
							boolean done = false;
							while ((line = urlReader.readLine()) != null) {
								if (endFlag.matcher(line).find()) {
									line = line.split("<")[0];
									done=true;
								}
								String words[] = line.split(",");
								for (int ii=0; ii < words.length; ii++) {
									String words2[] = words[ii].split("-");
									int start = Integer.valueOf(words2[0].trim()).intValue() - 1;
									int stop = Integer.valueOf(words2[1].trim()).intValue();
									for (int k = start; k < stop; k++) {
										disorder.setCharAt(k, 'd');
									}
								}
								if (done)
									break;
							}
						}
						if (flag2.matcher(line).find()) {
							Pattern endFlag = Pattern.compile("<");
							boolean done = false;
							while ((line = urlReader.readLine()) != null) {
								if (endFlag.matcher(line).find()) {
									line = line.split("<")[0];
									done=true;
								}
								String words[] = line.split(",");
								for (int ii=0; ii < words.length; ii++) {
									String words2[] = words[ii].split("-");
									int start = Integer.valueOf(words2[0].trim()).intValue() - 1;
									int stop = Integer.valueOf(words2[1].trim()).intValue();
									for (int k = start; k < stop; k++) {
										globdoms.setCharAt(k, 'g');
									}
								}
								if (done)
									break;
							}
						}
					}
					startGlobPlot = secondaryStruct.getText().length() + 1;
					secondaryStruct.setText(secondaryStruct.getText() + "\n"
							+ disorder);
					stopGlobPlot = startGlobPlot + disorder.length();

					secondaryStructLabel.setText(secondaryStructLabel.getText()
							+ "\nDisorder " + optionNames[i]);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				waitPopupText.append("    Done\n");
			}
			// Coiled-coil Lupas
			i = 18;
			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				flag = Pattern.compile("  \\|         \\|         \\|  ");
				Pattern zero = Pattern.compile("0");
				try {
					READ: while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							urlReader.readLine();
							urlReader.readLine();
							output = urlReader.readLine();
							output = fontOpen.matcher(output).replaceAll("");
							output = fontClose.matcher(output).replaceAll("");
							output = zero.matcher(output).replaceAll("-");
							secondaryStruct.setText(secondaryStruct.getText()
									+ "\n" + output);
							secondaryStructLabel.setText(secondaryStructLabel
									.getText()
									+ "\nFeature  " + optionNames[i]);
							break READ;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
				waitPopupText.append("    Done\n");
			}
			// TM helix prediction
			i = 19;
			option = optionMenu.getItem(i);
			System.out.println(optionNames[i] + " " + urlNames[i]);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				System.out.println("Selected");
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				StringBuffer tm = new StringBuffer(proteinSequence);
				int l = proteinSequence.length();
				for (int k = 0; k < l; k++) {
					tm.setCharAt(k, '-');
				}
				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				flag = Pattern.compile("TRANSMEM");
				Pattern flag2 = Pattern.compile("SIGNAL");
				try {
					while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							String words[] = line.split("\\s+");
							int start = Integer.valueOf(words[2]).intValue() - 1;
							int stop = Integer.valueOf(words[3]).intValue();
							for (int k = start; k < stop; k++) {
								tm.setCharAt(k, 't');
							}
						}else if (flag2.matcher(line).find()) {
							String words[] = line.split("\\s+");
							int start = Integer.valueOf(words[2]).intValue() - 1;
							int stop = Integer.valueOf(words[3]).intValue();
							for (int k = start; k < stop; k++) {
								tm.setCharAt(k, 's');
							}
						}
					}

					secondaryStruct.setText(secondaryStruct.getText() + "\n"
							+ tm);
					secondaryStructLabel.setText(secondaryStructLabel.getText()
								+ "\nFeature  " + optionNames[i]);
				} catch (Throwable t) {
					t.printStackTrace();
				}	
				waitPopupText.append("    Done\n");
			}
		
			i = 23;
			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {

				secondaryStruct.setText(secondaryStruct.getText() + "\n"
						+ globdoms);

				secondaryStructLabel.setText(secondaryStructLabel.getText()
						+ "\nDomains " + optionNames[i]);
			}


			// DLP-SVM domain linker prediction
			i = 24;
			option = optionMenu.getItem(i);
			optionSettings[i]='0';
			if (option.isSelected()) {
				optionSettings[i]='1';
				waitPopupText.append("Waiting for " + optionNames[i] + "...");

				StringBuffer dlp = new StringBuffer(proteinSequence);
				int l = proteinSequence.length();
				for (int k = 0; k < l; k++) {
					dlp.setCharAt(k, '-');
				}
				urlReader = openURL(urlNames[i], paramNames[i], paramValues[i]);

				flag = Pattern.compile("SVM-Joint");
				try {
					while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							break;
						}

					}
					flag = Pattern.compile("Peak Position");
					while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							break;
						}
					}
					flag = Pattern.compile("</table>");
					while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							break;
						}

						String words[] = line.split("<td>", 0);
						Pattern space = Pattern.compile("\\s+");
						words[4] = space.matcher(words[4]).replaceAll("");
						String words2[] = words[4].split("-", 0);

						int start = Integer.valueOf(words2[0]).intValue() - 1;
						int stop = Integer.valueOf(words2[1]).intValue();
						for (int k = start; k < stop; k++) {
							dlp.setCharAt(k, 'l');
						}
					}
					secondaryStruct.setText(secondaryStruct.getText() + "\n"
							+ dlp);
					secondaryStructLabel.setText(secondaryStructLabel.getText()
							+ "\nDomains  " + optionNames[i]);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				waitPopupText.append("    Done\n");
			}
			

			// Mark the scores that are over a threshold for the disorder
			// Highlighter is reset if we setText, so we need to do it here
			if (optionMenu.getItem(12).isSelected()) {
				markText(secondaryStruct, startRonn, stopRonn, 5);
			}
			if (optionMenu.getItem(13).isSelected()) {
				markText(secondaryStruct, startIupred, stopIupred, 5);
			}
			if (optionMenu.getItem(14).isSelected()) {
				markText(secondaryStruct, startDisembl, stopDisembl, 'd');
			}
			if (optionMenu.getItem(15).isSelected()) {
				markText(secondaryStruct, startGlobPlot, stopGlobPlot, 'd');
			}

		
			if (startedSmart) {
				restDone = true;
				waitPopupText.append("Waiting for " + optionNames[22] + "...");
			} else {
				waitPopup.dispose();
			}
			
			if (userPrefs != null) {
				userPrefs.put("optionSettings",new String(optionSettings));
				userPrefs.put("forward", forwardOverhang.getText());
				userPrefs.put("reverse", reverseOverhang.getText());
				if (useTemp.isSelected()) {
					userPrefs.put("lengthMethod","T");
				}else if (useNumb.isSelected()) {	
					userPrefs.put("lengthMethod","N");
				}		
				userPrefs.put("lastSequence",inputSequence.getText());
				Double temp= (Double) temperature.getValue();
				userPrefs.putDouble("temperature", temp.doubleValue());
				Integer numb= (Integer) numberOfBases.getValue();
				userPrefs.putInt("numberOfBases",numb.intValue());
				userPrefs.put("oligoPrefix",oligoName.getText());
			}
		}
	}
	
	// Translate a nucleotide sequence to a protein sequence
	public String translate() {

		Pattern nonSeq = Pattern.compile("[^a-zA-Z]");

		String[] residues = { "F", "F", "L", "L", "S", "S", "S", "S", "Y", "Y",
				"-", "-", "C", "C", "-", "W", "L", "L", "L", "L", "P", "P",
				"P", "P", "H", "H", "Q", "Q", "R", "R", "R", "R", "I", "I",
				"I", "M", "T", "T", "T", "T", "N", "N", "K", "K", "S", "S",
				"R", "R", "V", "V", "V", "V", "A", "A", "A", "A", "D", "D",
				"E", "E", "G", "G", "G", "G" };

		String[] codons = { "TTT", "TTC", "TTA", "TTG", "TCT", "TCC", "TCA",
				"TCG", "TAT", "TAC", "TAA", "TAG", "TGT", "TGC", "TGA", "TGG",
				"CTT", "CTC", "CTA", "CTG", "CCT", "CCC", "CCA", "CCG", "CAT",
				"CAC", "CAA", "CAG", "CGT", "CGC", "CGA", "CGG", "ATT", "ATC",
				"ATA", "ATG", "ACT", "ACC", "ACA", "ACG", "AAT", "AAC", "AAA",
				"AAG", "AGT", "AGC", "AGA", "AGG", "GTT", "GTC", "GTA", "GTG",
				"GCT", "GCC", "GCA", "GCG", "GAT", "GAC", "GAA", "GAG", "GGT",
				"GGC", "GGA", "GGG" };

		String dnaSequence;
		String protSequence = new String();
		dnaSequence = nonSeq.matcher(inputSequence.getText()).replaceAll("")
				.toUpperCase();
		int n = dnaSequence.length()-2;
		int m = codons.length;
		DNA: for (int i = 0; i < n; i = i + 3) {
			for (int j = 0; j < m; j++) {
				if (dnaSequence.substring(i, i + 3).matches(codons[j])) {
					if (residues[j].matches("-")) {
						break DNA;
					}
					protSequence = protSequence.concat(residues[j]);
					break;
				}
			}
		}

		int l = (int) Math.floor(protSequence.length() / 10);
		String resNos = new String();
		for (int i = 0; i < l; i++) {
			StringBuffer tag = new StringBuffer(String.valueOf((i + 1) * 10));
			int spaces = 9 - tag.length();
			for (int k = 0; k < spaces; k++) {
				tag.insert(0, ' ');
			}
			tag.append('|');
			resNos += tag.toString();
		}
		secondaryStruct.setText(protSequence + " \n" + resNos);
		secondaryStructLabel.setText("Sequence:\n");
		inputSequence.setText(dnaSequence);

		return protSequence;
	}

	
	// Does what it says on the tin


	public BufferedReader openURL(String urlName, String[] paramNames,
			String[] paramValues) {

		BufferedReader br = null;
		try {
			URL url = new URL(urlName);
			URLConnection con = url.openConnection();
			con.setDoInput(true);
			if (paramNames!=null)
				con.setDoOutput(true);
			con.setUseCaches(false);
			String msg = null;
			if (paramNames != null && paramNames.length > 0) {
				msg = paramNames[0] + "="
						+ URLEncoder.encode(paramValues[0], "UTF-8");
				for (int i = 1; i < paramNames.length; i++) {
					msg += "&" + paramNames[i] + "="
							+ URLEncoder.encode(paramValues[i], "UTF-8");
				}
			}
			if (msg != null) {
				con.setRequestProperty("CONTENT_LENGTH", "" + msg.length());
				OutputStream os = con.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os);
				osw.write(msg);
				osw.flush();
				osw.close();
			}
			InputStream is = con.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return br;
	}

	
	private JMenu getOverhangMenu() {
		JMenu menu = new JMenu("Overhangs");
		
		JMenuItem item;
		
		BufferedReader urlReader = null;
		String overhangsParam = getParameter("OVERHANGS");
		if (overhangsParam != null && !overhangsParam.equals("")) {
			if (overhangsParam.startsWith("./")) {
				
				overhangsParam=overhangsParam.replaceFirst(".",getCodeBase().toString());
			}
			urlReader=openURL(overhangsParam,null,null);
		}
		if (urlReader == null) {
			urlReader =  openURL(getCodeBase() + "/overhangs.txt",null,null);
		}
		if (urlReader == null) {
			urlReader=openURL("http://xtal.nki.nl/ccd/overhangs.txt",null,null);
		}
		
		if (urlReader == null) {
			return menu;
		}
		
		String line;
		try {
			while ((line = urlReader.readLine()) != null) {
								
				String[] words = line.split(";");
				if (words.length==3) {
					item = new JMenuItem(new OverhangSelectAction(words[0].trim(),words[1].trim(),words[2].trim()));
					menu.add(item);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return menu;			
	}

	
	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	// mouseclicked event control the marking of start on stop positions for
	// oligos
	public void mouseClicked(MouseEvent e) {
		if (markStart.isSelected()) {
			markStart(secondaryStruct);
		}
		if (markStop.isSelected()) {
			markStop(secondaryStruct);
		}
	}
	
	// Mark an oligo starting point
	public void markStart(JTextComponent textComp) {
		if (proteinSequence== null)
			return;
		
		try {
			Highlighter hilite = textComp.getHighlighter();
			int pos = textComp.getCaretPosition();
			if (pos <= proteinSequence.length()) {
				hilite.addHighlight(pos, pos + 1, startMarker);
			}
		} catch (BadLocationException e) {
		}
	}

	// Mark an oligo stop point
	public void markStop(JTextComponent textComp) {
		if (proteinSequence== null)
			return;
		try {
			Highlighter hilite = textComp.getHighlighter();
			int pos = textComp.getCaretPosition();
			if (pos <= proteinSequence.length()) {
				hilite.addHighlight(pos - 1, pos, stopMarker);
			}
		} catch (BadLocationException e) {
		}
	}

	
	
	// An instance of the private subclass of the default highlight painter
	Highlighter.HighlightPainter startMarker = new MyMarker(Color.green, 1);
	Highlighter.HighlightPainter stopMarker = new MyMarker(Color.red, 0);
	Highlighter.HighlightPainter highLighter = new MyHighlightPainter(
			Color.orange);

	// A private subclass of the default highlight painter
	// Uses a type flag to indicate start of stop marks
	class MyMarker extends DefaultHighlighter.DefaultHighlightPainter {
		private int type;

		public MyMarker(Color color, int i) {
			super(color);
			type = i;
		}

		public boolean isStartMarker() {
			if (type == 1) {
				return true;
			}
			return false;
		}

		public boolean isStopMarker() {
			if (type == 0) {
				return true;
			}
			return false;
		}

	}

	// A private subclass of the default highlight painter
	// Used to highlight scores in the secondary structure textComponent
	class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

		public MyHighlightPainter(Color color) {
			super(color);
		}

	}

	public void highlightScores(JTextComponent textComp, int start, int stop) {

		try {
			Highlighter hilite = textComp.getHighlighter();
			hilite.addHighlight(start, stop, highLighter);
		} catch (BadLocationException e) {
		}
	}




	// Create the MenuBar with the of servers to call
	protected JMenuBar createMenuBar() {
		JMenuItem text;
		JCheckBoxMenuItem option;
		JMenuBar menubar = new JMenuBar();
		optionNames = new String[] { "Secondary structure", "SOPMA", "HNN",
				"MLRC", "DPM", "DSC", "GOR-IV", "PHD", "PREDATOR", "SIMPA96",
				"", "Disorder", "RONN2", "IUPred", "DisEmbl", "GlobPlot","", 
				"Others features", "Lupas (Coiled-coil)","Phobius (TM+SP)",
				"", "Domains", "SMART", "GlobPlot","DLP-SVM (Linkers)"};
		//boolean[] optionDefaults =
		//{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,true};
		
		/*boolean[] optionDefaults = { false, false, true, false, false, false,
		true, true, false, false, false, false, true, true, false,
		false, true, false, false, false, true, true };
		*/
		String defaultSettings="0010001100001111001000111";
		if (userPrefs != null)
			optionSettings = userPrefs.get("optionSettings",defaultSettings).toCharArray();
		else
			optionSettings = defaultSettings.toCharArray();
		
		if (optionSettings.length!=25)
			optionSettings=defaultSettings.toCharArray();
	
			
		menubar.add(optionMenu);
		text = new JMenuItem(optionNames[0]);
		optionMenu.add(text);
		int i;
		for (i = 1; i < 10; i++) {
			option = new JCheckBoxMenuItem(optionNames[i]);
			option.setSelected(optionSettings[i]=='1');
			optionMenu.add(option);
		}
		optionMenu.addSeparator();
		i++;
		text = new JMenuItem(optionNames[i]);
		optionMenu.add(text);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		optionMenu.addSeparator();
		i++;
		text = new JMenuItem(optionNames[i]);
		optionMenu.add(text);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		optionMenu.addSeparator();
		i++;
		text = new JMenuItem(optionNames[i]);
		optionMenu.add(text);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		option = new JCheckBoxMenuItem(optionNames[i]);
		option.setSelected(optionSettings[i]=='1');
		optionMenu.add(option);
		i++;
		
		JMenu overhangSelect = getOverhangMenu();		
		menubar.add(overhangSelect);
		
		JMenu helpMenu = new JMenu("Help");
		menubar.add(helpMenu);
		text = new JMenuItem(new HtmlWindowAction("Help","http://xtal.nki.nl/ccd/help.html"));
		helpMenu.add(text).setText("Help");
		text = new JMenuItem(new HtmlWindowAction("FAQ","http://xtal.nki.nl/ccd/faq.html"));
		helpMenu.add(text).setText("FAQ");
		text = new JMenuItem(new HtmlWindowAction("Example Input","http://xtal.nki.nl/ccd/asequence.html"));
		helpMenu.add(text).setText("Example Input");
		text = new JMenuItem(new AboutAction());
		helpMenu.add(text).setText("About");
		
		return menubar;
	}

	// Highlight a region of text depending on a value
	private void markText(JTextComponent text, int start, int stop, int value) {

		int highlightStart = -1;
		try {
			String line = text.getText(start, stop - start);
			int len = line.length();
			for (int i = 0; i < len; i++) {
				char c = line.charAt(i);
				if (Character.isDigit(c)) {
					int score = Character.getNumericValue(c);
					if (highlightStart == -1) {
						if (score >= value) {
							highlightStart = i;
						}
					} else {
						if (score < value) {
							highlightScores(text, start + highlightStart, start
									+ i);
							highlightStart = -1;
						}
					}
				}
			}

			if (highlightStart != -1) {
				highlightScores(text, start + highlightStart, start + len);
			}

		} catch (BadLocationException e) {

		}
	}
	
	// Highlight a region of text depending on a value
	private void markText(JTextComponent text, int start, int stop, char value) {

		int highlightStart = -1;
		try {
			String line = text.getText(start, stop - start);
			int len = line.length();
			for (int i = 0; i < len; i++) {

				if (highlightStart == -1) {
					if (line.charAt(i)==value) {
						highlightStart = i;
					}
				} else {
					if (line.charAt(i)!=value) {
						highlightScores(text, start + highlightStart, start
								+ i);
						highlightStart = -1;
					}
				}
			}

			if (highlightStart != -1) {
				highlightScores(text, start + highlightStart, start + len);
			}

		} catch (BadLocationException e) {

		}
	}



	// Get output from the Smart server. This is slow, so do it on a separate
	// thread
	class SmartOutput extends Thread {

		BufferedReader urlReader;
		String url = ("http://smart.embl-heidelberg.de/smart/job_status.pl");

		String[] paramNames = new String[] { "jobid" };
		String[] paramValues = new String[] { "" };

		Pattern flag = Pattern.compile("SMART RESULT TEXTFORMAT");

		public SmartOutput(String jobId) {
			paramValues[0] = jobId;
		}

		public void run() {
			boolean done = false;
			String line;

			urlReader = openURL(url, paramNames, paramValues);
			while (!done) {
				try {
					READ: while ((line = urlReader.readLine()) != null) {
						if (flag.matcher(line).find()) {
							done = true;
							break READ;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}

				if (!done) {
					try {
						if (restDone) {
							waitPopupText.append(".");
						}
						sleep(5000);
					} catch (Throwable t) {
						t.printStackTrace();
					}
					urlReader = openURL(url, paramNames, paramValues);
				}
			}

			// Done waiting for the server. We have the output
			String domain = null;
			String type = null;
			String status = null;
			int start = 0;
			int end = 0;
			done = false;

			StringBuffer smartLine = new StringBuffer(proteinSequence);
			int l = proteinSequence.length();
			for (int k = 0; k < l; k++) {
				smartLine.setCharAt(k, '-');
			}

			StringBuffer lowComplexLine = new StringBuffer(proteinSequence);
			l = proteinSequence.length();
			for (int k = 0; k < l; k++) {
				lowComplexLine.setCharAt(k, '-');
			}

			char label = ' ';
			boolean hasLowComplexity = false;

			try {

				while ((line = urlReader.readLine()) != null) {
					if (line.startsWith("DOMAIN")) {
						done = false;
						String[] words = line.split("=");
						domain = words[1];
					} else if (line.startsWith("START")) {
						String[] words = line.split("=");
						start = Integer.valueOf(words[1]).intValue();
					} else if (line.startsWith("END")) {
						String[] words = line.split("=");
						end = Integer.valueOf(words[1]).intValue();
					} else if (line.startsWith("TYPE")) {
						String[] words = line.split("=");
						type = words[1];
					} else if (line.startsWith("STATUS")) {
						String[] words = line.split("=");
						String[] words2 = words[1].split("\\|");
						status = words2[0];
						done = true;
					}
					if (done) {
						label = ' ';
						System.out.println("Type " + type);
						System.out.println("Status " + status);
						System.out.println("Domain " + domain);
						System.out.println("Start " + start);
						System.out.println("End" + end);

						if (status.startsWith("visible")) {
							if (type.startsWith("SMART")) {
								label = 'x';
								domain = "Smart:" + domain;
							} else if (type.startsWith("PFAM")) {
								label = 'x';
							} else if (type.startsWith("PDB")) {
								label = 'x';
							} else if (type.startsWith("BLAST")) {
								label = 'x';
							} else if (type.startsWith("INTRINSIC")) {
								// Do not show the disorder features. We have it under disorder 
								if (domain.startsWith("DisEMBL")) {
//									label = 'd';
									label = '-';
								} else if (domain.startsWith("low_complexity")) {
									label = '-';
									// Do not show the low complexity features. We show it in a seperate line
									if (domain.startsWith("low_complexity")) {
										hasLowComplexity = true;
										for (int k = start - 1; k < end; k++) {
											lowComplexLine.setCharAt(k, 'l');
										}
									}
								} else {
//									label = '?';
									label = '-';
								}
							}
							int c = 0;
							domain = domain + "-";
							int len = domain.length();
							for (int k = start - 1; k < end; k++) {
								if (label == 'x') {
									smartLine.setCharAt(k, domain.charAt(c
											% len));
								} else {
									smartLine.setCharAt(k, label);
								}
								c++;
							}
						}
						done = false;
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}

			// If by magic we finished before the rest, we need to wait, as we
			// close the popup from this thread.
			// And we don't want to end up in between e.g. secondary structure
			// predictions. So wait before adding the output line
			while (!restDone) {
				try {
					sleep(1000);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			secondaryStructLabel.setText(secondaryStructLabel.getText() + "\nDomains  "
					+ optionNames[20]);
			secondaryStruct.setText(secondaryStruct.getText() + "\n"
					+ smartLine);
			if (hasLowComplexity) {
				secondaryStructLabel.setText(secondaryStructLabel.getText() + "\nLow complexity");
				secondaryStruct.setText(secondaryStruct.getText() + "\n"
						+ lowComplexLine);
			}


			// Mark the scores that are over a threshold for the disorder
			// Highlighter is reset if we setText, so we need to do it here
			if (optionMenu.getItem(12).isSelected()) {
				markText(secondaryStruct, startRonn, stopRonn, 5);
			}
			if (optionMenu.getItem(13).isSelected()) {
				markText(secondaryStruct, startIupred, stopIupred, 5);
			}
			if (optionMenu.getItem(14).isSelected()) {
				markText(secondaryStruct, startDisembl, stopDisembl, 'd');
			}
			if (optionMenu.getItem(15).isSelected()) {
				markText(secondaryStruct, startGlobPlot, stopGlobPlot, 'd');
			}

			smartOutputButton.setEnabled(true);
			waitPopup.dispose();
		}
	}

	


	// ********** ACTION INNER CLASSES ********** //

	// Generate the oligos based on the markers that are set
	class GetOligosAction extends AbstractAction {

		public GetOligosAction() {
			super("Get");
		}

		public void actionPerformed(ActionEvent ev) {
			int starts[] = new int[50];
			int stops[] = new int[50];
			boolean start_error[] = new boolean[50];
			boolean stop_error[] = new boolean[50];

			int nStarts = 0;
			int nStops = 0;
			Highlighter hilite = secondaryStruct.getHighlighter();
			Highlighter.Highlight[] hilites = hilite.getHighlights();
			OligoBuilder oligoBuilder = new OligoBuilder();
			String pre = oligoName.getText();
			for (int i = 0; i < hilites.length; i++) {
				if (hilites[i].getPainter() instanceof MyMarker) {
					MyMarker p = (MyMarker) hilites[i].getPainter();
					if (p.isStartMarker()) {
						int k = hilites[i].getStartOffset();
						starts[nStarts] = k;
						nStarts++;
					} else if (p.isStopMarker()) {
						int k = hilites[i].getEndOffset();
						stops[nStops] = k;
						nStops++;
					}
				}
			}
			if (nStarts==0 && nStops==0)
				return;
			
			// Now sort the starts and stops
			boolean cont = true;
			while (cont) {
				cont = false;
				for (int i = 0; i < nStarts - 1; i++) {
					if (starts[i] > starts[i + 1]) {
						int tmp = starts[i];
						starts[i] = starts[i + 1];
						starts[i + 1] = tmp;
						cont = true;
					}
				}
			}

			DefaultTableModel tableModel = (DefaultTableModel) oligos
					.getModel();

			int maxLength = 0;
			String maxString = new String("");
			int previous = -1;
			for (int i = 0; i < nStarts; i++) {
				int k = starts[i];
				if (k==previous) 
					continue;
				String oligo = oligoBuilder.getOligo(k * 3, false);
				String name = pre.concat(String.valueOf(k + 1)).concat("FW");
				tableModel.addRow(new Object[] { name, oligo });
				int l = oligo.length();
				if (l > maxLength) {
					maxLength = l;
					maxString = oligo;
				}

				// oligos.setValueAt(name,i,0);
				// oligos.setValueAt(oligo,i,1);

				if (oligo.startsWith("Error")) {
					start_error[i] = true;
				} else {
					start_error[i] = false;
				}
				previous=k;
			}

			cont = true;
			while (cont) {
				cont = false;
				for (int i = 0; i < nStops - 1; i++) {
					if (stops[i] > stops[i + 1]) {
						int tmp = stops[i];
						stops[i] = stops[i + 1];
						stops[i + 1] = tmp;
						cont = true;
					}
				}
			}
			previous = -1;
			for (int i = 0; i < nStops; i++) {
				int k = stops[i];
				if (k==previous) 
					continue;
				String oligo = oligoBuilder.getOligo(k * 3 - 1, true);
				String name = pre.concat(String.valueOf(k)).concat("RV");
				tableModel.addRow(new Object[] { name, oligo });
				// oligos.setValueAt(name,nStarts+i,0);
				// oligos.setValueAt(oligo,nStarts+i,1);

				int l = oligo.length();
				if (l > maxLength) {
					maxLength = l;
					maxString = oligo;
				}

				if (oligo.startsWith("Error")) {
					stop_error[i] = true;
				} else {
					stop_error[i] = false;
				}
				previous=k;
			}

			FontMetrics fm = getFontMetrics(font);
			int width = fm.stringWidth(maxString);
			oligos.getColumnModel().getColumn(1).setPreferredWidth(width + 10);

			tableModel = (DefaultTableModel) proteins.getModel();

			maxLength = 0;
			if (aligned.isSelected()) {
				maxLength = proteinSequence.length();
				maxString = proteinSequence;
			}
			// Now get the protein sequences
			for (int i = 0; i < nStarts; i++) {
				if (start_error[i])
					continue;

				String startName = new String(String.valueOf(starts[i] + 1))
						.concat("-");
				for (int j = 0; j < nStops; j++) {
					if (stop_error[j])
						continue;

					if (stops[j] > starts[i]) {
						String name = pre.concat(startName).concat(
								String.valueOf(stops[j]));
						if (aligned.isSelected()) {
							StringBuffer seq = new StringBuffer(proteinSequence);
							for (int p = 0; p < starts[i]; p++) {
								seq.setCharAt(p, ' ');
							}
							int length = proteinSequence.length();
							for (int p = stops[j]; p < length; p++) {
								seq.setCharAt(p, ' ');
							}
							tableModel.addRow(new Object[] { name, seq });
						} else {
							String seq = proteinSequence.substring(starts[i],
									stops[j]);
							tableModel.addRow(new Object[] { name, seq });
							int l = stops[j] - starts[i] + 1;
							if (l > maxLength) {
								maxLength = l;
								maxString = seq;
							}

						}
					}
				}
			}

			width = fm.stringWidth(maxString);
			proteins.getColumnModel().getColumn(1)
					.setPreferredWidth(width + 10);
		}
	}

	// get an individual oligo from a given starting position
	class OligoBuilder {

		char[] dnaSequence;
		boolean reverse;
		double targetTemp;
		int length;

		public OligoBuilder() {
			dnaSequence = inputSequence.getText().toCharArray();
			Double d = (Double) temperature.getValue();
			targetTemp = d.doubleValue();

			Integer l = (Integer) numberOfBases.getValue();
			length = l.intValue();

		}

		public String getOligo(int start, boolean reverse) {

			double currentTemp = 0.0;
			int nCGs = 0;
			int m = 0;
			double previousTemp = 0.0;
			String oligoString = new String();
			char[] oligo = new char[200];
			char[] complement = new char[200];

			boolean failure = false;
			int previousLength = 0;
			int currentLength = 0;
			double minDiff = 99999.99;
			double minDiffTemp = 0.0;
			int minDiffLength = 0;

			if (useTemp.isSelected()) {
				length = 0;
				TEMP: while (currentTemp < targetTemp) {

					if (reverse) {
						m = start - length;
					} else {
						m = start + length;
					}

					if (m >= dnaSequence.length || m < 0) {
						failure = true;
						break TEMP;
					}
					oligo[length] = dnaSequence[m];
					if (dnaSequence[m] == 'C' || dnaSequence[m] == 'G'
							|| dnaSequence[m] == 'c' || dnaSequence[m] == 'g') {
						nCGs++;
					}
					length++;

					double temperature = 64.9 + 41.0 * (nCGs - 16.4) / length;

					if (dnaSequence[m] == 'C' || dnaSequence[m] == 'G') {
						previousTemp = currentTemp;
						previousLength = currentLength;

						currentTemp = temperature;
						currentLength = length;
					}

					double diff = Math.abs(temperature - targetTemp);
					if (diff < minDiff) {
						minDiff = diff;
						minDiffLength = length;
						minDiffTemp = temperature;
					}

				}

				if ((targetTemp - previousTemp) < (currentTemp - targetTemp)) {
					length = previousLength;
					currentTemp = previousTemp;
				}

				
				if (Math.abs(targetTemp - currentTemp) > 3.0) {
					System.out
							.println("Ending of G or C deviates > 3.0 in Tm. Switch to closest temperature "
									+ minDiffTemp);
					length = minDiffLength;
					currentTemp = minDiffTemp;
				}
			} else {
				LOOP: for (int i = 0; i < length; i++) {
					if (reverse) {
						m = start - i;
					} else {
						m = start + i;
					}

					if (m >= dnaSequence.length || m < 0) {
						failure = true;
						break LOOP;
					}
					oligo[i] = dnaSequence[m];
				}
			}
			if (failure) {
				System.err
						.println("Did not reach target temperature or number of bases before reaching end of sequence");
				return ("Error (temperature/number of bases not reached)");
			}

			if (reverse) {
				for (int l = 0; l < length; l++) {
					if (oligo[l] == 'c' || oligo[l] == 'C') {
						complement[l] = 'G';
					} else if (oligo[l] == 'g' || oligo[l] == 'G') {
						complement[l] = 'C';
					} else if (oligo[l] == 'a' || oligo[l] == 'A') {
						complement[l] = 'T';
					} else if (oligo[l] == 't' || oligo[l] == 'T') {
						complement[l] = 'A';
					}
				}
				oligo = complement;
			}
			for (int l = 0; l < length; l++) {
				oligoString = oligoString.concat(String.valueOf(oligo[l]));
			}
			if (reverse) {
				return reverseOverhang.getText().concat(oligoString);
			} else {
				return forwardOverhang.getText().concat(oligoString);
			}

		}
	}

	
	
	
	// An exit action
	public class ExitAction extends AbstractAction {
		public ExitAction() {
			super("Exit");
		}

		public void actionPerformed(ActionEvent ev) {
			System.exit(0);
		}
	}
	
	public class AboutAction extends AbstractAction {
		public AboutAction() {
			super();
		}

		public void actionPerformed(ActionEvent ev) {
			
			JOptionPane.showMessageDialog(null,
				    "Crystallisation Construct Designer\nCopyright (C) 2008 The Netherlands Cancer Institute", 
				    "About CCD",JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public class HtmlWindowAction extends AbstractAction {
		
		String windowName;
		String urlName;
		
		public HtmlWindowAction(String windowName,String urlName) {
			super();
			this.windowName=windowName;			
			this.urlName=urlName;
		}

		public void actionPerformed(ActionEvent ev)  {
			JEditorPane helpPane=createHelpPane();
			JScrollPane helpScrollPane = new JScrollPane(helpPane);
	        helpScrollPane.setVerticalScrollBarPolicy(
	                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	        helpScrollPane.setPreferredSize(new Dimension(500, 550));
	        JPanel helpPanel=new JPanel(new BorderLayout());
	        helpPanel.add(helpScrollPane,BorderLayout.CENTER);

			JFrame frame = new JFrame(windowName);        
	        frame.getContentPane().add(helpPanel);
	        frame.pack();
	        frame.setVisible(true);
	        
		}

		private JEditorPane createHelpPane() {
			JEditorPane editorPane = new JEditorPane();
			editorPane.setEditable(false);
			try {
				URL helpURL = new URL(urlName);
				if (helpURL != null) {	
					try {
						editorPane.setPage(helpURL);
					} catch (IOException e) {
						System.err.println("Attempted to read a bad URL: " + helpURL);
					}	
				} else {
					System.err.println("Couldn't find file: help.html");
				}	
			} catch (MalformedURLException e) {
				System.out.println(e.getMessage());
			}
		   	editorPane.addHyperlinkListener(new LinkOutListener());
		   	return editorPane;
		}	
	}

 
    
    class LinkOutListener implements HyperlinkListener {
        	
    	public void hyperlinkUpdate(HyperlinkEvent hev) {
        	try {
        		if (hev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) { 
    				AppletContext a = getAppletContext();
    				a.showDocument(hev.getURL(),"_blank");
        		}			
        	}catch (Exception e) {
        		e.printStackTrace();
        	}	
    	}
    }
    
	// An action that opens an existing file
	class OpenAction extends AbstractAction {
		JTextComponent textComponent;

		public OpenAction(JTextComponent textComponent) {
			super("Open");
			this.textComponent = textComponent;
		}

		// Query user for a filename and attempt to open and read the file into
		// the text component.
		public void actionPerformed(ActionEvent ev) {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(CrystConDesignerApplet.this) != JFileChooser.APPROVE_OPTION)
				return;
			File file = chooser.getSelectedFile();
			if (file == null)
				return;

			FileReader reader = null;
			try {
				reader = new FileReader(file);
				textComponent.read(reader, null);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(CrystConDesignerApplet.this,
						"File Not Found", "ERROR", JOptionPane.ERROR_MESSAGE);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException x) {
					}
				}
			}
		}
	}

	// An action that saves the document to a file
	// Calls different methods depending on the instance

	class SaveAction extends AbstractAction {

		JTextComponent textComponent;
		JTextComponent labelComponent;
		JTable table;

		public SaveAction(JTextComponent textComponent) {
			super("Save");
			this.textComponent = textComponent;
			this.table = null;
			this.labelComponent = null;
		}

		public SaveAction(JTextComponent labelComponent,
				JTextComponent textComponent) {
			super("Save");
			this.textComponent = textComponent;
			this.labelComponent = labelComponent;
			this.table = null;
		}

		public SaveAction(JTable table) {
			super("Save");
			this.table = table;
			this.textComponent = null;
			this.labelComponent = null;
		}

		// Query user for a filename and attempt to open and write the text
		// component's content to the file.
		public void actionPerformed(ActionEvent ev) {
			if (textComponent != null) {
				if (labelComponent != null) {
					saveLabelAndTextComponent();
				} else {
					saveTextComponent();
				}
			}
			if (table != null) {
				saveTable();
			}
		}

		private void saveTextComponent() {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showSaveDialog(CrystConDesignerApplet.this) != JFileChooser.APPROVE_OPTION)
				return;
			File file = chooser.getSelectedFile();
			if (file == null)
				return;

			FileWriter writer = null;
			try {
				writer = new FileWriter(file);
				textComponent.write(writer);

			} catch (IOException ex) {
				JOptionPane.showMessageDialog(CrystConDesignerApplet.this,
						"File Not Saved", "ERROR", JOptionPane.ERROR_MESSAGE);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException x) {
					}
				}
			}
		}

		private void saveLabelAndTextComponent() {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showSaveDialog(CrystConDesignerApplet.this) != JFileChooser.APPROVE_OPTION)
				return;
			File file = chooser.getSelectedFile();
			if (file == null)
				return;

			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file));
				String[] labels = labelComponent.getText().split("\n");
				String[] texts = textComponent.getText().split("\n");
				int len = labels.length;
				for (int i = 0; i < len; i++) {
					StringBuffer la = new StringBuffer(labels[i]);
					int spaces = 20 - la.length();
					for (int k = 0; k < spaces; k++) {
						la.append(' ');
					}
					la.append(' ');
					writer.write(la.toString());
					writer.write(texts[i]);
					writer.newLine();
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(CrystConDesignerApplet.this,
						"File Not Saved", "ERROR", JOptionPane.ERROR_MESSAGE);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException x) {
					}
				}
			}
		}

		private void saveTable() {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showSaveDialog(CrystConDesignerApplet.this) != JFileChooser.APPROVE_OPTION)
				return;
			File file = chooser.getSelectedFile();
			if (file == null)
				return;

			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file));
				DefaultTableModel tableModel = (DefaultTableModel) table
						.getModel();

				int len = tableModel.getRowCount();
				for (int i = 0; i < len; i++) {
					writer.write((String) tableModel.getValueAt(i, 0));
					writer.write(",");
					writer.write((String) tableModel.getValueAt(i, 1));
					writer.newLine();
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(CrystConDesignerApplet.this,
						"File Not Saved", "ERROR", JOptionPane.ERROR_MESSAGE);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException x) {
					}
				}
			}
		}

	}
	
	
	// Get the last used sequence from the user preferences
	class GetLastSequenceAction extends AbstractAction {

		public GetLastSequenceAction() {
			super("Last");
		}

		public void actionPerformed(ActionEvent ev) {
			if (userPrefs != null)
				inputSequence.setText(userPrefs.get("lastSequence",""));
			else
				inputSequence.setText("");
		}
	}



	// An clear action for tables or textComponents
	class ClearAction extends AbstractAction {

		JTextComponent textComponent;
		JTable table;

		public ClearAction(JTextComponent textComponent) {
			super("Clear");
			this.textComponent = textComponent;
		}

		public ClearAction(JTable table) {
			super("Clear");
			this.table = table;
		}

		public void actionPerformed(ActionEvent ev) {
			if (textComponent != null)
				textComponent.setText("");
			if (table != null) {
				DefaultTableModel tableModel = (DefaultTableModel) table
						.getModel();

				for (int i = tableModel.getRowCount() - 1; i > -1; i--) {
					tableModel.removeRow(i);
				}
			}
		}
	}

	// Post a request to an url and retrieves the respons.
	class PostAction extends AbstractAction {

		public PostAction() {
			super("Submit");
		}

		public void actionPerformed(ActionEvent ev) {
			try {
				removeStartMarks(secondaryStruct);
				removeStopMarks(secondaryStruct);
				waitPopup = new JDialog();
				waitPopupText = new JTextArea();
				JScrollPane scrollpane = new JScrollPane(waitPopupText);
				waitPopupText.setLineWrap(false);
				waitPopupText.setEditable(false);
				waitPopup.getContentPane().add(scrollpane);
				waitPopup.setSize(350, 300);
				waitPopup.setTitle("CCD status");
				waitPopup.setLocationRelativeTo(content);
				waitPopup.setVisible(true);

				GetPredictions getPredictions = new GetPredictions();

				getPredictions.start();
			} catch (Throwable t) {
				t.printStackTrace();
			}

		}
	}


	// Clear the oligo starts
	class ClearStartAction extends AbstractAction {

		public ClearStartAction() {
			super("Clear start(s)");
		}

		public void actionPerformed(ActionEvent ev) {

			removeStartMarks(secondaryStruct);
		}
		

	}


	// Clear the oligo stops
	class ClearStopAction extends AbstractAction {

		public ClearStopAction() {
			super ("Clear stop(s)");
		}

		public void actionPerformed(ActionEvent ev) {

			removeStopMarks(secondaryStruct);
			
		}
		


	}
	
	// Clear the oligo starts
	class UndoMarkAction extends AbstractAction {

		public UndoMarkAction() {
			super("Undo");
		}

		public void actionPerformed(ActionEvent ev) {

			undoMark(secondaryStruct);
		}
		

	}


	// Removes only start highlights
	public void removeStartMarks(JTextComponent textComp) {
		Highlighter hilite = textComp.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();

		for (int i = 0; i < hilites.length; i++) {
			if (hilites[i].getPainter() instanceof MyMarker) {
				MyMarker p = (MyMarker) hilites[i].getPainter();
				if (p.isStartMarker()) {
					hilite.removeHighlight(hilites[i]);
				}
			}
		}
	}
	
	// Remove stop highLights
	public void removeStopMarks(JTextComponent textComp) {
		Highlighter hilite = textComp.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();

		for (int i = 0; i < hilites.length; i++) {
			if (hilites[i].getPainter() instanceof MyMarker) {
				MyMarker p = (MyMarker) hilites[i].getPainter();
				if (p.isStopMarker()) {
					hilite.removeHighlight(hilites[i]);
				}
			}
		}
	}
	
	// Undo last highLight
	public void undoMark(JTextComponent textComp) {
		Highlighter hilite = textComp.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();
		for (int i = hilites.length-1; i >= 0; i--) {
			if (hilites[i].getPainter() instanceof MyMarker) {
				hilite.removeHighlight(hilites[i]);
				return;
			}
		}
	}
	

	// Open an new page with the HTML-formatted SMART server output
	class SmartOutputAction extends AbstractAction {

		public SmartOutputAction() {
			super("Smart Output");	
		}

		public void actionPerformed(ActionEvent ev) {

			try {
				String link = ("http://smart.embl-heidelberg.de/smart/job_status.pl?jobid=" + smartGraphicsJobId);

				AppletContext a = getAppletContext();
				URL url = new URL(link);
				a.showDocument(url, "_blank");
			} catch (MalformedURLException e) {
				System.out.println(e.getMessage());
			}
		}
	}


	// Switch between temperature and fixed number methods
	class SwitchTNAction extends AbstractAction {

		public SwitchTNAction() {
		}

		public void actionPerformed(ActionEvent ev) {
			if (useTemp.isSelected()) {
				temperature.setEnabled(true);
				numberOfBases.setEnabled(false);
			} else {
				temperature.setEnabled(false);
				numberOfBases.setEnabled(true);
			}
		}
	}
	
	public class OverhangSelectAction extends AbstractAction {
		
		String forward,reverse;
		public OverhangSelectAction(String name, String fwd, String rvs) {
			super(name);
			forward=fwd;
			reverse=rvs; 
		}

		public void actionPerformed(ActionEvent ev)  {
			forwardOverhang.setText(forward);
			reverseOverhang.setText(reverse);	        
		}
	}
	

	// Not used
	class TextRenderer extends JTextArea implements TableCellRenderer,
			MouseListener {

		public TextRenderer() {
			setLineWrap(false);
			setEditable(true);
			addMouseListener(this);
		}

		public Component getTableCellRendererComponent(JTable table,
				Object obj, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setText((String) obj);

			return this;
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
			if (markStart.isSelected()) {
				markStart(this);
			}
			if (markStop.isSelected()) {
				markStop(this);
			}
		}

	}

	/**
	 * Synchronize the data models of any two JComponents that use a
	 * BoundedRangeModel ( such as a JScrollBar, JSlider or ProgressBar).
	 * 
	 * @see javax.swing.BoundedRangeModel
	 * @see javax.swing.event.ChangeListener
	 * @Author R. Kevin Cole
	 */
	class MyChangeListener implements ChangeListener {
		private BoundedRangeModel myModel;

		/**
		 * @param model
		 *            This model is forced to move in synchronization to this
		 *            ChangeListener's event-source.
		 */
		public MyChangeListener(BoundedRangeModel model) {
			myModel = model;
		}

		// - begin implementation of ChangeListener
		//
		/**
		 * Envoked when the target of the listener has changed its state.
		 */
		public void stateChanged(ChangeEvent e) {
			BoundedRangeModel sourceModel = (BoundedRangeModel) e.getSource();

			int sourceDiff = sourceModel.getMaximum()
					- sourceModel.getMinimum();
			int destDiff = myModel.getMaximum() - myModel.getMinimum();
			int destValue = sourceModel.getValue();

			if (sourceDiff != destDiff)
				destValue = (destDiff * sourceModel.getValue()) / sourceDiff;

			myModel.setValue(myModel.getMinimum() + destValue);
		}
		//
		// - end implementation of ChangeListener
	}
	
}
