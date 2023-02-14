
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

package org.eml.MMAX2.gui.windows;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableLevel;
import org.eml.MMAX2.annotation.markables.MarkablePointer;
import org.eml.MMAX2.annotation.markables.MarkableRelation;
import org.eml.MMAX2.annotation.scheme.MMAX2Attribute;
import org.eml.MMAX2.api.AttributeAPI;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.discourse.MMAX2Discourse;

public class MMAX2MarkablePointerBrowser extends javax.swing.JFrame implements java.awt.event.ActionListener, java.awt.event.WindowListener
{        
    MMAX2 mmax2 = null;
    JLabel levelBoxLabel = null;
    JComboBox<String> levelBox = null;
    JLabel attributeBoxLabel = null;
    JComboBox<String> attributeBox = null;
    JCheckBox permanentBox = null;
    MMAX2Discourse discourse = null;
    JScrollPane treeViewPane = null;
    JTree tree = null;
    DefaultMutableTreeNode root = null;        
    
    public MMAX2MarkablePointerBrowser(MMAX2 _mmax2) 
    {
        super();
        addWindowListener(this);
        mmax2 = _mmax2;
        discourse = mmax2.getCurrentDiscourse();
                               
        root = new DefaultMutableTreeNode("Document");
        tree = new JTree(root);
        
        treeViewPane = new JScrollPane(tree);
                        
        getContentPane().add(treeViewPane);
        
        levelBox = new JComboBox<String>();
        levelBox.setFont(MMAX2.getStandardFont());
        levelBox.addItem("<none>");
        MarkableLevel[] levels = discourse.getCurrentMarkableChart().getMarkableLevels();