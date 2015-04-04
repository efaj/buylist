import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Properties;

public class MainFrame{
	public static final String COST_STRING="Total Cost=$";
	public static final String CALC_COST="Recalculate Costs";
	public static final Dimension SIZE_DEFAULT=new Dimension(400, 300);

	private String catMark="*";
	private String multiMark="x";
	private DefaultMutableTreeNode rootCategory=
			new DefaultMutableTreeNode(new Category("Root", 0) );
	private Properties props = new Properties();
	public static final String PROP_NAME="buylist.cfg";

	// Initialize all swing objects.
	private JFrame frame = new JFrame("BuyList"); //create Frame
	private JPanel pnlSouth = new JPanel(); // South quadrant

	private JLabel totalCost=new JLabel(COST_STRING);
	private JButton calcCost=new JButton(CALC_COST);
	private JScrollPane editorScroll;
	private JEditorPane editor=new JEditorPane();
	private JScrollPane treeScroll;
	private JTree catTree=new JTree(rootCategory);
	private JSplitPane split;
	// Menu
	private JMenuBar mb = new JMenuBar(); // Menubar
	private JMenu menuFile = new JMenu("File"); // File Entry on Menu bar
	private JMenuItem mbOpen = new JMenuItem("Open"); // Quit sub item
	private JMenuItem mbSave = new JMenuItem("Save"); // Quit sub item
	private JMenuItem mbQuit = new JMenuItem("Quit"); // Quit sub item

	/** Constructor for the GUI */
	public MainFrame(){
		try {
			FileInputStream in = new FileInputStream(PROP_NAME);
			props.load(in);
			in.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		rootCategory.add(new DefaultMutableTreeNode(
				new Category("No Category", 0)) );
		// Set menubar
		frame.setJMenuBar(mb);

		//Build Menus
		mbOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				InputEvent.CTRL_MASK));
		menuFile.add(mbOpen);
		mbSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				InputEvent.CTRL_MASK));
		menuFile.add(mbSave);
		menuFile.add(mbQuit);
		mb.add(menuFile);

		calcCost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parseEditor();
			}});
		pnlSouth.add(calcCost, BorderLayout.WEST);
		pnlSouth.add(totalCost,BorderLayout.EAST);
		editor.setEditable(false);
		editorScroll=new JScrollPane(editor);
		Dimension pSize=new Dimension();
		pSize.width=Integer.parseInt(props.getProperty("PreferredWidth",
				""+SIZE_DEFAULT.width));
		pSize.height=Integer.parseInt(props.getProperty("PreferredHeight",
				""+SIZE_DEFAULT.height));
		frame.setPreferredSize(pSize);
		catTree.setRootVisible(true);
		catTree.expandRow(0);
		catTree.setRootVisible(false);
		treeScroll=new JScrollPane(catTree);
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				editorScroll, treeScroll);
		split.setOneTouchExpandable(true);
		if(props.getProperty("splitRatio")!=null){
			double ratio=Double.parseDouble(
					props.getProperty("SplitRatio", ""+0.75));
			split.setDividerLocation( ratio );
			split.setResizeWeight( ratio);
		}

		// Setup Main Frame
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(pnlSouth, BorderLayout.SOUTH);
		frame.getContentPane().add(split, BorderLayout.CENTER);

		// Allows the Swing App to be closed
		frame.addWindowListener(new ListenCloseWdw());

		//Add Menu listener
		mbOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser(
						props.getProperty("filename") );
				int returnVal= fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					props.setProperty("filename", 
							fc.getSelectedFile().getAbsolutePath() );
					loadList(props.getProperty("filename"));
				}
			}});
		mbSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}});
		mbQuit.addActionListener(new ListenMenuQuit());
	}

	public class ListenMenuQuit implements ActionListener{
		public void actionPerformed(ActionEvent e){
			quit();
		}
	}

	public class ListenCloseWdw extends WindowAdapter{
		public void windowClosing(WindowEvent e){
			quit();         
		}
	}

	public void refreshCost(){
		refreshCost(rootCategory);
		DecimalFormat df = new DecimalFormat("#.00");
		totalCost.setText(COST_STRING+df.format(
				((Category)rootCategory.getUserObject()).cost) );
		catTree.repaint();
	}

	public void refreshCost(DefaultMutableTreeNode node){
		double sumCost=0;
		int sumCant=0;
		if(node.getChildCount()!=0){
			DefaultMutableTreeNode child=(DefaultMutableTreeNode)node.getFirstChild();
			while(child!=null) {
				refreshCost(child);
				sumCost+=((Category)child.getUserObject()).cost;
				sumCant+=((Category)child.getUserObject()).cant;
				child=(DefaultMutableTreeNode)node.getChildAfter(child);
			}
			((Category)node.getUserObject()).cost+=sumCost;
			((Category)node.getUserObject()).cant+=sumCant;
		}
	}

	public TreeNode searchChild(DefaultMutableTreeNode node, Category cat){
		DefaultMutableTreeNode child=null;
		if(node.getChildCount()!=0){
			child=(DefaultMutableTreeNode)node.getFirstChild();
			while(child!=null) {
				if(((Category)child.getUserObject()) == cat)
					break;
				child=(DefaultMutableTreeNode)node.getChildAfter(child);
			}
		}
		return child;
	}

	public void quit() {
		try{
			FileOutputStream out = new FileOutputStream(PROP_NAME);
			props.setProperty("PreferredWidth", 
					""+frame.getWidth());
			props.setProperty("PreferredHeight", 
					""+frame.getHeight());
			props.setProperty("SplitRatio", ""+
					((double)split.getDividerLocation())/frame.getWidth() );
			props.store(out, "---No Comment---");
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void loadList(String filename){
		File localFile = new File(filename);
		filename = "file:///" + localFile.getAbsolutePath ( );  
		try {
			Document doc = editor.getDocument();
			doc.putProperty(Document.StreamDescriptionProperty, null);
			editor.setPage ( filename );
			editor.setEditable(true);
		} catch ( Exception e1 ) {  
			editor.setText ( "Could not load page:" + filename + "\n" +   
					"Error:" + e1.getMessage ( ) );  
		}
		parseEditor();
	}

	public void parseEditor(){
		Category currentCat=new Category("Root", 0);
		DefaultMutableTreeNode node=
				new DefaultMutableTreeNode(currentCat );
		rootCategory=node;
		catTree.setModel( new DefaultTreeModel(rootCategory) );
		Element root = editor.getDocument().getDefaultRootElement();
		int curCatLevel=1;
		for(int ii=0;ii<root.getElementCount();ii++){
			int start=root.getElement(ii).getStartOffset();
			int end=root.getElement(ii).getEndOffset();
			try {
				String line=editor.getText(start, end-start);
				if(!line.trim().isEmpty()){
					if(line.startsWith(catMark)){
						int catLevel=curCatLevel-countCatLevel(line);
						if(catLevel<0)catLevel=0;
						curCatLevel=curCatLevel+1-catLevel;
						while(catLevel>0){
							node=(DefaultMutableTreeNode)node.getParent();
							catLevel--;
						}
						Category newCat=new Category(
								line.substring(
										catMark.length()*countCatLevel(line),
										line.length()-1 ), 0);
						if(searchChild(node, newCat)==null){
							node.add(new DefaultMutableTreeNode(newCat));
							node=(DefaultMutableTreeNode)node.getLastChild();
						}
						currentCat=newCat;
						currentCat.cost=0;
					}else{
						String split[]=line.split(" ");
						double cost=Double.parseDouble(split[0].substring(1));
						int cant=1;
						if(split[1].startsWith(multiMark) ){
							cant=Integer.parseInt(
									split[1].substring(multiMark.length()) );
							cost*=cant;
						}
						currentCat.cant+=cant;
						currentCat.cost+=cost;
					}
				}
			} catch (Exception e) {
				System.err.println("At element "+ii);
				e.printStackTrace();
			}
		}
		refreshCost();
		catTree.setRootVisible(true);
		catTree.expandRow(0);
		catTree.setRootVisible(false);
	}

	public int countCatLevel(String line){
		int level=0;
		while(line.charAt(level)==catMark.charAt(0)){
			level++;
		}
		return level;
	}

	public void save(){
		if(props.getProperty("filename")!=null &&
				!props.getProperty("filename").trim().isEmpty()){
			BufferedWriter out;
			try {
				out = new BufferedWriter(new FileWriter(
						props.getProperty("filename")));
				try{
					editor.write(out);
				}finally{
					out.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void launchFrame(){
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		refreshCost();
		frame.pack();
		frame.setLocationRelativeTo(null);
		String filename=props.getProperty("filename");
		if(filename!=null && !filename.trim().isEmpty())loadList(filename);
		frame.setVisible(true);
	}

	public static void main(String args[]){
		MainFrame gui = new MainFrame();
		gui.launchFrame();
	}
}