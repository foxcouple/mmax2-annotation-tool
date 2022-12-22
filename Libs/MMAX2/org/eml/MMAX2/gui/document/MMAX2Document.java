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

package org.eml.MMAX2.gui.document;

import java.awt.Color;
import java.awt.Font;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.core.MMAX2;

public class MMAX2Document extends DefaultStyledDocument
{        
    private DefaultDocumentEvent uncommittedChanges = null;
    
    private SimpleAttributeSet currentAttributes = null;        
    
    private MMAX2 mmax2 = null;
    
    /** Creates new MMAX2Document */
    public MMAX2Document(String currentDisplayFontName, int currentDisplayFontSize) 
    {                      
        currentAttributes = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(currentAttributes, (float)0.5);
        setDisplayFontName(currentDisplayFontName);
        setDisplayFontSize(currentDisplayFontSize);        
    }  

    
    public final void setMMAX2(MMAX2 _mmax2)
    {
        mmax2 = _mmax2;     
    }
                
    public final String getDisplayFontName()
    {
        return StyleConstants.getFontFamily(currentAttributes);
    }
    
    public final void setDisplayFontName(String name)
    {
//    	Font f = new Font(name, Font.PLAIN, StyleConstants.getFontSize(currentAttributes));
//        mmax2.getCurrentTextPane().setFont(f);
        StyleConstants.setFontFamily(currentAttributes,name);   
        mmax2.markableSelectorFont = new Font(name, Font.PLAIN,11);        
    }
    
    public final void setDisplayFontSize(int size)
    {
        StyleConstants.setFontSize(currentAttributes,size);   
    }
        
    
    public final void setSize(int size)
    {
        flush();
        StyleConstants.setFontSize(this.currentAttributes,size);
    }
        
    public final void setBold(boolean status)// int documentPosition)
    {
        flush();
        StyleConstants.setBold(currentAttributes,status);
    }
    
    public final void setUnderline(boolean status)
    {
        flush();
        StyleConstants.setUnderline(currentAttributes,status);        
    }

    public final void setItalic(boolean status)
    {
        flush();
        StyleConstants.setItalic(currentAttributes,status);        
    }

    public final void setSubscript(boolean status)
    {
        flush();
        StyleConstants.setSubscript(currentAttributes,status);        
    }

    public final void setSuperscript(boolean status)
    {
        flush();
        StyleConstants.setSuperscript(currentAttributes,status);
    }
           
    public final void setStrikeThrough(boolean status)
    {
        flush();
        StyleConstants.setStrikeThrough(currentAttributes,status);                
    }
      
    public final void setColor(Color color, boolean status)
    {
        flush();
        if (status)
        {          
            /** color is to be activated. So set it from current position on. */
            StyleConstants.setForeground(currentAttributes, color);
            /** And put it on colorStack. */
        }
    }

    public final void setDefaultColor()
    {
        
        flush();
        // color is to be activated. So set it from current position on. 
        StyleConstants.setForeground(currentAttributes, Color.black);        
    }
    
    
    public final void dump()
    {
        try
        {
            System.out.println(getText(0,getLength()));
        }
        catch (javax.swing.text.BadLocationException ex)
        {
            
        }
    }
    
    public