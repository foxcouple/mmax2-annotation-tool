
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

package org.eml.MMAX2.discourse;

// Display Attributes
import java.awt.Color;
import java.io.File;

import javax.swing.JOptionPane;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xpath.NodeSet;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableFileLoader;
import org.eml.MMAX2.annotation.markables.MarkableLevel;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class MMAX2DiscourseLoader 
{
    protected String commonPathsFile 			= "common_paths.xml";    
    protected String commonSchemePath 			= "";
    protected String commonStylePath 			= "";
    protected String commonBasedataPath 		= "";
    protected String commonCustomizationPath 	= "";
    protected String commonMarkablePath			= "";
    protected String commonQueryPath			= "";    
    protected String rootPath 					= "";    
    protected String wordFileName 				= "";    