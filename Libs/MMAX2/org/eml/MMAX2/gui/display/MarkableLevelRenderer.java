
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