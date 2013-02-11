package com.gundersoft.caesura;

import android.content.Context;

public class Interpreter {
	private Context context;
	private int commandsInArray;
	private String[] commandArray = null;
	private Function[] functionArray = null;
	public Interpreter(Context appContext) {
		commandsInArray = 0;
		commandArray = new String[0];
		functionArray = new Function[0];
		context = appContext;
	}
	public interface Function {
		public void runCommand(GabSnippet codeLine);
		public boolean runHelp(GabSnippet codeLine);
	}
	public void registerFunction(int functionName, Function newFunc) {
		_registerFunction(context.getString(functionName), newFunc);
	}
	public void registerFunction(String functionName, Function newFunc) {
		_registerFunction(functionName, newFunc);
	}
	public boolean executeLine(GabSnippet codeLine, boolean getHelp) {
		for(int i=0; i<commandArray.length; i++){
			if(codeLine.compareArgument(0, commandArray[i])){
				if(getHelp) {
					if(functionArray[i].runHelp(codeLine) == true) {
						return true;
					}
				} else {
					functionArray[i].runCommand(codeLine);
					return true;
				}
			}
		}
		return false;
	}
	private void _registerFunction(String functionName, Function newFunc) {
		String[] tmpCommandArray = new String[commandArray.length + 1];
		Function[] tmpFunctionArray = new Function[functionArray.length + 1];
		
		_copyArray(functionArray, tmpFunctionArray, -1);
		tmpFunctionArray[functionArray.length] = newFunc;
		functionArray = tmpFunctionArray;
		
		_copyArray(commandArray, tmpCommandArray, -1);
		tmpCommandArray[commandArray.length] = functionName;
		commandArray = tmpCommandArray;
	}
	private void _copyArray(Function[] sourceArray, Function[] destinationArray, int numberToCopy) {
		for(int i=0; i<sourceArray.length; i++){
			if(i <= numberToCopy || numberToCopy == -1){
				destinationArray[i] = sourceArray[i];
			} else {
				break;
			}
		}
	}
	private void _copyArray(String[] sourceArray, String[] destinationArray, int numberToCopy) {
		for(int i=0; i<sourceArray.length; i++){
			if(i <= numberToCopy || numberToCopy == -1){
				destinationArray[i] = sourceArray[i];
			} else {
				break;
			}
		}
	}
	public String [] enumCommands() {
		return commandArray;
	}
	public static class GabSnippet {
		private boolean caseSensitive = false;
		private String[] refinedGab;
		public GabSnippet(String rawGab) {
			refinedGab = rawGab.split(" ");
		}
		@Override
		public String toString() {
			return toString(0);
		}
		public String toString(int startIndex) {
			String finalProduct = "";
			for (int i=startIndex; i<refinedGab.length; i++){
				finalProduct += refinedGab[i];
				if(i != (refinedGab.length - 1)){
					finalProduct += " ";
				}
			}
			if(finalProduct.compareTo("") == 0) {
				return null;
			} else 
				return finalProduct;
		}
		public boolean wasCreated() {
			if(refinedGab != null) {
				return true;
			}
			return false;
		}
		public int getNumberOfArgs() {
			if(wasCreated()) {
				return refinedGab.length;
			}
			return -1;
		}
		public String getGabArg(int argument) {
			if(wasCreated() && argument < refinedGab.length) {
				return refinedGab[argument];
			} 
			return null;
		}
		public void setCaseSensitive(boolean isCaseSensitive) {
			caseSensitive = isCaseSensitive;
		}
		public boolean isCaseSensitive() {
			return caseSensitive;
		}
		public boolean compareArgument(int argumentNumber, String textToCompare) {
			String argument = getGabArg(argumentNumber);
			if(argument != null) {
				if(!caseSensitive) {
					argument = argument.toLowerCase();
					textToCompare = textToCompare.toLowerCase();
				}
				if(argument.compareTo(textToCompare) == 0)
					return true;
			}
			return false;
		}
	}
}
