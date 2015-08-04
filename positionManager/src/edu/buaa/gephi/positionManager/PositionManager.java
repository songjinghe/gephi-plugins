package edu.buaa.gephi.positionManager;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.tools.spi.Tool;
import org.gephi.tools.spi.ToolEventListener;
import org.gephi.tools.spi.ToolSelectionType;
import org.gephi.tools.spi.ToolUI;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Tool which can save/restore node positions to/from file.
 * <p>
 * The tool has two button and a text field.
 * you should fill the text field with file name( or absolute path )
 * and press "save" button, then the colors were saved to the file
 * you specified in the text field.
 * <p>
 * The tool identify nodes by their labels, so your graph must have
 * unique label for each node. When restore from file, only the nodes
 * which match the labels in file would be colored, others will keep
 * their original color.
 * 
 * @author Song Jing He
 */
@ServiceProvider(service = Tool.class)
public class PositionManager implements Tool {

    private final PositionManagerUI ui = new PositionManagerUI();

    @Override
    public void select() {
    }

    @Override
    public void unselect() {
    }

    private void operate(String op) throws UnsupportedEncodingException, IOException {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        if (pc.getCurrentProject() == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("No project opened in Gephi."));
            return;
        }

        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = gc.getModel();
        Graph graph = graphModel.getGraph();

        //System.out.println(System.getProperty("user.dir"));
        File file = new File(ui.getText());
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        if (op.equals("restore")) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("node position will be restore from "+file.getAbsolutePath()));
            String lineContent;
            Map<String, Node> nodeMap = new HashMap<String, Node>();
            for (Node node : graph.getNodes()) {
                nodeMap.put(node.getNodeData().getLabel(), node);
            }
            while ((lineContent = br.readLine()) != null) {
                String[] content = lineContent.split(":xyz:");
                String nodeName = content[0];
                String[] position = content[1].split(",");
                Node node = nodeMap.get(nodeName);
                if (node == null) {

                } else {
                    node.getNodeData().setX(Float.valueOf(position[0]));
                    node.getNodeData().setY(Float.valueOf(position[1]));
                    node.getNodeData().setZ(Float.valueOf(position[2]));
                }
            }
            br.close();
        } else {
            StringBuilder str = new StringBuilder();
            for (Node node : graph.getNodes().toArray()) {
                str.append(node.getNodeData().getLabel())
                        .append(":xyz:")
                        .append(node.getNodeData().x()).append(",")
                        .append(node.getNodeData().y()).append(",")
                        .append(node.getNodeData().z()).append("\n");
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(str.toString().getBytes("UTF-8"));
            bos.close();
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("node position have been saved to "+file.getAbsolutePath()));
        }
    }

    @Override
    public ToolEventListener[] getListeners() {
        return new ToolEventListener[]{};
    }

    @Override
    public ToolUI getUI() {
        return ui;
    }

    @Override
    public ToolSelectionType getSelectionType() {
        return ToolSelectionType.SELECTION;

    }

    private static class PositionManagerUI implements ToolUI {

        private JTextField fileInput;

        public String getText() {
            return fileInput.getText();
        }

        @Override
        public JPanel getPropertiesBar(Tool tool) {
            final PositionManager myTool = (PositionManager) tool;
            JPanel panel = new JPanel();

            //Buttons
            JButton saveToColumnButton = new JButton("save position");
            saveToColumnButton.setDefaultCapable(true);
            saveToColumnButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        myTool.operate("save");
                    } catch (IOException ex) {
                        Logger.getLogger(PositionManager.class.getName()).log(Level.SEVERE, null, ex);
                        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error: unable to save to file."));
                    }
                }
            });
            JButton applyToLayoutButton = new JButton("restore position");
            applyToLayoutButton.setDefaultCapable(true);
            applyToLayoutButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        myTool.operate("restore");
                    } catch (IOException ex) {
                        Logger.getLogger(PositionManager.class.getName()).log(Level.SEVERE, null, ex);
                        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error: unable to restore from file."));
                    }
                }
            });
            fileInput = new JTextField("save to/restore from file:", 16);
            panel.add(saveToColumnButton);
            panel.add(applyToLayoutButton);
            panel.add(fileInput);
            return panel;
        }

        @Override
        public Icon getIcon() {
            return new ImageIcon(getClass().getResource("/edu/buaa/gephi/positionManager/resources/pm.png"));
        }

        @Override
        public String getName() {
            return "Save/Restore Node position";
        }

        @Override
        public String getDescription() {
            return "save/restore node position to a file";
        }

        @Override
        public int getPosition() {
            return 1000;
        }
    }
}