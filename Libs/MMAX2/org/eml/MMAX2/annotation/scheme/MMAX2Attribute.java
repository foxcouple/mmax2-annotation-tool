
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

/*
 * Re-worked handling of casing for attribute names and values (1.15)
 */

package org.eml.MMAX2.annotation.scheme;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.eml.MMAX2.annotation.markables.MarkableHelper;
import org.eml.MMAX2.annotation.markables.MarkableRelation;
import org.eml.MMAX2.api.AttributeAPI;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MMAX2Attribute extends JPanel implements java.awt.event.ActionListener, javax.swing.event.DocumentListener, org.eml.MMAX2.api.AttributeAPI
{
    private String ID;   
    /** Name of the Attribute this MMAX2Attribute controls, as supplied in the annotation scheme (for display purposes) */
    private String displayAttributeName;    
    
    /** Whether this Attribute is currently frozen. */
    private boolean frozen = false;    
    /** Number of options for this Attribute (for exclusive nominal attributes only). */
//    private int size;
    /** Reference to the AnnotationScheme object that this Attribute is defined in / part of */    
    private MMAX2AnnotationScheme annotationscheme;            
    /** Type of this Attribute: AttributeAPI.NOMINAL_BUTTON, AttributeAPI.NOMINAL_LIST, AttributeAPI.FREETEXT, AttributeAPI.MARKABLE_SET, AttributeAPI.MARKABLE_POINTER, ... */
    private int type;
    
    private String toShowInFlag="";
    
    private int 	lineWidth;
    private Color 	lineColor;
    private int 	lineStyle;
    private int 	maxSize;
    
    private boolean dashed;
    private String add_to_markableset_instruction;
    private String remove_from_markableset_instruction;
    private String adopt_into_markableset_instruction;
    private String merge_into_markableset_instruction;
    private String point_to_markable_instruction;
    private String remove_pointer_to_markable_instruction;    
    private MarkableRelation markableRelation;    
    private String targetDomain;    
    private UIMATypeMapping uimaTypeMapping;
    
    // For type==NOMINAL_BUTTON: JButton to be set when all buttons are to be unset
    JRadioButton invisibleButton;
    
    // Contains at position x the string value of the button / list item at position x. Used in getDefaultValue, getSelectedValue, setSelectedValue and getDefault 
    // TODO Value strings should not be lower-cased here, because we might need the returned values for display, and for this they need to have the correct casing
    // DONE: This now contains the values as they are defined in the anno scheme
    private ArrayList<String> valueIndicesToValueStrings;

    /* Here, values are used as keys, so they need to be normalized */
    /* For retrieving a button index for a value, the value can be lower-cased ad-hoc */
    private Hashtable<String, Integer> lowerCasedValueStringsToValueIndices;
    
    // New 1.15: This maps a lower-cased value string (as key) to the correctly cased value string (as value)
    // It is used for finding the correct value name in cases where values in stored annotations (legacy) do not conform to the spelling in the anno scheme
    private Hashtable<String, String> lowerCasedValueStringsToValueStrings;
    
    // Contains all buttons
    private ArrayList<JRadioButton> buttons;
    
    /* Contains on index c the 'next' value of the JRadioButton / ListItem at index c */
    private ArrayList<String> nextAttributes;
    
    /* Groups the JRadioButtons for each SchemeLevel */
    ButtonGroup group = null;

    // For type==NOMINAL_LIST
    private JComboBox<String> listSelector = null;    
    // For type==FREETEXT
    JTextArea freetextArea;
    JScrollPane scrollPane;
    // For type==MARKABLE_SET, MARKABLE_POINTER
    JLabel idLabel;     	    
    JLabel attributeLabel;    
               
    public boolean isBranching = false;
    public boolean readOnly = false;
    String tooltiptext;    
    public String oldValue = "";
    int currentIndex = -1;
    
    private String noneAvailableForValue = "<no hint available for this value>";
    
    private ArrayList<MMAX2Attribute> dependsOn 	= new ArrayList<MMAX2Attribute>();    
    private ArrayList<String> orderedValues = new ArrayList<String>();
    
    /** Creates new SchemeLevel. attributeName is in the original spelling (upper/lower case) as supplied in the annotation scheme. */
    /* New in 1.15: Maintain casing for attributes and values, but make all comparisons case-insensitive */
    public MMAX2Attribute(String id, String attributeName, int _type, NodeList allChildren, MMAX2AnnotationScheme currentScheme, int width, String tiptext, String hintText, int _lineWidth, Color _color, int _lineStyle, int _maxSize, String _targetDomain, String _add_instruction, String _remove_instruction, String _adopt_instruction, String _merge_instruction, String _point_to_markable_instruction, String _remove_pointer_to_markable_instruction, float fontSize, boolean _dashed, String _toShowInFlag, UIMATypeMapping _uimaTypeMapping)
    {                                    
        boolean compact = false;
    	String compactVar = System.getProperty("compact");
    	if (compactVar != null && compactVar.equalsIgnoreCase("true")) { compact = true; }
        if (compact) { fontSize-=3;}
                                                
        setAlignmentX(JPanel.LEFT_ALIGNMENT);
        
        toShowInFlag = _toShowInFlag;        
        dashed = _dashed;
        // TODO: Support ordering for relation-type attributes
        type = _type;        
        // tiptext is the text on the 'text' attribute on the attribute
        // It should only be used for tooltips, and not for annotationhints
        tooltiptext = tiptext;        
        final String tempName = attributeName;
        final String tip = tooltiptext;
        final String tempHintText=hintText;
        
        if (currentScheme.isVerbose()) {System.err.println("\n    Attribute '"+attributeName +"' (type "+type+"):");}
                
        ID 						= id;
        displayAttributeName 	= attributeName;
//        size 					= 0;
        annotationscheme 		= currentScheme;
        lineWidth 				= _lineWidth;
        lineColor 				= _color;
        lineStyle 				= _lineStyle;
        maxSize 				= _maxSize;
        targetDomain 			= _targetDomain;
        
        add_to_markableset_instruction 			= _add_instruction;
        remove_from_markableset_instruction 	= _remove_instruction;
        adopt_into_markableset_instruction 		= _adopt_instruction;
        merge_into_markableset_instruction 		= _merge_instruction;        
        point_to_markable_instruction 			= _point_to_markable_instruction;
        remove_pointer_to_markable_instruction 	= _remove_pointer_to_markable_instruction;
                
        // Init list of MMAX2Attributes this one points to, if any
        nextAttributes = new ArrayList<String>();                     
        uimaTypeMapping = _uimaTypeMapping;        
        String filler = "";
        for (int q=0;q<width+3;q++)
        {
            filler = filler + " ";
        }                
        
        attributeLabel = new JLabel(displayAttributeName);     
        attributeLabel.setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
        if (MMAX2.getStandardFont() != null) { attributeLabel.setFont(MMAX2.getStandardFont().deriveFont((float)fontSize)); }

        // TODO Support different colors for different attribute labels
        attributeLabel.setForeground(Color.darkGray);               
        if (tooltiptext.equals("")==false){ attributeLabel.setToolTipText(tooltiptext); }

        final MMAX2AnnotationScheme schemeCopy = currentScheme;

        if (tempHintText.equals("")==false)
        {
            attributeLabel.addMouseListener(
            new java.awt.event.MouseAdapter()
            {
                public void mouseEntered(java.awt.event.MouseEvent me) { schemeCopy.showAnnotationHint(tempHintText,false,tempName); }
                public void mouseExited(java.awt.event.MouseEvent me) { schemeCopy.hideAnnotationHint(); }
                public void mouseClicked(java.awt.event.MouseEvent me)
                {
                    if (me.getButton()==java.awt.event.MouseEvent.BUTTON3)
                    {
                        schemeCopy.showAnnotationHint(tempHintText,true,tempName);
                        return;
                    }
                }
            }
            );
        }

        setAlignmentX(JComponent.LEFT_ALIGNMENT);
        setLayout(new FlowLayout(FlowLayout.LEADING,0,0));

        /* Create left to right Box to accept label and (JRadioButtons or JComboBox) */                
        Box innerBox = Box.createHorizontalBox();//                
        Box labelBox = Box.createVerticalBox();
        
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
                
        JPanel buttonBox = new JPanel();        
        
        /* Add Attribute Name as first element */        
        attributeLabel.setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
        attributeLabel.addMouseListener(
        new java.awt.event.MouseAdapter()
            {
                public void mouseEntered(java.awt.event.MouseEvent me) { if (schemeCopy.getAttributePanel().getContainer().isHintToFront()) { schemeCopy.annotationHintToFront(); }}
                public void mouseExited(java.awt.event.MouseEvent me) { }
            });        

        labelPanel.add(attributeLabel);

        // TODO Vary this to optimize layout
        labelBox.add(Box.createHorizontalStrut(120));