
/*
 * Copyright 2021 Mark-Christoph Müller
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package org.eml.MMAX2.annotation.markables;

// Nodes returned to StyleSheet
import java.awt.Color;
import java.awt.Cursor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.ElementImpl;
import org.apache.xpath.NodeSet;
import org.eml.MMAX2.annotation.query.MMAX2MatchingCriterion;
import org.eml.MMAX2.annotation.query.MMAX2QueryException;
import org.eml.MMAX2.annotation.query.MMAX2QueryTree;
import org.eml.MMAX2.annotation.scheme.MMAX2AnnotationScheme;
import org.eml.MMAX2.annotation.scheme.MMAX2Attribute;
import org.eml.MMAX2.annotation.scheme.UIMATypeMapping;
import org.eml.MMAX2.api.AttributeAPI;
import org.eml.MMAX2.api.MarkableLevelAPI;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.discourse.MMAX2Discourse;
import org.eml.MMAX2.discourse.MMAX2DiscourseElement;
import org.eml.MMAX2.discourse.MMAX2DiscourseElementSequence;
import org.eml.MMAX2.gui.display.MMAX2OneClickAnnotationSelector;
import org.eml.MMAX2.gui.display.MarkableLevelRenderer;
import org.eml.MMAX2.gui.document.MMAX2Document;
import org.eml.MMAX2.gui.windows.MMAX2MarkableBrowser;
import org.eml.MMAX2.gui.windows.MMAX2MarkablePointerBrowser;
import org.eml.MMAX2.gui.windows.MMAX2MarkableSetBrowser;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public class MarkableLevel implements java.awt.event.ActionListener, MarkableLevelAPI
{    
    /** Reference to the markableDOM that the markables on this layer come from. */    
    private DocumentImpl markableDOM;       
    
    /** Reference to the discourse this MarkableLayer belongs to. */
    private MMAX2Discourse currentDiscourse;    
    /** HashMap mapping all Markables in this layer to their IDs. */
    private HashMap<String, Markable> markableHash;    
    /** Name of the markable xml file pertaining to this layer. */
    private String markableFileName="";    
    /** Name of the markable level (read from 'level' attribute). */
    private String markableLevelName="";    
    /** XML Header of the markable xml file pertaining to this layer. */
    private String markableFileHeader="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";    
    
    private String encoding = "UTF-8";
    /** HashMap which maps DE id string to arrays of markables associated with the DE. 
        Filled by this.registerMarkableAtDiscourseElement, used by getMarkablesAtDiscourseElement. */
    private String markableNameSpace="";
    private String dtdReference="<!DOCTYPE markables>";
    private HashMap<String, Markable[]> markablesAtDiscourseElement;
    /** HashMap which maps DE id string to arrays of markables started by the DE. 
        Filled by this.registerMarkableAtDiscourseElement, used by getMarkablesStartedByDiscourseElement. */
    private HashMap<String, Markable[]> startedMarkablesAtDiscourseElement;
    /** HashMap which maps DE id string to arrays of markables ended by the DE. 
        Filled by this.registerMarkableAtDiscourseElement, used by getMarkablesEndedByDiscourseElement. */
    private HashMap<String, Markable[]> endedMarkablesAtDiscourseElement;    
    /** Array containing at index X an array of those Markables associated with the DE with discourse position x.
        Filled by this.createDisplayPositionToMarkableMapping, used by this.getMarkableAtDiscoursePosition. */
    private Markable[][] markablesAtDiscoursePosition;    
    /** Boolean variable reflecting the current activation status of this MarkableLayer. */
    private boolean active; 
    /** Boolean variable reflecting the current visibility status of this MarkableLayer. */
    private boolean visible;
    /** Current position (0 to len -1) of this MarkableLayer in current ordering in MarkableChart. */
    private int position;
    private JComboBox<String> activatorComboBox = null;
    /** Arrow for moving this layer up in the hierarchy, appearing in MarkableLayerControlPanel. */
    private BasicArrowButton moveUp = null;
    /** Arrow for moving this layer down in the hierarchy, appearing in MarkableLayerControlPanel. */    
    private BasicArrowButton moveDown = null;
    
    private HashMap<String, MarkableRelation> markableSetRelations = null;    
    private HashMap<String, MarkableRelation> markablePointerRelations = null;
    
    private JButton updateCustomization = null;
    private JButton validateButton = null;
    private JButton deleteAllButton = null;
    
    private JCheckBox switchCustomizations = null;
        
    private MarkableLevelRenderer renderer = null;
    
    private JLabel nameLabel = null;
    
    private String matchableLevelName="";
    private boolean hasHandles = false;   
    private MMAX2AnnotationScheme annotationscheme = null;
        
    private MMAX2 mmax2 = null;
    private boolean dirty = false;
    
    private JMenuItem saveMenuItem = null;
    
    private String customizationFileName="";
    
    private boolean readOnly = false;
    
    boolean VERBOSE = false;
    boolean DEBUG = false;
    boolean PURGE_SINGLETON_SETS = true; 
    
    /** Creates new MarkableLevel */
    public MarkableLevel(DocumentImpl _markableDOM, String _markableFileName, String _markableLevelName, MMAX2AnnotationScheme _scheme, String _customizationFileName)
    {                        
    	try { if (System.getProperty("verbose").equalsIgnoreCase("true")) {VERBOSE = true;} }
    	catch (java.lang.NullPointerException x) { }

    	try { if (System.getProperty("debug").equalsIgnoreCase("true")) {DEBUG = true;} }
    	catch (java.lang.NullPointerException x) { }

    	try { if (System.getProperty("purge_singleton_sets").equalsIgnoreCase("false")) {PURGE_SINGLETON_SETS = false;} }
    	catch (java.lang.NullPointerException x) { }
    	    	
        customizationFileName=_customizationFileName;
        // ??
        matchableLevelName=","+_markableLevelName.toLowerCase()+",";        
         
        if (_markableDOM != null)
        {            
            //encoding = _markableDOM.getInputEncoding();
            //if (encoding == null)			 { encoding = "UTF-8"; }
        	if (_markableDOM.getInputEncoding() != null)
        	{
        		encoding = _markableDOM.getInputEncoding();
        	}
            if (encoding.equals("")==false)	 { markableFileHeader="<?xml version=\"1.0\" encoding=\""+encoding+"\"?>"; }            
            if (isDebug()) System.err.println("    Markable file header: "+markableFileHeader);
            
            markableDOM = _markableDOM;                   
            // Check for name space availability
            try
            {
                // New March 30, 2005: Check if xmlns is available at all            
                if (markableDOM.getElementsByTagName("markables").item(0).getAttributes().getNamedItem("xmlns") == null)
                {
                    JOptionPane.showMessageDialog(null,"Missing name space declaration on markable level "+_markableLevelName+"!\nEvery markable level file MUST have a name space declaration!\nMarkables on this level will not work properly!\n\nPlease modify the file\n"+_markableFileName,"Missing name space declaration!",JOptionPane.ERROR_MESSAGE);
                }
            }
            catch (java.lang.NullPointerException ex)
            {
            	String h = "Cannot access xmlns element in file "+_markableLevelName+"!\nProbably missing markables.dtd.";
            	System.err.println(h);
                JOptionPane.showMessageDialog(null,h,"Cannot access name space!",JOptionPane.ERROR_MESSAGE);
            }

            // Try to set name space
            try										 	{ markableNameSpace = markableDOM.getElementsByTagName("markables").item(0).getAttributes().getNamedItem("xmlns").getNodeValue(); }
            catch (java.lang.NullPointerException ex) 	{ }
                            
            // New March 30, 2005: Check if doctype is available at all            
            if (markableDOM.getDoctype() == null)
            {
                JOptionPane.showMessageDialog(null,"Missing DOCTYPE declaration on markable level "+_markableLevelName+"!\nEvery markable level file MUST have a DOCTYPE declaration!\nMarkables on this level will not work properly!\n\nPlease modify the file\n"+_markableFileName,"Missing DOCTYPE declaration!",JOptionPane.ERROR_MESSAGE);
            }
            else
            {            
                if (markableDOM.getDoctype().getPublicId() != null) 		{ dtdReference = "<!DOCTYPE markables PUBLIC \""+markableDOM.getDoctype().getPublicId()+"\">"; }
                else if (markableDOM.getDoctype().getSystemId() != null) 	{ dtdReference = "<!DOCTYPE markables SYSTEM \""+markableDOM.getDoctype().getSystemId()+"\">"; }
            }
        }
        
        markableFileName = _markableFileName;
        markableLevelName = _markableLevelName;
        annotationscheme = _scheme;
        markablesAtDiscourseElement = new HashMap<String, Markable[]>();
        startedMarkablesAtDiscourseElement = new HashMap<String, Markable[]>();
        endedMarkablesAtDiscourseElement = new HashMap<String, Markable[]>();                   
        markableSetRelations = new HashMap<String, MarkableRelation>();
        markablePointerRelations = new HashMap<String, MarkableRelation>();        
        
        if (_markableLevelName.equalsIgnoreCase("internal_basedata_representation")) 
        { 
        	System.err.println("Level for internal_basedata_representation!");
        	return; 
        }
        
        renderer = new MarkableLevelRenderer(this, customizationFileName);
        
        // Init GUI elements
        moveUp = new BasicArrowButton(SwingConstants.NORTH);
        if (isDefined()==false) { moveUp.setEnabled(false); }
        moveUp.addActionListener(this);
        moveUp.setToolTipText("Move this level up in hierarchy");
        moveDown = new BasicArrowButton(SwingConstants.SOUTH);
        if (isDefined()==false) { moveDown.setEnabled(false); }        
        moveDown.addActionListener(this);           
        moveDown.setToolTipText("Move this level down in hierarchy");
        
        activatorComboBox = new JComboBox<String>();
        activatorComboBox.setFont(activatorComboBox.getFont().deriveFont((float)10));
        activatorComboBox.setBorder(new EmptyBorder(0,0,1,1));        
        if (isDefined()==false) { activatorComboBox.setEnabled(false); }        
        activatorComboBox.setToolTipText("Activate/hide/deactivate this level");
        activatorComboBox.setActionCommand("activator");
        // Each MarkableLayer is active by default;
        active = true;
        visible = true;
        activatorComboBox.addItem("active");
        activatorComboBox.addItem("visible");
        activatorComboBox.addItem("inactive");
        activatorComboBox.addActionListener(this);
        
        nameLabel = new JLabel(markableLevelName);
        nameLabel.setOpaque(true);
        // Set foregroundcolor only if it is not transparent, otherwise black will be used
        if (renderer.getForegroundIsTransparent()==false) 	{ nameLabel.setForeground(renderer.getForegroundColor()); }
        else 												{ nameLabel.setForeground(Color.black); }
        
        // If the background is transparent, set label background to white, which is transparent
        if (renderer.getBackgroundIsTransparent()) 	{ nameLabel.setBackground(Color.white);}
        else 										{ nameLabel.setBackground(renderer.getBackgroundColor()); }        
        
        updateCustomization = new JButton("Update");
        updateCustomization.setFont(updateCustomization.getFont().deriveFont((float)10));
        updateCustomization.setBorder(new EmptyBorder(0,0,1,1));        
        updateCustomization.setActionCommand("update");
        updateCustomization.addActionListener(this);        
        if (customizationFileName.equals(""))
        { updateCustomization.setEnabled(false); }
        else
        {   
            updateCustomization.setEnabled(true);
            updateCustomization.setToolTipText(customizationFileName);
        }
        
        if (isDefined()==false) { updateCustomization.setEnabled(false); }
                
        validateButton = new JButton("Validate");
        validateButton.setFont(validateButton.getFont().deriveFont((float)10));
        validateButton.setBorder(new EmptyBorder(0,0,1,1));
        
        if (isDefined()==false) { validateButton.setEnabled(false); }
        
        validateButton.setActionCommand("validate");
        validateButton.addActionListener(this);

        deleteAllButton = new JButton("Delete");
        deleteAllButton.setFont(deleteAllButton.getFont().deriveFont((float)10));
        deleteAllButton.setBorder(new EmptyBorder(0,0,1,1));
        
        if (isDefined()==false) { deleteAllButton.setEnabled(false); }        
        deleteAllButton.setActionCommand("delete_all");
        deleteAllButton.addActionListener(this);        
        
        switchCustomizations = new JCheckBox("");
        if (isDefined()==false) { switchCustomizations.setEnabled(false); }
        switchCustomizations.setActionCommand("switch");
        switchCustomizations.addActionListener(this);
        switchCustomizations.setToolTipText("Activate / deactivate markable customization for this level");
        if (customizationFileName.equals(""))
        { switchCustomizations.setEnabled(false); }
        else
        {
            switchCustomizations.setSelected(true);
            renderer.updateSimpleMarkableCustomizations(true);
        }
        
        saveMenuItem = new JMenuItem(markableLevelName);
        saveMenuItem.setFont(MMAX2.getStandardFont());
        saveMenuItem.addActionListener(this);
        saveMenuItem.setActionCommand("save_this_level");
        saveMenuItem.setEnabled(false);                      
    }
    
    public boolean isVerbose()
    {
    	return VERBOSE;
    }

    public boolean isDebug()
    {
    	return DEBUG;
    }
    
    
/*    
    public HashSet getAllUIMATypeMappings()
    {
    	// Get mappings for all attributes in this level first
    	HashSet result = annotationscheme.getAllUIMAAttributeMappings();
    	// Add level-level mapping as well
    	if (uimaTypeMapping != null)
    	{
    		result.add(uimaTypeMapping);
    	}
    	return result;
    }
*/    
    public final boolean getIsReadOnly()
    {
        return readOnly;
    }
    
    public final void setIsReadOnly(boolean status)
    {
    	System.err.println("Setting readOnly to "+status+" for level"+this.getMarkableLevelName());
        readOnly = status;
        
    }    
    
    public final UIMATypeMapping getUIMATypeMapping()
    {
    	return annotationscheme.getUIMATypeMapping();
    }

    
    public final String getCustomizationFileName()
    {
        return customizationFileName;
    }
    
    public final MMAX2AnnotationScheme updateAnnotationScheme()
    {
        String temp = annotationscheme.getSchemeFileName();
        annotationscheme=null;
        annotationscheme = new MMAX2AnnotationScheme(temp);
        annotationscheme.setMMAX2(mmax2);
        return annotationscheme;        
    }
    
    public final void setMMAX2(MMAX2 _mmax2)
    {
        mmax2 = _mmax2;
        annotationscheme.setMMAX2(_mmax2);
    }
    
    public final boolean isDefined()
    {
        return markableDOM != null;
    }
    
    public final JMenuItem getSaveMarkableLevelItem()
    {
        return saveMenuItem;
    }
    
    public final boolean hasMarkableStartingAt(String deID)
    {
        return (startedMarkablesAtDiscourseElement.get(deID)!=null);
    }

    public final boolean hasMarkableEndingAt(String deID)
    {        
        return (endedMarkablesAtDiscourseElement.get(deID)!=null);
    }

    public final Markable getMarkableAtSpan(String span)
    {
        Markable result = null;
        Iterator allMarkables = markableHash.values().iterator();
        while (allMarkables.hasNext())
        {
            Markable temp =(Markable) allMarkables.next();
            if (span.equalsIgnoreCase(MarkableHelper.getSpan(temp)))
            {
                result = temp;
                break;
            }
        }                
        return result;
    }
    
    
    public final void setIsDirty(boolean status, boolean refresh)
    {        
        if (dirty != status)
        {
            dirty = status;        
            if (isVerbose()) System.err.println("MarkableLevel "+markableLevelName+" set to dirty="+status);
            if (currentDiscourse.getHasGUI()) { saveMenuItem.setEnabled(status); }
        }
        else
        {
            // The status to be set was already active
        }
        
        if (getCurrentDiscourse()!=null && getCurrentDiscourse().getMMAX2() != null)
        {
            if (currentDiscourse.getHasGUI())
            {
                getCurrentDiscourse().getMMAX2().updateIsAnnotationModified();
            }
        }
        
        if (currentDiscourse.getHasGUI())
        {
            ArrayList<?> activeBrowsers = null;
            if (refresh)
            {
                // New: check for and update currently active markable browsers
                activeBrowsers = getCurrentDiscourse().getMMAX2().getMarkableBrowsersForMarkableLevel(markableLevelName);
                for (int z=0;z<activeBrowsers.size();z++)
                {
                    ((MMAX2MarkableBrowser)activeBrowsers.get(z)).refresh();
                }                
            }
            if (getCurrentDiscourse().getMMAX2() != null)
            {
                activeBrowsers = getCurrentDiscourse().getMMAX2().getMarkableSetBrowsersForMarkableLevel(markableLevelName);
                for (int z=0;z<activeBrowsers.size();z++)
                {
                    ((MMAX2MarkableSetBrowser)activeBrowsers.get(z)).update();//IfDisplaying(markableLevelName, affectedAttribute);
                }
            }
            if (getCurrentDiscourse().getMMAX2() != null)
            {
                activeBrowsers = getCurrentDiscourse().getMMAX2().getMarkablePointerBrowsersForMarkableLevel(markableLevelName);
                for (int z=0;z<activeBrowsers.size();z++)
                {
                    ((MMAX2MarkablePointerBrowser)activeBrowsers.get(z)).update();//IfDisplaying(markableLevelName, affectedAttribute);
                }
            }
            
        }        
    }
    
    public final boolean getIsDirty()
    {
        return dirty;
    }
    
    
    /** This method returns a list of attribute names for which *all* the values in valueList are defined,
        or empty list. valueList is a comma-separated list of values, and the entire list is enclosed in 
        curly braces. */
    public final ArrayList getAttributeNamesForValues(String valueList, String optionalAttributeName)
    {
        return annotationscheme.getAttributeNamesForValues(valueList, optionalAttributeName);
    }
    
    public final void setValidateButtonEnabled(boolean status)
    {
        validateButton.setEnabled(status);
    }
    
    public final void validate()
    {
        if (isVerbose()) System.err.println("Validating "+markableHash.size() +" markables from MarkableLevel "+getMarkableLevelName());
        Iterator allMarkables = markableHash.values().iterator();
        Markable current = null;
        // Hide attribute window during validation
        getCurrentAnnotationScheme().getCurrentAttributePanel().getContainer().setVisible(false);
        // Iterate over all Markables on this level
        while (allMarkables.hasNext())
        {
            // Get next Markable
            current =(Markable)allMarkables.next();
            // Leave validation and adding of defaults to Attribute Window
            getCurrentAnnotationScheme().getCurrentAttributePanel().displayMarkableAttributes(current);
        }
        getCurrentAnnotationScheme().getCurrentAttributePanel().getContainer().setVisible(true);
        getCurrentAnnotationScheme().getCurrentAttributePanel().displayMarkableAttributes(null);
    }
    
    public final void deleteAllMarkables()
    {
        ArrayList temp = new ArrayList();
        Iterator allMarkables = markableHash.values().iterator();
        while (allMarkables.hasNext())
        {
            temp.add((Markable)allMarkables.next());
        }
        allMarkables = null;
        
        for (int b=0;b<temp.size();b++)
        {
            deleteMarkable((Markable)temp.get(b));
        }
    }
    
    public final void deleteMarkable(Markable deletee)
    {                   
        // Get set relations deletee may be part of
        MarkableRelation[] currentRelations = getActiveMarkableSetRelationsForMarkable(deletee);
        // Iterate over all set relations
        for (int b=0;b<currentRelations.length;b++)
        {
            MarkableRelation relation = currentRelations[b];
            MarkableSet set = relation.getMarkableSetContainingMarkable(deletee);
            if (set!=null)
            {
                set.removeMarkable(deletee);
            }
        }
        
        // Get reference to root node
        Node root= markableDOM.getElementsByTagName("markables").item(0);
        // Remove deletee (as node) from this
        root.removeChild(deletee.getNodeRepresentation());
        // Remove deletee from markableHash, the sole markable repository
        markableHash.remove(deletee.getID());
        unregisterMarkable(deletee);
        if (getCurrentDiscourse().getHasGUI())
        {
            Integer[] positions = currentDiscourse.removeDisplayAssociationsForMarkable(deletee);
            if (positions.length != 0)
            {
                renderer.removeHandlesAtDisplayPositions(positions);        
            }       
            deletee.renderMe(MMAX2Constants.RENDER_REMOVED);
            // Destroy further references to deletee markable
            currentDiscourse.getMMAX2().setCurrentSecondaryMarkable(null);
            currentDiscourse.getMMAX2().getCurrentTextPane().setCurrentHoveree(null,0);            
        }                
        deletee = null;
        setIsDirty(true,true);
    }

    public final Markable addMarkable(String[] discourseElementIDs, HashMap attributes)
    {
    	if (attributes == null) attributes = new HashMap();
    	ArrayList discourseElements = new ArrayList();
    	for (int n=0;n<discourseElementIDs.length;n++)
    	{
    		discourseElements.add(currentDiscourse.getDiscourseElementByID(discourseElementIDs[n]));
    	}
    	return addMarkable(discourseElements,attributes);
    }
    
    public final Markable addMarkable(ArrayList discourseElements, HashMap attributes)
    {
    	String[][] fragments = MarkableHelper.toFragments(discourseElements);
    	return addMarkable(fragments, attributes);
    }

    public final Markable addMarkable(String[][] fragments, HashMap attributes)
    {
        // Create new ID String 
        String id = currentDiscourse.getCurrentMarkableChart().getNextFreeMarkableID();
        // Get independent attribute with default values, incl. those dependent on default, and so on
        MMAX2Attribute[] mmaxAttributes = (MMAX2Attribute[])annotationscheme.getInitialAttributes().toArray(new MMAX2Attribute[0]);
        // Create node representation
        ElementImpl node =(ElementImpl) markableDOM.createElementNS(markableNameSpace,"markable");
        Node root= markableDOM.getElementsByTagName("markables").item(0);
        root.insertBefore((Node)node,root.getFirstChild());
        for (int i=0;i<mmaxAttributes.length;i++)
        {
            String currentAttrib = ((MMAX2Attribute)mmaxAttributes[i]).getDisplayName();
            if (attributes.containsKey(currentAttrib)==false)
            {
                attributes.put(new String(currentAttrib),new String(((MMAX2Attribute)mmaxAttributes[i]).getSelectedValue()));
                ((Element)node).setAttribute(new String(currentAttrib),new String(((MMAX2Attribute)mmaxAttributes[i]).getSelectedValue()));
            }
            else
            {
                // The supplied attributes (via create) have precedence over default ones
            }
        }
        ((Element)node).setAttribute(new String("id"),new String(id));
        // Create new markable object from above parameters
        Markable newMarkable = new Markable((Node)node,id,fragments,attributes,this);

//        System.out.println(fragments);
        
        markableHash.put(id, newMarkable);
        MarkableHelper.setDisplayPositions(newMarkable);
        
        for (int z=0;z<fragments.length;z++)
        {
            String[] currentFragment = fragments[z];
            for (int y=0;y<currentFragment.length;y++)
            {
                updateDiscoursePositionToMarkableMapping(fragments[z][y]);
            }
        }
        
        // Make sure Markable is displayed properly
        if (currentDiscourse.getHasGUI())
        {
            MMAX2Document doc =currentDiscourse.getDisplayDocument();
            doc.startChanges(newMarkable);               
            newMarkable.renderMe(MMAX2Constants.RENDER_UNSELECTED);
            doc.commitChanges();
        }
        // NEW: 
        setIsDirty(true,true);
        
        return newMarkable;
    }
    
    
    public final Markable addMarkable(String fragment)
    {
        // Create new ID String 
        String id = currentDiscourse.getCurrentMarkableChart().getNextFreeMarkableID();
        // Create fragments array of arrays from word4..word12; this is never discontinuous
        String[][] fragments = parseMarkableSpan(fragment,currentDiscourse.getWordDOM(),this);
        // Create and get attributes for new markable        
        HashMap attributes = new HashMap();
        // Get independent attribute with default values, incl. those dependent on default, and so on
        MMAX2Attribute[] mmaxAttributes = (MMAX2Attribute[])annotationscheme.getInitialAttributes().toArray(new MMAX2Attribute[0]);
        // Create node representation
        ElementImpl node =(ElementImpl) markableDOM.createElementNS(markableNameSpace,"markable");
        Node root= markableDOM.getElementsByTagName("markables").item(0);
        root.insertBefore((Node)node,root.getFirstChild());
        for (int i=0;i<mmaxAttributes.length;i++)
        {
            attributes.put(new String(((MMAX2Attribute)mmaxAttributes[i]).getDisplayName()),new String(((MMAX2Attribute)mmaxAttributes[i]).getSelectedValue()));
            ((Element)node).setAttribute(new String(((MMAX2Attribute)mmaxAttributes[i]).getDisplayName()),new String(((MMAX2Attribute)mmaxAttributes[i]).getSelectedValue()));
        }
        ((Element)node).setAttribute(new String("id"),new String(id));
        // Create new markable object from above parameters
        Markable newMarkable = new Markable((Node)node,id,fragments,attributes,this);        
        markableHash.put(id, newMarkable);
        MarkableHelper.setDisplayPositions(newMarkable);
        
        for (int z=0;z<fragments.length;z++)
        {
            String[] currentFragment = fragments[z];
            for (int y=0;y<currentFragment.length;y++)
            {
                updateDiscoursePositionToMarkableMapping(fragments[z][y]);
            }
        }
        
        // Make sure Markable is displayed properly
        if (currentDiscourse.getHasGUI())
        {
            MMAX2Document doc =currentDiscourse.getDisplayDocument();
            doc.startChanges(newMarkable);               
            newMarkable.renderMe(MMAX2Constants.RENDER_UNSELECTED);
            doc.commitChanges();
        } 
        setIsDirty(true,true);
        
        
        return newMarkable;
    }
            
    public final void saveMarkables(String newFileName)
    {
    	saveMarkables(newFileName, false);
    }
    
    public final void saveMarkables(String newFileName, boolean autoSaveMode)
    {
        if (getIsDirty()==false)
        {
        	if (autoSaveMode) System.err.print("Auto-Save: ");
            if (isVerbose()) System.err.println("Markable level "+getMarkableLevelName()+" is clean, not saving!");
            return;
        }
        
        if (getIsReadOnly()==true)
        {
        	if (autoSaveMode) { System.err.println("Auto-Save: "+"Markable level "+getMarkableLevelName()+" is READ-ONLY, not saving!");}
        	else              { JOptionPane.showMessageDialog(null,"Markable level "+getMarkableLevelName()+" is READ-ONLY, not saving!","Save problem",JOptionPane.INFORMATION_MESSAGE); }
            return;
        }
        
        if (autoSaveMode) System.err.print("Auto-Save: ");
        if (isVerbose()) System.err.println("Saving level "+getMarkableLevelName()+" ... ");
        if (newFileName.equals("")==false){ markableFileName = newFileName; }
                
        /* Test file for existence */
        File destinationFile = new File(markableFileName);
        
        if(destinationFile.exists())
        {        	
        	// The file does exist already
        	// Since it exists but is read-only, it is protected and should not be writable
            if (destinationFile.canWrite() == false)
            {
            	if (autoSaveMode) { System.err.println("Auto-Save: "+"Cannot save markables on level "+getMarkableLevelName()+"!'Write' not allowed!"); }
            	else              { JOptionPane.showMessageDialog(null,"Cannot save markables on level "+getMarkableLevelName()+"!\n'Write' not allowed!","Save problem:"+markableFileName,JOptionPane.WARNING_MESSAGE);	 }                
                return;
            }
        	
            /* The file to be written is already existing and writable, so create backup copy first*/
            /* This should be the normal case */
        	// 
            if (autoSaveMode && isVerbose()) System.err.print("Auto-Save: ");            
            if (isVerbose()) System.err.println("Filename "+destinationFile.getAbsolutePath()+" exists, creating *timestamped* backup file!");
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
                        
            File oldDestinationFile = new File(this.markableFileName +"."+timeStamp+".bak");
            destinationFile.renameTo(oldDestinationFile);
        }                   
        
	/* Write DOM to file */
        if (autoSaveMode) System.err.print("Auto-Save: ");
        System.err.println("Writing to file " + markableFileName);
        
        BufferedWriter fw = null;
        try 							{ fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(markableFileName),this.encoding)); }
        catch (java.io.IOException ex)	{ ex.printStackTrace(); }
        
        String rootElement ="";
        if (markableNameSpace.equals("")) { rootElement = "<markables>"; }
        else { rootElement = "<markables xmlns=\""+markableNameSpace+"\">"; }
                
        try
        {
            fw.write(markableFileHeader+"\n"+dtdReference+"\n"+rootElement+"\n");
            fw.flush();
        }
        catch (java.io.IOException ex)
        {
            System.out.println(ex.getMessage());
        }
        
        Set<String> allMarkableIDsSet = this.markableHash.keySet();
        if (isVerbose()) System.err.println("Saving "+allMarkableIDsSet.size()+" markables");
        Iterator<String> allMarkableIDs = allMarkableIDsSet.iterator();
        Markable currentMarkable = null;        
        while(allMarkableIDs.hasNext())
        {
            currentMarkable = (Markable) markableHash.get(allMarkableIDs.next());
            try
            {
                fw.write(MarkableHelper.toXMLElement(currentMarkable, getCurrentAnnotationScheme())+"\n");
            }
            catch (java.io.IOException ex) { System.err.println("Error saving "+(currentMarkable.getID())); }
        }        
        try
        {
            fw.write("</markables>");
            fw.close();
        }
        catch (java.io.IOException ex) { System.err.println(ex.getMessage()); }        
        setIsDirty(false,false);        
    }
               
    
    /** This method initializes one MarkableRelation object for each attribute of type MARKABLE_SET, MARKABLE_POINTER and SET_POINTER and
     *  adds it to this MarkableLevel's MarkableRelations list. */
    public final void initMarkableRelations(MMAX2 _mmax2)
    {
        MMAX2Attribute[] currentAttributes = null;
        MMAX2Attribute currentAttribute = null;
        Markable currentMarkable = null;
        String currentAttributeName = null;
        String currentValue = "";
        
        // Get all Attributes of type MARKABLE_SET
        currentAttributes = annotationscheme.getAttributesByType(AttributeAPI.MARKABLE_SET);
        // Iterate over all Attributes of type MARKABLE_SET
        for (int i=0;i<currentAttributes.length;i++)
        {
            // Get current Attribute, e.g. member
            currentAttribute = (MMAX2Attribute) currentAttributes[i];
            currentAttributeName = currentAttribute.getDisplayName();
            // Create one MarkableRelation for each Attribute of type MARKABLE_SET (always order, for now)
            MarkableRelation newRelation = new MarkableRelation(currentAttributeName,
            		currentAttribute.getType(),
            		true, 
            		currentAttribute.getLineWidth(), 
            		currentAttribute.getLineColor(), 
            		currentAttribute.getLineStyle(), 
            		currentAttribute.getMaxSize(),
            		currentAttribute.getIsDashed(), 
            		currentAttribute.getAttributeNameToShowInMarkablePointerFlag(),
            		_mmax2);

            // Add newly created MarkableRelation to respective HashMap
            markableSetRelations.put(currentAttributeName,newRelation);
            // Associate currentAttribute with pertaining MarkableRelation
            currentAttribute.setMarkableRelation(newRelation);
            newRelation = null;
        }
                
        // Iterate over all MarkableSetRelations
        Iterator<String> allAttributeNames = markableSetRelations.keySet().iterator();
        while (allAttributeNames.hasNext())
        {
            MarkableRelation currentRelation = (MarkableRelation) markableSetRelations.get((String)allAttributeNames.next());
            currentAttributeName = currentRelation.getAttributeName();
            
            // Iterate over all Markables on this MarkableLevel
            Set<String> allMarkableIDsSet = markableHash.keySet();
            Iterator<String> it = allMarkableIDsSet.iterator();
            while(it.hasNext())                
            {
                // Get current Markable
                currentMarkable = (Markable)markableHash.get(it.next());
                // Try to retrieve value of current attribute from current markable, e.g. from member
                currentValue = currentMarkable.getAttributeValue(currentAttributeName);
//            	System.err.println(currentValue);
                if (currentValue != null && currentValue.equals("")==false && currentValue.equals(MMAX2.defaultRelationValue)==false)
                {
                    // The current Markable has a non-empty value for the current attribute, so add current markable to Relation for this attribute
                    currentRelation.addMarkableWithAttributeValueToMarkableSet(currentMarkable,currentValue);
                    // Determine numerical value of this set id
                    int currentNum = MMAX2Utils.parseID(currentValue);
                    if (this.currentDiscourse.getCurrentMarkableChart().getNextFreeMarkableSetNum() <= currentNum)
                    {
                        this.currentDiscourse.getCurrentMarkableChart().setNextFreeMarkableSetNum(currentNum+1);
                    }
                }
            }
        }// for all attributes
        
        
        if (PURGE_SINGLETON_SETS)
        {
	        // Note: Some sets might be singletons if they were created / edited outside the tool
	        // Iterate over all MarkableSetRelations again
	        allAttributeNames = markableSetRelations.keySet().iterator();
	        while (allAttributeNames.hasNext())
	        {
	            MarkableRelation currentRelation = (MarkableRelation) markableSetRelations.get((String)allAttributeNames.next());
	        	System.err.println("\nNote: Purging singleton sets for attribute "+currentRelation.getAttributeName());
	            MarkableSet[] sets = currentRelation.getMarkableSets(true);
	            for (int m=0;m<sets.length;m++)
	            {
	            	MarkableSet set = sets[m];
	            	if (set.getSize()==1)
	            	{
	            		Markable sm = set.getInitialMarkable();
	            		System.err.println("\n"+sm);
	            		sm.setAttributeValue(currentRelation.getAttributeName(), MMAX2.defaultRelationValue);
	            		sm.setAttributeValueToNode(currentRelation.getAttributeName(), MMAX2.defaultRelationValue);
	            		set.removeMeFromMarkableRelation();
	            	}
	            }	            
	        }// for all attributes
        }        
        
        
        // Get all Attributes of type MARKABLE_POINTER
        currentAttributes = this.annotationscheme.getAttributesByType(AttributeAPI.MARKABLE_POINTER);
        // Iterate over all Attributes of type MARKABLE_POINTER
        for (int i=0;i<currentAttributes.length;i++)
        {
            // Get current Attribute, e.g. antecedent
            currentAttribute = (MMAX2Attribute) currentAttributes[i];
            currentAttributeName = currentAttribute.getDisplayName();
            // Create one MarkableRelation for each Attribute of type MARKABLE_POINTER (always order, for now)
            MarkableRelation newRelation = new MarkableRelation(currentAttributeName,currentAttribute.getType(),true, currentAttribute.getLineWidth(), currentAttribute.getLineColor(), currentAttribute.getLineStyle(),currentAttribute.getMaxSize(),currentAttribute.getIsDashed(), currentAttribute.getAttributeNameToShowInMarkablePointerFlag(), mmax2);
            // Add newly created MarkableRelation to respective HashMap
            markablePointerRelations.put(currentAttributeName,newRelation);
            // Associate currentAttribute with pertaining MarkableRelation
            currentAttribute.setMarkableRelation(newRelation);            
            newRelation = null;
        }
        
        // Iterate over all MarkablePointerRelations
        allAttributeNames = markablePointerRelations.keySet().iterator();
        while (allAttributeNames.hasNext())
        {
            MarkableRelation currentRelation = (MarkableRelation) markablePointerRelations.get((String)allAttributeNames.next());
            currentAttributeName = currentRelation.getAttributeName();
            
            // Iterate over all Markables on this MarkableLevel
            Set allMarkableIDsSet = markableHash.keySet();
            Iterator it = allMarkableIDsSet.iterator();
            while(it.hasNext())
            {
                // Get current Markable
                currentMarkable = (Markable)markableHash.get(it.next());
                // Try to retrieve value of current attribute from current markable, e.g. from member
                currentValue = currentMarkable.getAttributeValue(currentAttributeName);
                if (currentValue != null && currentValue.equals("")==false && currentValue.equals(MMAX2.defaultRelationValue)==false)
                {
                    // The current Markable has a non-empty value for the current attribute, so add current markable to Relation for this attribute
                    currentRelation.createMarkablePointer(currentMarkable,this);
                }
            }
        }// for all attributes        
    }
    
    public final MarkableRelation[] getActiveMarkableSetRelationsForMarkable(Markable markable)
    {
        ArrayList templist = new ArrayList();
        MarkableRelation[] result = new MarkableRelation[0];
        Iterator allAttributeNames = markableSetRelations.keySet().iterator(); // slow
        while (allAttributeNames.hasNext())
        {
            MarkableRelation currentRelation = (MarkableRelation) markableSetRelations.get((String)allAttributeNames.next());
            String currentAttributeName = currentRelation.getAttributeName();
            // Todo: Optimize acces to getAttributeValue!
            if (markable.getAttributeValue(currentAttributeName)!=null && markable.getAttributeValue(currentAttributeName).equals("")==false && markable.getAttributeValue(currentAttributeName).equals(MMAX2.defaultRelationValue)==false)
            {
                // The currentMarkable has a valid value for the current MarkableRelation
                templist.add(currentRelation);
            }
        }
        if (templist.size()!=0)
        {
            result = new MarkableRelation[templist.size()];
            result = (MarkableRelation[]) templist.toArray(new MarkableRelation[1]);
        }
        return result;
    }

    public final MarkablePointer[] getActiveMarkablePointersForTargetMarkable(Markable markable, String pointerRelationName)
    {
        ArrayList templist = new ArrayList();
        MarkablePointer[] result = null;
        
        // Get MarkablePointer of name pointerRelationName
        MarkableRelation requiredPointerRelation = (MarkableRelation)markablePointerRelations.get(pointerRelationName);
        // Get list of all MarkablePointers pointing to markable
        templist = (ArrayList)Arrays.asList(requiredPointerRelation.getMarkablePointersWithTargetMarkable(markable));
        
        if (templist.size()!=0)
        {
            //result = new MarkableRelation[templist.size()];
            result = (MarkablePointer[]) templist.toArray(new MarkablePointer[0]);
        }
        else
        {
            result = new MarkablePointer[0];
        }
        return result;        
    }
    
    public final MarkableRelation[] getActiveMarkablePointerRelationsForSourceMarkable(Markable markable)
    {
        ArrayList templist = new ArrayList();
        MarkableRelation[] result = new MarkableRelation[0];
        Iterator allAttributeNames = markablePointerRelations.keySet().iterator();//slow
        // Iterate over all pointer relations (by name)
        while (allAttributeNames.hasNext())
        {
            // Get current MarkablePointerRelation
            MarkableRelation currentRelation = (MarkableRelation) markablePointerRelations.get((String)allAttributeNames.next());
            // Get attribute name associated with relation
            String currentAttributeName = currentRelation.getAttributeName();
            // Todo: Optimize access to getAttributeValue!
            if (markable.getAttributeValue(currentAttributeName) !=null && 
                markable.getAttributeValue(currentAttributeName).equals("")==false && 
                markable.getAttributeValue(currentAttributeName).equals(MMAX2.defaultRelationValue)==false)
            {
                // The currentMarkable has a valid value for the current MarkableRelation
                // So, it is the source markable os a pointer relation
                templist.add(currentRelation);
            }
        }
        if (templist.size()!=0)
        {
            result = new MarkableRelation[templist.size()];
            result = (MarkableRelation[]) templist.toArray(new MarkableRelation[1]);
        }
        return result;
    }

    public final ArrayList getMarkablePointersForTargetMarkable(Markable markable)
    {
        ArrayList result = new ArrayList();
        Iterator allAttributeNames = markablePointerRelations.keySet().iterator();//slow
        while (allAttributeNames.hasNext())
        {
            // Get current MarkablePointerRelation
            MarkableRelation currentRelation = (MarkableRelation) markablePointerRelations.get((String)allAttributeNames.next());
            result.addAll(Arrays.asList(currentRelation.getMarkablePointersWithTargetMarkable(markable)));
        }
        return result;
    }
    
    
    public final void destroyDependentComponents()
    {
        // Iterate over all Markables on this MarkableLevel
        Set allMarkableIDsSet = markableHash.keySet();
        Iterator it = allMarkableIDsSet.iterator();
        while(it.hasNext())
        {
            ((Markable)markableHash.get(it.next())).destroyDependentComponents();
        }
    
        currentDiscourse = null;
        markableDOM = null;
        endedMarkablesAtDiscourseElement.clear();
        endedMarkablesAtDiscourseElement = null;
        startedMarkablesAtDiscourseElement.clear();
        startedMarkablesAtDiscourseElement = null;
        markableHash.clear();
        markableHash = null;
        markablesAtDiscourseElement.clear();
        markablesAtDiscourseElement = null;        
        markablesAtDiscoursePosition = null;
        renderer.destroyDependentComponents();
        renderer = null;
        moveUp.removeActionListener(this);
        moveUp = null;
        moveDown.removeActionListener(this);
        moveDown = null;
        updateCustomization.removeActionListener(this);
        updateCustomization = null;
        activatorComboBox.removeActionListener(this);
        activatorComboBox = null;
        switchCustomizations.removeActionListener(this);
        switchCustomizations = null;
        markableSetRelations.clear();
        markableSetRelations = null;
        markablePointerRelations.clear();
        markablePointerRelations = null;
        annotationscheme.destroyDependentComponents();
        annotationscheme = null;
//        System.gc();        
    }
    
    public final MMAX2AnnotationScheme getCurrentAnnotationScheme()
    {
        return this.annotationscheme;
    }
    
    protected final void resetMarkablesForStyleSheetReapplication()
    {
        // TODO: reset this.markablesAtDiscoursePosition, because discourse positions may change as the result of style sheet reapplication
        // Iterate over all Markables on this MarkableLevel
        Set allMarkableIDsSet = markableHash.keySet();
        Iterator it = allMarkableIDsSet.iterator();
        while(it.hasNext())        

        {
            ((Markable) markableHash.get(it.next())).resetHandles();
        }
    }
    
    /** This method returns the currently valid background color defined for DisplayPosition displayPosition. 
        It is determined on the basis of the markable(s) this MarkableLayer has on this position. If no markables exist on this 
        position, it is assumjed to be a MarkableHandle, and the color is determined ...*/
    public final Color getBackgroundColorAtDisplayPosition(int displayPosition)
    {
        Color resultColor = null;
        int discoursePosition = 0;
        
        Markable[] markablesAtDisplayPosition = getAllMarkablesAtDiscoursePosition(currentDiscourse.getDiscoursePositionAtDisplayPosition(displayPosition));
        return resultColor;
    }
        
    public final int getSize()
    {
        System.err.println("MarkableLevel.getSize() is deprecated! Use getMarkableCount() instead!");
        return this.markableHash.size();
    }

    public final int getMarkableCount()
    {
        return this.markableHash.size();
    }
    
    
    public final JLabel getNameLabel()
    {
        return this.nameLabel;
    }
    
    public final JComboBox getActivatorComboBox()
    {
        return activatorComboBox;
    }

    public final JCheckBox getSwitchCheckBox()
    {
        return this.switchCustomizations;
    }
    
    public final BasicArrowButton getMoveUpButton()
    {
        return this.moveUp;
    }

    public final BasicArrowButton getMoveDownButton()
    {
        return this.moveDown;
    }
    
    public final JButton getUpdateButton()
    {
        return this.updateCustomization;
    }
   
    public final JButton getValidateButton()
    {
        return validateButton;
    }

    public final JButton getDeleteButton()
    {
        return deleteAllButton;
    }
    
    public final void setPosition(int pos)
    {        
        position = pos;
        moveUp.setActionCommand("up:"+pos);
        moveDown.setActionCommand("down:"+pos);
    }
    
    public final int getPosition()
    {
        return position;
    }
    
    public final String getMarkableFileName()
    {
        return markableFileName;
    }
    
    public final String getAbsoluteMarkableFileName()
    {
        File temp = new File(markableFileName);
        return temp.getAbsolutePath();
    }
    
    public final String getMarkableLevelName()
    {
        return markableLevelName;
    }
    
    public final String getMatchableMarkableLevelName()
    {
        return matchableLevelName;
    }
    
    public final MarkableLevelRenderer getRenderer()
    {
        return renderer;
    }
    
    public final boolean getIsActive()
    {
        return active;
    }

    public final boolean getIsVisible()
    {
        return visible;
    }
    
    public final boolean getHasHandles()
    {
        return hasHandles;
    }
    
    public final void setHasHandles(boolean status)
    {
        hasHandles = status;        
    }
    
    /** Used by MMAX query. */
    public final ArrayList getMarkablesMatchingAll(MMAX2MatchingCriterion criterion)
    {
        ArrayList resultList = new ArrayList();
        if (markableLevelName.equalsIgnoreCase("internal_basedata_representation")==false)
        {
            Markable currentMarkable = null;
            ArrayList list = new ArrayList(markableHash.values());
            for (int t=0;t<list.size();t++)
            {
                currentMarkable = (Markable)list.get(t);
                if (MarkableHelper.matchesAll(currentMarkable,criterion))
                {
                    resultList.add(currentMarkable);
                }
            }
        }
        else
        {
            MMAX2DiscourseElement currentDE = null;
            ArrayList list = (ArrayList)java.util.Arrays.asList(getCurrentDiscourse().getDiscourseElements());
            for (int t=0;t<list.size();t++)
            {
                currentDE = (MMAX2DiscourseElement)list.get(t);
                if (MarkableHelper.matchesAll(currentDE, criterion))
                {
                    resultList.add(currentDE);
                }
            }                        
        }
        return resultList;
    }
    
    /** Used by MMAX query. */
    public final ArrayList getMarkablesMatchingAny(MMAX2MatchingCriterion criterion)
    {        
        ArrayList resultList = new ArrayList();
        if (markableLevelName.equalsIgnoreCase("internal_basedata_representation")==false)
        {
            Markable currentMarkable = null;
            ArrayList list = new ArrayList(markableHash.values());
            for (int t=0;t<list.size();t++)
            {
                currentMarkable = (Markable)list.get(t);
                if (MarkableHelper.matchesAny(currentMarkable, criterion))
                {
                    resultList.add(currentMarkable);
                }
            }
        }
        else
        {
            // This level is a de level!
            MMAX2DiscourseElement currentDE = null;
            ArrayList list = (ArrayList)java.util.Arrays.asList(getCurrentDiscourse().getDiscourseElements());
            for (int t=0;t<list.size();t++)
            {
                currentDE = (MMAX2DiscourseElement)list.get(t);
                if (MarkableHelper.matchesAny(currentDE, criterion))
                {
                    resultList.add(currentDE);
                }
            }            
        }
        return resultList;
    }
   
    
    public final ArrayList getMarkables()
    {
        return new ArrayList(markableHash.values());
    }

    public final ArrayList getMarkables(Comparator comp)
    {
        ArrayList temp =  new ArrayList(markableHash.values());
        if (comp != null)
        {
            Markable[] tempArray =  (Markable[])temp.toArray(new Markable[0]);;
            java.util.Arrays.sort(tempArray,comp);
            temp =  new ArrayList(java.util.Arrays.asList(tempArray));
        }
        return temp;
    }
   
    
    public final ArrayList  getMatchingMarkables(String queryString)
    {
        MMAX2QueryTree tree = null;
        try
        {
            tree = new MMAX2QueryTree(queryString, this);
        }
        catch (MMAX2QueryException ex)
        {
        	ex.printStackTrace();
            return new ArrayList();
        }

        if (tree != null)
        {        
            return tree.execute(new DiscourseOrderMarkableComparator());
        }
        else
        {
            return new ArrayList();
        }
    }
    
    public final void updateMarkables()
    {        
        startedMarkablesAtDiscourseElement = null;
        startedMarkablesAtDiscourseElement = new HashMap();
        endedMarkablesAtDiscourseElement = null;
        endedMarkablesAtDiscourseElement = new HashMap();
        
        markablesAtDiscourseElement=null;
        markablesAtDiscourseElement=new HashMap();       
        
        NodeList allMarkableNodes = markableDOM.getElementsByTagName("markable");
        Node currentMarkableNode = null;
        int len = allMarkableNodes.getLength();
        
        String currentID = "";
        String currentSpan = "";
        
        Markable currentMarkable=null;
        // Iterate over all Markable elements
        for (int z=0;z<len;z++)
        {
            // Get current markable element
            currentMarkableNode = allMarkableNodes.item(z);
            currentID = currentMarkableNode.getAttributes().getNamedItem("id").getNodeValue();            

            currentMarkable = getMarkableByID(currentID);

            currentSpan = MarkableHelper.getSpan(currentMarkable);
            currentMarkable.update(parseMarkableSpan(currentSpan, currentDiscourse.getWordDOM(),this));
        }                
    }
    
    /** This method is called from the MMAX2DiscourseLoader after the MMAX2Discourse field has been set
        on this level. Markable spans are expanded on the basis of the element IDs actually
        contained in the base data, and not by mere interpolation of integer IDs!         
        Synopsis: This method basically iterates over all elements in this.markableDOM, creates a Markable object
        for each by means of the Markable constructor, and adds that to this.markableHash.      
     */
    public final int createMarkables()
    {        
        boolean readOnlyAtStart=getIsReadOnly();        
        int maxIDNum = 0;
        boolean added = false;
        if (isDefined())
        {            
            // Get list of all markable elements currently existing on this level. 
        	// These were read from a markables file.
        	// Markables might contain attributes that do not conform to anno scheme!!
            NodeList allMarkableNodes = markableDOM.getElementsByTagName("markable");
            Node currentMarkableNode = null;
            int len = allMarkableNodes.getLength();        
            markableHash = new HashMap<String, Markable>(len);            
            String currentID = "";
            int currentIDNum = 0;
            String currentSpan = "";
        
            Markable newMarkable=null;
            HashMap<String, String> attributes = null;
            // Iterate over all Markable elements
            for (int z=0;z<len;z++)
            {
                currentMarkableNode = allMarkableNodes.item(z);
                if (currentMarkableNode.getAttributes().getNamedItem("mmax_level")==null)
                {
                    ((Element)currentMarkableNode).setAttribute("mmax_level",getMarkableLevelName());
                    setIsDirty(true, false);
                    // Remember that soth. was added
                    added = true;
                }
                
                // Create attributes HashMap for Markable object attributes.
                // Attributes and values might come in any casing, and need to be normalized to their correct one
                // System.err.println(currentMarkableNode.getAttributes());
                attributes = MMAX2Utils.convertNodeMapToHashMap(currentMarkableNode.getAttributes(), this.getCurrentAnnotationScheme());
                // 1.15: attributes and values are in canonical casing here (i.e. as defined in scheme)
                
                // Extract numerical part of current id
                currentIDNum = MMAX2Utils.parseID((String)attributes.get("id"));
                if (currentIDNum > maxIDNum) maxIDNum = currentIDNum;
                // Remove id and span attributes.
                attributes.remove("id");
                attributes.remove("span");            
                try { currentID = currentMarkableNode.getAttributes().getNamedItem("id").getNodeValue(); }
                catch (java.lang.NullPointerException ex)
                {
                    JOptionPane.showMessageDialog(null,"Missing ID attribute on markable!","MarkableLevel: "+markableFileName,JOptionPane.ERROR_MESSAGE);                
                }
            
                try { currentSpan = currentMarkableNode.getAttributes().getNamedItem("span").getNodeValue(); }
                catch (java.lang.NullPointerException ex)
                {
                    JOptionPane.showMessageDialog(null,"Missing span attribute on markable!","MarkableLevel: "+markableFileName,JOptionPane.ERROR_MESSAGE);                                
                }            

                // Create new Markable object (not much will happen there)
                // attributes hash has been normalized already. currentMarkableNode is still in the format read from the file. 
                newMarkable = new Markable(currentMarkableNode, currentID, parseMarkableSpan(currentSpan,this.currentDiscourse.getWordDOM(),this), attributes,this);
                
                // Create mapping of Markable to its ID
                markableHash.put(currentID, newMarkable);
                newMarkable = null;            
            }
        }  
        else { markableHash = new HashMap<String, Markable>(); }
        
        if (added)
        {
            if (getCurrentDiscourse().getHasGUI())
            {
                JOptionPane.showMessageDialog(null,"The attribute 'mmax_level' has been added to at least one markable on level "+getMarkableLevelName()+"!\nPlease make sure to save this level later.","Markable level has been modified!",JOptionPane.INFORMATION_MESSAGE);
            }
            else
            {
                System.err.println("The attribute 'mmax_level' has been added to at least one markable on level "+getMarkableLevelName()+"!");
            }
        }        
        if (readOnlyAtStart == false && getIsReadOnly()) { System.err.println("Level "+getMarkableLevelName()+" has been set to read-only!"); }
                
        return maxIDNum;
    }

    public void setCurrentDiscourse(MMAX2Discourse _discourse)
    {
        currentDiscourse = _discourse;
    }
    
    public MMAX2Discourse getCurrentDiscourse()
    {
        return currentDiscourse;
    }        
    
    public Markable getMarkableByID(String markableId)
    {
        return (Markable) this.markableHash.get(markableId);
    }

    public final Markable[] getAllMarkablesStartingWith(MMAX2DiscourseElementSequence sequence)
    {       
        MMAX2DiscourseElement[] elements = sequence.getContent();
        ArrayList temp = new ArrayList();
        // Get disc pos of last element in sequence
        int lastDiscPosInElements = elements[elements.length-1].getDiscoursePosition();
        // Get all markables started by the first DE in parameter list
        Markable[] started = getAllMarkablesStartedByDiscourseElement(elements[0].getID());
        // Iterate over all markables started at first DE
        for (int z=0;z<started.length;z++)
        {
            // Get final disc pos of current markable
            int currentFinalDiscPos = started[z].getRightmostDiscoursePosition();
            if (currentFinalDiscPos <= lastDiscPosInElements)
            {
                // Add it if it is earlier or equal
                temp.add(started[z]);
            }
        }
        return (Markable[]) temp.toArray(new Markable[0]);
    }

    public final Markable getSingleLongestMarkableStartingWith(MMAX2DiscourseElementSequence sequence)
    {
        Markable result = null;
        Markable[] candidates = getAllMarkablesStartingWith(sequence);
        if (candidates.length==1)
        {
            result = candidates[0];
        }
        else if (candidates.length > 0)
        {
            //java.util.Arrays.sort(candidates,new ShorterBeforeLongerComparator());
            java.util.Arrays.sort(candidates,new DiscourseOrderMarkableComparator());
            if (candidates[candidates.length-1].getSize() != candidates[candidates.length-2].getSize())
            {
                result = candidates[candidates.length-1];
            }
        }
        if (result != null && result.getSize() < sequence.getLength())
        {
            result = null;
        }            
        return result;
    }
    
    
    /** This method returns an array of those Markable objects associated with discourseElement Id, or empty array if none. 
        Since this is on MarkableLayer level, no distinction is made wrt to active/inactive. 
        The retrieved Array comes from a hash, so this method is efficient (a little less so if sort==true,
        which causes the markables to be sorted in discourse order, shorter before longer ones). */
    public Markable[] getAllMarkablesAtDiscourseElement(String discourseElementId, boolean sort)
    {
        Markable[] result = (Markable[]) markablesAtDiscourseElement.get(discourseElementId);
        if (result == null) result = new Markable[0];
        if (sort)
        {
            Arrays.sort(result,getCurrentDiscourse().DISCOURSEORDERCOMP);
        }
        return result;
    }

    
    /** This method returns an ArrayList of those Markable objects associated with discourseElement Id, or empty list if none. 
        Since this is on MarkableLayer level, no distinction is made wrt to active/inactive. 
        The retrieved Array comes from a hash, so this method is efficient (a little less so if sort==true,
        which causes the markables to be sorted in discourse order, shorter after longer ones). */
    public ArrayList getMarkablesAtDiscourseElementID(String discourseElementId, Comparator comp)
    {
        Markable[] result = (Markable[]) markablesAtDiscourseElement.get(discourseElementId);