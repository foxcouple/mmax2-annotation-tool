
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