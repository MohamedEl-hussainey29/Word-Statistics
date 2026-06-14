****Word-Statistics****
--------------------------
 program that reads all text files form a specific directory and return word statistics (number of words 
per file/directory, longest word, shortest word, number of “is”, “are” and “you”). - - - 
The program should have a simple GUI 
The input of the program is a directory 
o It should then search for all text files that reside in that directory 
o There should be an option to check for text files in subdirectories 

------------------------------------------------------------------------------------------------
Problem Modeling
------------------
GUI - 
Mian thread identify text files in 
directory and its subdirectory (one or 
two level) and show them in GUI 
Each thread explores one or more text 
files 
o No. of threads is based on the 
number of processors (core) 
Each thread should send updates to  GUI
----------------------------------------------------------

**GUI**
-------
Input 
o Directory path (or selection via browse button) 
o Checkbox for including subdirectories - - 
Output 
o In table form: 
▪ #words 
▪ #is 
▪ #are 
▪ #you  
▪ Longest word per file 
▪ Shortest word per file 
▪ Longest word per directory  
▪ Shortest word per directory  
