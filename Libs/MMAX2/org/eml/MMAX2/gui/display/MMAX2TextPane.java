
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

import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTextPane;
import javax.swing.Timer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableSet;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class MMAX2TextPane extends JTextPane implements AdjustmentListener, KeyListener
{                                
    private boolean isDraggingGoingOn=false;        
    private boolean CONTROL_DOWN=false;
    private boolean mouseInPane = true;    
    private Timer refreshControlTimer = null;

    // Milliseconds before refreshTimer is stopped after the last scrolling ocurred
    private int TIME_TO_REFRESH_AFTER_LAST_SCROLLING = 5; // was 1000
    private Timer refreshTimer = null;
    // Milliseconds between separate refreshTimer firings
    private int TIME_BETWEEN_REFRESHES = 10;// was 10
    
    private Timer activateHoveringLatencyTimer = null;
    private int HOVERING_LATENCY_TIME = 100;
    
    private MMAX2CaretListener currentCaretListener = null;
    private MMAX2MouseListener currentMouseListener = null;
    private MMAX2MouseMotionListener currentMouseMotionListener = null;
    
    private MMAX2 mmax2 = null;
    private MouseEvent currentMouseMoveEvent = null;
    private Markable currentHoveree = null;
    
    private boolean showHandlesOfCurrentFragmentOnly = false;
    private boolean showFloatingAttributeWindow = false;
    
    private int currentDot = 0;
    
    private JPopupMenu floatingAttributeWindow = null;
    private JPopupMenu markableSetPeerWindow = null;

    public MMAX2TextPane()
    {           	
    	currentMouseListener = new MMAX2MouseListener();
        addMouseListener(currentMouseListener);     
        currentMouseMotionListener = new MMAX2MouseMotionListener();
        this.addMouseMotionListener(currentMouseMotionListener);     
        currentCaretListener = new MMAX2CaretListener();
        addCaretListener(currentCaretListener);
        
        setCaret(new MMAX2Caret());
        setEditable(false);                
        setDoubleBuffered(true);
        
        // Init refreshControlTimer (0 is a placeholder, overridden by TIME_TO_REFRESH_AFTER_LAST_SCROLLING later)
        refreshControlTimer = new Timer(0 , new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                stopAutoRefresh();
            }                
        });
            
        refreshControlTimer.setCoalesce(true);
        // Make refreshControlTimer fire only once
        refreshControlTimer.setRepeats(false);
        // Set time before first (and only) firing
        refreshControlTimer.setInitialDelay(TIME_TO_REFRESH_AFTER_LAST_SCROLLING);
            
        refreshTimer = new Timer(TIME_BETWEEN_REFRESHES, new ActionListener()