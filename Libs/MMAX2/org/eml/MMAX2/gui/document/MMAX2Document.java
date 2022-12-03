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
        setDisplayFontName(current