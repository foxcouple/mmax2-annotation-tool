
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

package org.eml.MMAX2.gui.display;

// XML Parsing
import java.awt.Color;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.xerces.parsers.DOMParser;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableChart;
import org.eml.MMAX2.annotation.markables.MarkableLevel;
import org.eml.MMAX2.annotation.markables.MarkablePointer;
import org.eml.MMAX2.annotation.markables.MarkableRelation;
import org.eml.MMAX2.annotation.markables.Renderable;
import org.eml.MMAX2.annotation.markables.SimpleMarkableCustomization;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.gui.document.MMAX2Document;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class MarkableLevelRenderer 
{
    /** Reference to the MarkableLayer object that this MarkableLayerRenderer is associated with. */
    private MarkableLevel level=null;
            
    private Color backgroundColor = null;
    private Color foregroundColor = null;
    private Color handleColor = null;
                   
    private SimpleAttributeSet defaultActiveHandleStyle=new SimpleAttributeSet();    
            
    private boolean foregroundIsTransparent = true;
    private boolean backgroundIsTransparent = true;
    
    private boolean bold = false;
    private boolean italic = false;
    private boolean underline = false;
    private boolean strikethrough = false;
    private boolean subscript = false;
    private boolean superscript = false;

    private String customizationFileName = "";
    private SimpleMarkableCustomization[] customizations = new SimpleMarkableCustomization[0];            
    private int customizationCount=0;
        
    /** Creates new MarkableLayerRenderer */ 
    public MarkableLevelRenderer(MarkableLevel _level, String _customizationFileName)     
    {
        /** Set reference to associated MarkableLayer. */
        level = _level;        
                        
        /** Initialize default rendering styles. */
        
        if (handleColor != null)
        {
            StyleConstants.setBackground(defaultActiveHandleStyle, handleColor);
        }
        else
        {
            /** Use foreground Color as background color for MarkableHandle hovering, if no explicit color is set for handles */
            if (foregroundIsTransparent==false)
            {
                StyleConstants.setBackground(defaultActiveHandleStyle, foregroundColor);
            }
            else
            {
                StyleConstants.setBackground(defaultActiveHandleStyle, Color.black);
            }
        }
        
        customizationFileName = _customizationFileName;
    }
      
    public final void destroyDependentComponents()
    {
        level = null;
        customizations = null;
        
    }
    
    
//    protected void finalize()
//    {
////        System.err.println("MarkableLevelRenderer is being finalized!");        
//        try
//        {
//            super.finalize();
//        }
//        catch (java.lang.Throwable ex)
//        {
//            ex.printStackTrace();
//        }        
//    }
    
    
    public final int getCustomizationCount()
    {
        return customizationCount;
    }
    
    public final void updateSimpleMarkableCustomizations(boolean status)
    {        
        if (status == false)
        {
            customizationCount = 0;
        }
        else
        {
            if (customizationFileName.equals("")==false)
            {
                customizations = null;
                customizations = new SimpleMarkableCustomization[0];            
                DOMParser parser = new DOMParser();            
                Document customizationDOM = null;            
                try
                {
                    parser.setFeature("http://xml.org/sax/features/validation",false);
                }
                catch (org.xml.sax.SAXNotRecognizedException ex)
                {
                    ex.printStackTrace();            
                    return;
                }
                catch (org.xml.sax.SAXNotSupportedException ex)
                {
                    ex.printStackTrace();
                    return;
                }

                try
                {
                    //parser.parse(new InputSource("FILE:"+customizationFileName));
                    parser.parse(new InputSource(new File(customizationFileName).toURI().toString()));
                }
                catch (org.xml.sax.SAXParseException exception)
                {
                    String error = "Line: "+exception.getLineNumber()+" Column: "+exception.getColumnNumber()+"\n"+exception.toString();            
                    JOptionPane.showMessageDialog(null,error,"MarkableFileLoader: "+customizationFileName,JOptionPane.ERROR_MESSAGE);
                }                
                catch (org.xml.sax.SAXException exception)
                {
                    String error = exception.toString();
                    JOptionPane.showMessageDialog(null,error,"MarkableFileLoader: "+customizationFileName,JOptionPane.ERROR_MESSAGE);
                }
                catch (java.io.IOException exception)
                {
                    String error = exception.toString();
                    JOptionPane.showMessageDialog(null,error,"MarkableFileLoader: "+customizationFileName,JOptionPane.ERROR_MESSAGE);
                }

                customizationDOM =parser.getDocument();
                NodeList allRules = customizationDOM.getElementsByTagName("rule");

                int custNum = allRules.getLength();
                customizations = new SimpleMarkableCustomization[custNum];

                for (int p=0;p<custNum;p++)
                {
                    String pattern = "";
                    String style="";
                    String connector = "";
                    try
                    {
                        pattern =allRules.item(p).getAttributes().getNamedItem("pattern").getNodeValue();
                    }
                    catch (java.lang.NullPointerException ex)
                    {
                     // 
                    }
                    try
                    {
                        style =allRules.item(p).getAttributes().getNamedItem("style").getNodeValue();
                    }
                    catch (java.lang.NullPointerException ex)
                    {
                        // 
                    }
                    try
                    {
                        connector =allRules.item(p).getAttributes().getNamedItem("connector").getNodeValue();
                    }
                    catch (java.lang.NullPointerException ex)
                    {
                        connector = "and";
                    }
                    int numConnector = MMAX2Constants.AND;
                    if (connector.equalsIgnoreCase("or"))numConnector = MMAX2Constants.OR;
                    customizations[p] = new SimpleMarkableCustomization(level, pattern,numConnector,MMAX2Utils.createSimpleAttributeSet(style,false));                
                } // for p
            }// if not ""                        
            customizationCount = customizations.length;         
        }
    }
    
    public final Color getHandleColor()
    {
        return StyleConstants.getBackground(defaultActiveHandleStyle);
    }
    
    public final void render(Markable markable, int mode)
    {
        SimpleAttributeSet styleToUse = null;
        
        // Get references to document, textpane and markable chart once 
        MMAX2Document doc = level.getCurrentDiscourse().getDisplayDocument();
        MMAX2TextPane pane = level.getCurrentDiscourse().getMMAX2().getCurrentTextPane();
        MarkableChart chart = level.getCurrentDiscourse().getCurrentMarkableChart();

        int[] displayStartPositions = null;
        int[] displayEndPositions = null;
        String[][] fragments = null;
        
        if (markable != null)
        {
            // Get each fragment and its resp. display start and end position
            displayStartPositions = markable.getDisplayStartPositions();
            displayEndPositions = markable.getDisplayEndPositions();
            fragments = markable.getFragments(); // ok
        }
        // The current markable was deleted
        if (mode == MMAX2Constants.RENDER_REMOVED)
        {
            String[] currentFrag = null;
            doc.startChanges(markable);
            // Get array of fragments
            fragments = markable.getFragments();
            // Iterate over all fragments
            for (int z=0;z<fragments.length;z++)
            {
                // Get current fragment
                currentFrag = fragments[z];
                int discPos =0;
                // Iterate over current fragment
                for (int r=0;r<currentFrag.length;r++)
                {                    
                    // Get attributes to be displayed in this position.
                    // Since the markable has been removed already, it will not yield any attributes, 
                    // and thus will not show up any more.                     
                    styleToUse=chart.getTopAttributesAtDiscourseElement(currentFrag[r]);
                    discPos = level.getCurrentDiscourse().getDiscoursePositionFromDiscourseElementID(currentFrag[r]);
                    doc.bulkApplyStyleToDiscourseElement(level.getCurrentDiscourse().getDisplayStartPositionFromDiscoursePosition(discPos),styleToUse,true);
                }
            }
            doc.commitChanges();
        }
        else if (mode == MMAX2Constants.RENDER_IN_SET)
        {
            // Modified for discont: 30.01.05
            // First, reapply currently valid attributes
            // This will enforce any customization-dependent attribute changes, including size and font
            markable.renderMe(MMAX2Constants.RERENDER_THIS);            
            
            // Then, add set-related selection attributes on top
            styleToUse = getAttributesForMarkable(markable);
            
            if (styleToUse.isDefined(StyleConstants.FontSize)==false)
            {
                // If no attribute-dependent size exists, use current display size
                StyleConstants.setFontSize(styleToUse,MMAX2.currentDisplayFontSize);
            }
            if (styleToUse.isDefined(StyleConstants.FontFamily)==false)
            {
                // If no attribute-dependent size exists, use current display size
                StyleConstants.setFontFamily(styleToUse,MMAX2.currentDisplayFontName);
            }
            
            StyleConstants.setBackground(styleToUse,Color.lightGray);
            
            // Iterate over all fragments of currentmarkable
            for (int b=0;b<displayStartPositions.length;b++)
            {
                doc.bulkApplyStyleToDisplaySpanBackground(displayStartPositions[b],displayEndPositions[b]-displayStartPositions[b]+1, styleToUse);
            }
        }
        else if (mode == MMAX2Constants.RENDER_IN_SEARCHRESULT)
        {
            // Modified for discont: 30.01.05
            doc.startChanges(markable);

            styleToUse = getAttributesForMarkable(markable);
            if (styleToUse.isDefined(StyleConstants.FontSize)==false)
            {
                // If no attribute-dependent size exists, use current display size
                StyleConstants.setFontSize(styleToUse,MMAX2.currentDisplayFontSize);
            }
            if (styleToUse.isDefined(StyleConstants.FontFamily)==false)
            {
                // If no attribute-dependent size exists, use current display size
                StyleConstants.setFontFamily(styleToUse,MMAX2.currentDisplayFontName);
            }
            
            StyleConstants.setBackground(styleToUse,Color.orange);
            
            // Iterate over all fragments of currentmarkable
            for (int b=0;b<displayStartPositions.length;b++)
            {            
                doc.bulkApplyStyleToDisplaySpanBackground(displayStartPositions[b],displayEndPositions[b]-displayStartPositions[b]+1, styleToUse);
            }
            doc.commitChanges();
        }
        else if (mode == MMAX2Constants.RERENDER_THIS)
        {
            // No mod for discont necessary