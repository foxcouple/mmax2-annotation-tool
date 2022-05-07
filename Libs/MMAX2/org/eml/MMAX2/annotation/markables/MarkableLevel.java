
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
    