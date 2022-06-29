
/*
 * Copyright 2007 Mark-Christoph Müller
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.text.SimpleAttributeSet;

import org.eml.MMAX2.annotation.query.MMAX2MatchingCriterion;
import org.eml.MMAX2.annotation.scheme.MMAX2Attribute;
import org.eml.MMAX2.api.AttributeAPI;
import org.eml.MMAX2.utils.MMAX2Constants;


public class SimpleMarkableCustomization 
{
    private String[] criteria = null;
    private MMAX2MatchingCriterion[] matchingCriteria = null;
    private SimpleAttributeSet attributes = null;
    private int connector = MMAX2Constants.AND;
    private MarkableLevel level = null;
    
    // Create two lists to store relations and assigned colors for relation-attributes