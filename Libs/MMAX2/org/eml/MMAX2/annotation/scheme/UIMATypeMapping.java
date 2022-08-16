package org.eml.MMAX2.annotation.scheme;

public class UIMATypeMapping 
{
	private String descriptorPath="";
	private String className="";	
	
	public UIMATypeMapping (String _descriptorPath, String _className)
	{
		descriptorPath = _descriptorPath;
		className = _className;
	}
	
	public UIMATypeMapping (String _completePath)
	{		
		int hashPos = _comple