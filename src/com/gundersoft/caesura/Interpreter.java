package com.gundersoft.caesura;

import java.util.HashMap;

import android.content.Context;

/**
 * Handles the input and interpretation of commands sent through SMS.
 * @author Christian Gunderman
 */
public class Interpreter {
	/** The Context for the Android app that instantiated this Intepreter */
	private Context context;
	/** A HashMap of commands available to this implementation */
	private HashMap<String, Function> commands;
	
	/**
	 * Instantiates a simple shell style interpreter
	 * @param The Context of the given Android app.
	 */
	public Interpreter(Context appContext) {
		commands = new HashMap<String, Function>();
		context = appContext;
	}
	
	/**
	 * Defines an interface, allowing Function implementers to
	 * define their own run method, as well as help routines.
	 */
	public interface Function {
		/**
		 * Defines the code that will be run upon execution of
		 * this command.
		 * @param codeLine A system provided GabSnippet object
		 * containing arguments.
		 */
		public void runCommand(GabSnippet codeLine);
		
		/**
		 * Should define what to do when help is requested by 
		 * the user for this command. 
		 * @param codeLine A GabSnippet object defining additional
		 * arguments given by the user.
		 * @return True if help is provided, and false if 
		 * no help message is sent.
		 */
		public boolean runHelp(GabSnippet codeLine);
	}
	
	/**
	 * Registers a new function to the Interpreter
	 * @param functionName A String containing the name for the function
	 * @param newFunc The Function object that will be run when this command
	 * is recv.
	 */
	public void registerFunction(int functionName, Function newFunc)  {
		registerFunction(context.getString(functionName), newFunc);
	}
	
	/**
	 * Overload of register function. See registerFunction for details.
	 * @param functionName A String containing the name of this function.
	 * @param newFunc A Function object containing a function to be run
	 * when this command is recv.
	 */
	public void registerFunction(String functionName, Function newFunc) {
		commands.put(functionName.toLowerCase(), newFunc);
	}
	
	/**
	 * Executes a given line by treating the first word as a command 
	 * and passing subsequent words as arguments.
	 * @param codeLine The line of code to execute.
	 * @param getHelp If true, runs the help associated with the given
	 * command, rather than the command itself.
	 * @return Returns true if the command exists. If getHelp was true,
	 * returns true if the help command ran provided a help message, and
	 * false if not.
	 */
	public boolean executeLine(GabSnippet codeLine, boolean getHelp) {
		Function function = this.commands.get(codeLine.getGabArg(0).toLowerCase());
		if(function != null) {
			// if help for command is requested
			if(getHelp) {
				if(function.runHelp(codeLine) == true) {
					return true;
				}
			} else {
				function.runCommand(codeLine);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns a String array containing a list of commands
	 * @return
	 */
	public String [] enumCommands() {
		//TODO: Do something about this...less than ideal situation
		//return commandArray;
		return null;
	}
	
	/**
	 * A piece of textual command or command line arguments, broken up into
	 * a convenient, usable format.
	 */
	public static class GabSnippet {
		/** The array of split strings */
		private String[] refinedGab;
		
		/**
		 * Constructs a GabSnippet from some raw text.
		 * @param rawGab A String of raw text made up of
		 * arguments separated by spaces.
		 * @throws InvalidRawGabException Thrown if the provided
		 * rawGab argument cannot be split into an array.
		 */
		public GabSnippet(String rawGab) throws InvalidRawGabException {
			refinedGab = rawGab.split(" ");
			if(refinedGab == null)
				throw new InvalidRawGabException("Invalid rawGab. Unable to split String.");
		}
		
		/**
		 * Thrown if the rawGab argument of GabSnippet constructor cannot be
		 * split.
		 */
		public static class InvalidRawGabException extends Exception {
			/** Added so Java compiler will shut up */
			private static final long serialVersionUID = 1L;

			/**
			 * Instantiates the exception.
			 * @param msg Message to send with exception.
			 */
			public InvalidRawGabException(String msg) {
				super(msg);
			}
		}
		
		/**
		 * Returns a String representation of the command 
		 * (first word) of this GabSnippet.
		 */
		@Override
		public String toString() {
			return toString(0);
		}
		
		/**
		 * Gets a String representation from the specified 
		 * argument, 0 being the command, and any larger
		 * numbers being arguments for the command. 
		 * @param startIndex
		 * @return
		 */
		public String toString(int startIndex) {
			StringBuilder finalProduct = new StringBuilder();
			for (int i = startIndex; i < refinedGab.length; i++) {
				finalProduct.append(refinedGab[i]);
				if(i != (refinedGab.length - 1))
					finalProduct.append(' ');
			}
			
			// if empty string, return null
			if(finalProduct.length() == 0) {
				return null;
			} else 
				return finalProduct.toString();
		}
		
		/**
		 * Gets the number of arguments in this GabSnippet.
		 * @return
		 */
		public int getNumberOfArgs() {
			return refinedGab.length;
		}
		
		/**
		 * Gets a specific argument.
		 * @param argument The argument to return. 0 returns
		 * the first entry in the array, the command, and any 
		 * larger numbers returns the words after the command.
		 * @return A String containing the requested argument.
		 * Returns null if the GabSnippet does not have that
		 * many arguments.
		 */
		public String getGabArg(int argument) {
			if(argument < refinedGab.length) {
				return refinedGab[argument];
			} 
			return null;
		}
		
		/**
		 * Compares the given String to a specified argument. Comparison
		 * is not case sensitive.
		 * @param argumentNumber The number of the argument to compare.
		 * @param textToCompare The String to compare to the selected
		 * argument.
		 * @return True if the argument matches the given String, and false
		 * if the arguments do not match or there is not an argument with
		 * this index.
		 */
		public boolean compareArg(int argumentNumber, String textToCompare) {
			String argument = getGabArg(argumentNumber);
			if(argument != null) {
				if(argument.toLowerCase().compareTo(textToCompare.toLowerCase()) == 0)
					return true;
			}
			return false;
		}
	}
}
