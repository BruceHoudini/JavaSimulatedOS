	package os_resources;

	import java.io.File;
	import java.io.FileNotFoundException;
	import java.util.Scanner;

	public class Loader {
		
		
		public Loader(File programfile) throws FileNotFoundException, OSException, MemoryException{
			
			//try{
			Scanner scan = new Scanner(programfile);
			processFile(scan);
			scan.close();
			/*}
			catch(FileNotFoundException exception){
				System.out.println("File not found");		
			}*/
			
		}
		private void processFile(Scanner scan)throws OSException, MemoryException{
			while (scan.hasNextLine()){
				String line = scan.nextLine();
				//debug
				System.out.println(line);
				//debug
				int index = 0;
				if (line.charAt(index) == '/')
				{
					index++;
					if (line.charAt(index) == '/'){
						index++;
						if (line.charAt(index) == ' '){
							index++;
							line = line.substring(index, line.length());
							//processControlCard(line, scan));
							createProcess(line, scan);
						}
					}
				}
				else{
					//debug
					for(int i = 0; i < DISK.getPointer(); i++)
					System.out.println("This is from DISK: " + DISK.load(0));
					//debug
					throw new OSException("'// 'Expected!");
				}
				processFile(scan);
			}
		}
		private void createProcess(String line, Scanner scan) throws OSException, MemoryException{
			PType cardType = processControlCard(line);
			int ccIndex = 0;
			String PIDst, numInstst, priorityst, sizeInBuffst, sizeOutBuffst, sizeTempBuffst;
			int PID, numInst, numData, priority, sizeInBuff, sizeOutBuff, sizeTempBuff;
			//Hamfisted enmasse initialization of PCB variables
			PID = numInst = numData = priority = sizeInBuff = sizeOutBuff = sizeTempBuff = 0;
			int pAddr = DISK.getPointer();
			//Checks if control card is JOB and extracts JOB information for Process creation
			//if statements exists after calling saving subsequent instruction to disk until
			//next control card is reached.
			if (cardType == PType.JOB){
				ccIndex+=4;
				//Gets PID from line, converts hex string into decimal integer
				//Each subsequent three line block does the same thing with other
				//values necessary to create a Process object
				PIDst = getNextLexeme(line, ccIndex);
				ccIndex += PIDst.length();
				PID = Integer.parseInt(PIDst, 16);
				
				//debug
				System.out.println(ccIndex);
				//System.out.println(PIDst);
				//debug
				
				numInstst = getNextLexeme(line, ccIndex);
				ccIndex += numInstst.length();
				numInst = Integer.parseInt(numInstst, 16);
				
				//debug
				System.out.println(ccIndex);
				//System.out.println(numInstst);
				//debug
				
				priorityst = getNextLexeme(line, ccIndex);
				ccIndex += priorityst.length();
				priority = Integer.parseInt(priorityst, 16);
				
				//debug
				//System.out.println(PID);
				System.out.println(ccIndex);
				//debug
			
				saveToDisk(numInst, scan);
			}
			line = scan.nextLine();
			//debug
			//System.out.println(line);
			//debug
			
			//The following nested "if's" are a hacked together fix. Loop taken from ProcessFile, ProcessControlCard() Removed
			//Should allow ProcessControlCard() to run twice while keeping the Process variables it needs
			//To aggregate in order to provide complete information to the new process it creates.
			//As a result of the JOB/DATA JOB/DATA JOB/DATA etc. consistency, it seems like an "okay" solution for now
			//Without these if's createProcess() runs once for each JOB and DATA control card which ends up creating 
			//double the necessary processes each with only half the necessary information (Either info from JOB
			//or info from DATA).
			
			//If we can make these if loops their own method called "stripControl" or something to that effect it would
			//definitely make this class look much cleaner.
			
			//...It worked! First try.
			int index = 0;
			if (line.charAt(index) == '/')
			{
				index++;
				if (line.charAt(index) == '/'){
					index++;
					if (line.charAt(index) == ' '){
						index++;
						line = line.substring(index, line.length());
					}
				}
			}
			else{
				//debug
				for(int i = 0; i < DISK.getPointer(); i++)
				System.out.println("This is from DISK: " + DISK.load(0));
				//debug
				throw new OSException("'// 'Expected!");
			}
			cardType = processControlCard(line);
			ccIndex = 0;
			//Checks if control card is DATA and extracts DATA information for Process creation
			//if statements exists after calling saving subsequent instruction to disk until
			//next control card is reached.
			if (cardType == PType.DATA){
				ccIndex+=5;
				//Takes hex string, converts to decimal integer, stores in respective Process variables
				//Just like in the "if (cardType == PType.JOB)" check
				sizeInBuffst = getNextLexeme(line, ccIndex);
				ccIndex += sizeInBuffst.length();
				sizeInBuff = Integer.parseInt(sizeInBuffst, 16);
				
				sizeOutBuffst = getNextLexeme(line, ccIndex);
				ccIndex += sizeOutBuffst.length();
				sizeOutBuff = Integer.parseInt(sizeOutBuffst, 16);
				
				sizeTempBuffst = getNextLexeme(line, ccIndex);
				ccIndex += sizeTempBuffst.length();
				sizeTempBuff = Integer.parseInt(sizeTempBuffst, 16);
				
				numData = saveToDisk(scan);
			}
			PCB.memory.add(new Process(PID, pAddr, numInst, numData, priority, sizeInBuff, sizeOutBuff, sizeTempBuff));
		}
		
		private PType processControlCard(String line) throws OSException{
			String pTypeCheck = getNextLexeme(line, 0);
			//debug
			System.out.println(pTypeCheck);
			//debug
			PType ccType;
			if (pTypeCheck.equals("JOB")){
				ccType = PType.JOB;
			}
			else if (pTypeCheck.equals("Data"))
				ccType = PType.DATA;
			else if (pTypeCheck.equals("END"))
				ccType = PType.END_PROC;
			//This "ND" nonsense is the result of an annoying bug I haven't had the patience to fix properly
			//The last "// END" control card doesn't have a space between the "//" and the "END" so instead
			//of "// END" as is standard throughout the rest of the program, the final END is "//END" which
			//causes the string checking to error, skipping the first E. To fix this I juts added a check for
			//"ND" which is handled as the EOF or "End of File" control card.
			else if (pTypeCheck.equals("ND"))
				ccType = PType.EOF;
			else
				throw new OSException("Expected JOB, DATA, or END Control Card Type");
			return ccType;
		}
		
		private String getNextLexeme(String line, int origin){
			int index;
			//debug
			//System.out.println(origin);
			//debug
			while (origin < line.length() && Character.isWhitespace(line.charAt(origin)))
				origin++;
			index = origin;
			//debug
			//System.out.println(index);
			System.out.println(origin);
			//debug
			while (index < line.length() && !Character.isWhitespace(line.charAt(index))){
				index++;
			}
			//debug
			System.out.println(index);
			//System.out.println(origin);
			//debug
			return line.substring(origin, index);
		}
			
		//Primary method for saving to DISK. Called after JOB control card detected.
		//Iterates number of times specified by the numberOfInstructions value of the
		//JOB control card.
		private void saveToDisk(int iterations, Scanner scan) throws MemoryException{
			int i = DISK.getPointer();
			int iter = i + iterations;
			//debug
			//System.out.println(iter);
			//debug
			String temp;
			while (i < iter){
				temp = scan.nextLine();
				//debug
				//System.out.println(temp.substring(2));
				//debug
				DISK.save(i, temp);
				//debug
				//System.out.println("Saved to DISK from JOB: " + DISK.load(i));
				//debug
				i++;
			}
		}
		
		//@overload
		//Overloaded saveToDisk() method used as a "scan file, save to disk" loop when the number of iterations is
		//necessary is unknown and unspecified. Less efficient than the primary saveToDisk method() and is restricted
		//towards being used only following DATA control cards as result of the check for "// END" which would always
		//error if used after a JOB control card which is followed by DATA rather than END.
		private int saveToDisk(Scanner scan) throws OSException, MemoryException{
			int i = DISK.getPointer();
			int numWrites = i;
			String temp = scan.nextLine();
			while (temp.charAt(0) != '/'){
				DISK.save(i, temp);
				//debug
				//System.out.println("Saved to DISK from DATA: " + DISK.load(i));
				//debug
				i++;
				temp = scan.nextLine();
			}
			PType endTest = processControlCard(temp.substring(3, temp.length()));
			if (endTest != PType.END_PROC && endTest != PType.EOF){
				throw new OSException("Expected END");
			}
			if (i - numWrites <= 0){
				throw new MemoryException("No Data detected or no Data written when loading data to DISK");
			}
			else
				return i - numWrites;
		}

	}
